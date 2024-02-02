package com.artur.youtback.service;

import com.artur.youtback.converter.VideoConverter;
import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.VideoMetadata;
import com.artur.youtback.entity.user.UserMetadata;
import com.artur.youtback.entity.user.WatchHistory;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.repository.*;
import com.artur.youtback.service.minio.MinioService;
import com.artur.youtback.tool.Ffmpeg;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.util.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class VideoService {
    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoMetadataRepository videoMetadataRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    UserMetadataRepository userMetadataRepository;
    @Autowired
    RecommendationService recommendationService;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    WatchHistoryRepository watchHistoryRepository;
    @Autowired
    Environment environment;
    @Autowired
    Ffmpeg ffmpeg;
    @Autowired
    MinioService minioService;
    @Autowired
    VideoConverter videoConverter;


    public List<Video> findAll(SortOption sortOption) throws NotFoundException {

        if(sortOption != null){
            return videoRepository.findAll().stream().limit(AppConstants.MAX_VIDEOS_PER_REQUEST)
                    .sorted(SortOptionsComparators.get(sortOption))
                    .map(videoConverter::convertToModel).toList();
        }

        return videoRepository.findAll().stream().limit(AppConstants.MAX_VIDEOS_PER_REQUEST)
                .map(videoConverter::convertToModel).toList();
    }

    public Video findById(Long id) throws NotFoundException{
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(id);
        if(optionalVideoEntity.isEmpty()) throw new NotFoundException("Video not Found");

        return videoConverter.convertToModel(optionalVideoEntity.get());
    }

    public List<Video> findByOption(String option, String value) throws NullPointerException, IllegalArgumentException{
        return Objects.requireNonNull(Tools.findByOption(option, value, videoRepository).stream().map(videoConverter::convertToModel).toList());
    }

    public Collection<Video> recommendations(Long userId, Set<Long> excludes, String[] languages, Integer size) throws IllegalArgumentException{
        if(languages.length == 0) throw new IllegalArgumentException("Should be at least one language");
        try {
            return recommendationService.getRecommendationsFor(userId,excludes, languages, size)
                    .stream().map(videoConverter::convertToModel).collect(Collectors.toList());
        } catch (NotFoundException e) {
            logger.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Video watchById(Long videoId, Long userId) throws NotFoundException{
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        videoEntity.setViews(videoEntity.getViews() + 1);
        if(userId != null){
            userRepository.findById(userId).ifPresent(userEntity -> {
                UserMetadata userMetadata;
                if(userEntity.getUserMetadata() == null){
                    userMetadata = new UserMetadata(userEntity);
                } else {
                    userMetadata = userEntity.getUserMetadata();
                }
                userMetadata.incrementLanguage(videoEntity.getVideoMetadata().getLanguage());
                userMetadata.incrementCategory(videoEntity.getVideoMetadata().getCategory());
                userEntity.getWatchHistory().removeIf(el ->{
                    if(el.getDate().isAfter(LocalDateTime.now().minusDays(1))&& Objects.equals(el.getVideoId(), videoId)){
                        watchHistoryRepository.deleteById(el.getId());
                        return true;
                    }
                    return false;
                });
                userEntity.getWatchHistory().add(new WatchHistory(null, userEntity, videoId));
                userRepository.save(userEntity);
            });
        }
        videoRepository.save(videoEntity);
        return videoConverter.convertToModel(videoEntity);
    }


    public InputStream m3u8Index(Long videoId) throws NotFoundException {
        // TODO: 29.01.2024 make m3u8Index and ts methods to return StreamingResponseBody
        try{
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    minioService.getObject(AppConstants.VIDEO_PATH + videoId + "/index.m3u8")
//            ));
//            return outputStream -> {
//                try{
//                    String line;
//                    while((line = reader.readLine()) != null){
//                        outputStream.write(line.getBytes());
//                        outputStream.flush();
//                    }
//                } catch(Exception e){
//                    logger.error(e.getMessage());
//                    e.printStackTrace();
//                } finally {
//                    outputStream.close();
//                    reader.close();
//                }
//            };
            return minioService.getObject(AppConstants.VIDEO_PATH + videoId + "/index.m3u8");
        } catch(Exception e){
            throw new NotFoundException("cannot retrieve target m3u8 file: " + e);
        }
    }

    public InputStream ts(Long id,String filename) throws NotFoundException {
        try{
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    minioService.getObject(AppConstants.VIDEO_PATH + id + "/" + filename)
//            ));
//            return outputStream -> {
//                try{
//                    String line;
//                    while((line = reader.readLine()) != null){
//                        outputStream.write(line.getBytes());
//                        outputStream.flush();
//                    }
//                } catch(Exception e){
//                    logger.error(e.getMessage());
//                    e.printStackTrace();
//                } finally {
//                    outputStream.close();
//                    reader.close();
//                }
//            };
            return minioService.getObject(AppConstants.VIDEO_PATH + id + "/" + filename);
        } catch(Exception e){
            logger.error(e.getMessage());
            throw new NotFoundException("cannot retrieve target [" + filename + " ] file");
        }
    }

    public Optional<VideoEntity> create(VideoCreateRequest video, Long userId)  throws Exception{
        return create(video.title(), video.description(), video.category(), video.thumbnail().getBytes(), video.video().getBytes(), userId);
    }

    public Optional<VideoEntity> create(String title, String description, String category, File thumbnail, File video, Long userId)  throws Exception{
        return create(title, description, category, Files.readAllBytes(thumbnail.toPath()), Files.readAllBytes(video.toPath()), userId);
    }

    @Transactional
    private Optional<VideoEntity> create(String title, String description, String category, byte[] thumbnail, byte[] video, Long userId) throws Exception{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        LanguageDetector languageDetector = new OptimaizeLangDetector().loadModels();
        try {
            Integer duration = (int)Float.parseFloat(MediaUtils.getDuration(video));
            String language = languageDetector.detect(title).getLanguage();
            VideoEntity savedEntity = videoRepository.save(videoConverter.convertToEntity(title, description, userEntity));
            videoMetadataRepository.save(new VideoMetadata(savedEntity, language, duration, category));

            String folder = AppConstants.VIDEO_PATH + savedEntity.getId();
            byte[] imageBytes = ImageUtils.compressAndSave(thumbnail);
            minioService.putObject(imageBytes, folder + "/" + AppConstants.THUMBNAIL_FILENAME);

            convertAndUpload(video, savedEntity.getId());
            return Optional.of(savedEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Could not create video uploaded from client cause: " + e);
        }
    }

    public void convertAndUpload(byte[] video, Long videoId) throws Exception{
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tmp-ffmpeg");
            Path index = Path.of(tempDir + "/" + "index.mp4");
            Files.write(index, video);
            ffmpeg.convertVideoToHls(index.toFile());
            upload(tempDir.toString(), videoId);
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    public void upload(String prefix, Long videoId) throws Exception {
        for(File file : Objects.requireNonNull(new File(prefix).listFiles())){
            String filename = AppConstants.VIDEO_PATH + videoId + "/" + file.getName();
            logger.trace("Uploading " + filename);
            minioService.uploadObject(file, filename);
        }
    }

    @Transactional
    public void deleteById(Long id) throws Exception {
        if(!videoRepository.existsById(id)) throw new NotFoundException("Video not found");
        VideoEntity videoEntity = videoRepository.getReferenceById(id);
        likeRepository.deleteAllById(videoEntity.getLikes().stream().map(Like::getId).toList());
        watchHistoryRepository.deleteAllByVideoId(id);
        videoRepository.deleteById(id);
        minioService.removeFolder(AppConstants.VIDEO_PATH + id);
        logger.trace("Video with id {} was successfully deleted", id);
    }

    public void update(VideoUpdateRequest updateRequest) throws Exception {
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(updateRequest.videoId());
        if(optionalVideoEntity.isEmpty()) throw new NotFoundException("Video not Found");

        //data allowed to update
        VideoEntity videoEntity = optionalVideoEntity.get();
        if(updateRequest.description() != null){
            videoEntity.setDescription(updateRequest.description());
        }
        if(updateRequest.title() != null){
            videoEntity.setTitle(updateRequest.title());
        }
        if(updateRequest.thumbnail() != null){
            minioService.putObject(ImageUtils.compressAndSave(updateRequest.thumbnail().getBytes()), AppConstants.VIDEO_PATH + videoEntity.getId() + AppConstants.THUMBNAIL_FILENAME);
        }
        if(updateRequest.video() != null){
            minioService.removeFolder(AppConstants.VIDEO_PATH + videoEntity.getId());
            convertAndUpload(updateRequest.video().getBytes(), updateRequest.videoId());
        }
        if(updateRequest.category() != null){
            videoEntity.getVideoMetadata().setCategory(updateRequest.category());
        }
        videoRepository.save(videoEntity);
    }

    public void testMethod(){
        //преобразовать в новый формат все видео
        long start = System.currentTimeMillis();
        try {
            minioService.removeFolder(AppConstants.VIDEO_PATH + "235");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("Refactoring completed in " + (System.currentTimeMillis() - start) + "ms");
    }
    @Transactional
    public int addVideos(int amount) {
        AtomicInteger createdVideos = new AtomicInteger(amount);
        String[] categories = {"Sport", "Music", "Education", "Movies", "Games", "Other"};
        String[][] titles = {{"Football", "Basketball", "Hockey", "Golf"}, {"Eminem", "XXXTentacion", "Drake", "Три дня дождя", "Playboi Carti","Yeat"}, {"Java", "Php", "English", "French", "C#", "C++"}, {"Oppenheimer", "American psycho", "Good fellas","Fight club","Breaking bad", "The boys"}, {"GTA V", "GTA San Andreas", "GTA IV", "Fortnite", "Minecraft", "Need For Speed Most Wanted"}, {"Monkeys", "Cars", "Dogs", "Cats", "Nature"}};
        String videoThumbnailsToCreateDirectory = "video-thumbnails-to-create";
        String videosToCreateDirectory = "videos-to-create";
        File[][] thumbnails = {new File(videoThumbnailsToCreateDirectory + "/sport").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/music").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/education").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/movies").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/games").listFiles(),
                new File(videoThumbnailsToCreateDirectory + "/other").listFiles()
        };
        File[][] videos = {new File(videosToCreateDirectory + "/sport").listFiles(),
                new File(videosToCreateDirectory + "/music").listFiles(),
                new File(videosToCreateDirectory + "/education").listFiles(),
                new File(videosToCreateDirectory + "/movies").listFiles(),
                new File(videosToCreateDirectory + "/games").listFiles(),
                new File(videosToCreateDirectory + "/other").listFiles()
        };
        String description = "Nothing here...";
        List<UserEntity> users = userRepository.findAll();
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Runnable task = () -> {
            transactionTemplate.execute(status -> {
                UserEntity user = users.get((int) Math.floor(Math.random() * users.size()));
                int categoryIndex = (int)Math.floor(Math.random() * categories.length);
                String category = categories[categoryIndex];
                String title = titles[categoryIndex][(int)Math.floor(Math.random() * titles[categoryIndex].length)] + " by " + user.getId();
                try {
                    VideoEntity createdVideo = create(title, description, category, thumbnails[categoryIndex][(int)Math.floor(Math.random() * thumbnails[categoryIndex].length)],
                            videos[categoryIndex][(int)Math.floor(Math.random() * videos[categoryIndex].length)], user.getId()).orElseThrow(()-> new RuntimeException("user not found"));
                    int likesToAdd = (int)Math.floor(Math.random() * users.size());
                    Set<Long> exceptions = new HashSet<>();
                    for (int i = 0;i < likesToAdd;i++){
                        Instant timestamp = Instant.now().minus((int)Math.floor(Math.random() * 2592000), ChronoUnit.SECONDS);
                        UserEntity userEntity = users.get((int)Math.floor(Math.random() * users.size()));
                        if(!exceptions.contains(userEntity.getId())){
                            addLike(userEntity, createdVideo, timestamp);
                            exceptions.add(userEntity.getId());
                        }else{
                            i--;
                        }
                    }
                } catch (Exception e) {
                    status.setRollbackOnly();
                    createdVideos.decrementAndGet();
                    logger.error(e.getMessage());
                }
                return null;
            });
        };
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        for(int i = 0; i< amount; i++){
            executor.execute(task);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(200, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return createdVideos.get();
    }

    private void addLike(UserEntity user, VideoEntity video, Instant timestamp){
        Like like = new Like();
        like.setVideoEntity(video);
        like.setUserEntity(user);
        like.setTimestamp(timestamp);
        likeRepository.save(like);
    }


     protected static class Tools {

         static List<VideoEntity> findByOption(String option, String value, VideoRepository videoRepository) throws IllegalArgumentException {
             if (option.equals(FindOptions.VideoOptions.BY_ID.name()) && value != null) {
                 return videoRepository.findById(Long.parseLong(value)).stream().toList();
             } else if (option.equals(FindOptions.VideoOptions.BY_LIKES.name()) && value != null) {
                 String[] fromTo = value.split("/");
                 if (fromTo.length == 2) {
                     return videoRepository.findByLikes(fromTo[0], fromTo[1], Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
                 } // else throwing exception below
             } else if (option.equals(FindOptions.VideoOptions.BY_VIEWS.name()) && value != null) {
                 String[] fromTo = value.split("/");
                 if (fromTo.length == 2) {
                     return videoRepository.findByViews(fromTo[0], fromTo[1], Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
                 } // else throwing exception below
             } else if (option.equals(FindOptions.VideoOptions.BY_TITLE.name()) && value != null) {
                 return videoRepository.findByTitle(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
             } else if (option.equals(FindOptions.VideoOptions.MOST_DURATION.name())) {
                 return videoRepository.findMostDuration(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
             } else if (option.equals(FindOptions.VideoOptions.MOST_LIKES.name())) {
                 return videoRepository.findMostLikes(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
             } else if (option.equals(FindOptions.VideoOptions.MOST_VIEWS.name())) {
                 return videoRepository.findMostViews(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
             }
             throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
         }

     }

}
