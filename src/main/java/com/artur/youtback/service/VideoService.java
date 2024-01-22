package com.artur.youtback.service;

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
import com.artur.youtback.tool.Ffmpeg;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    PlatformTransactionManager platformTransactionManager;
    @Autowired
    WatchHistoryRepository watchHistoryRepository;
    @Autowired
    Environment environment;
    @Autowired
    Ffmpeg ffmpeg;


    public List<Video> findAll(SortOption sortOption) throws NotFoundException {

        if(sortOption != null){
            return videoRepository.findAll().stream().limit(AppConstants.MAX_VIDEOS_PER_REQUEST)
                    .sorted(SortOptionsComparators.get(sortOption))
                    .map(Video::toModel).toList();
        }

        return videoRepository.findAll().stream().limit(AppConstants.MAX_VIDEOS_PER_REQUEST)
                .map(Video::toModel).toList();
    }

    public Video findById(Long id) throws NotFoundException{
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(id);
        if(optionalVideoEntity.isEmpty()) throw new NotFoundException("Video not Found");

        return Video.toModel(optionalVideoEntity.get());
    }

    public List<Video> findByOption(String option, String value) throws NullPointerException, IllegalArgumentException{
        return Objects.requireNonNull(Tools.findByOption(option, value, videoRepository).stream().map(Video::toModel).toList());
    }

    public Collection<Video> recommendations(Long userId, Set<Long> excludes, String[] languages, Integer size) throws IllegalArgumentException{
        if(languages.length == 0) throw new IllegalArgumentException("Should be at least one language");
        try {
            return recommendationService.getRecommendationsFor(userId,excludes, languages, size)
                    .stream().map(Video::toModel).collect(Collectors.toList());
        } catch (NotFoundException e) {
            logger.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Video watchById(Long videoId, String userId) throws NotFoundException{
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        videoRepository.incrementViewsById(videoId);
        if(userId != null && !userId.isEmpty()){
            userRepository.findById(Long.parseLong(userId)).ifPresent(userEntity -> {
                UserMetadata userMetadata;
                if(userEntity.getUserMetadata() == null){
                    userMetadata = new UserMetadata(userEntity);
                } else {
                    userMetadata = userEntity.getUserMetadata();
                }
                userMetadata.incrementLanguage(videoEntity.getVideoMetadata().getLanguage());
                userMetadata.incrementCategory(videoEntity.getVideoMetadata().getCategory());
                userEntity.setWatchHistory(
                        userEntity.getWatchHistory().stream().filter(el ->{
                            if(el.getVideoId() == videoId){
                                watchHistoryRepository.deleteById(el.getId());
                                return false;
                            }
                            return true;
                        }).collect(Collectors.toList())
                );
                userEntity.getWatchHistory().removeIf(el ->{
                    if(el.getDate().isAfter(LocalDateTime.now().minus(1, ChronoUnit.DAYS))&& Objects.equals(el.getVideoId(), videoId)){
                        watchHistoryRepository.deleteById(el.getId());
                        return true;
                    }
                    return false;
                });
                userEntity.getWatchHistory().add(new WatchHistory(null, userEntity, videoId));
                userRepository.save(userEntity);
            });
        }
        return Video.toModel(videoEntity);
    }


    public File convertToHls(File video) throws IOException, InterruptedException {
        String withoutExtension = StringUtils.stripFilenameExtension(video.getName());
        Path newDir = Path.of(AppConstants.VIDEO_PATH + withoutExtension);
        if(!Files.exists(newDir)){
            Files.createDirectory(newDir);
            Files.move(video.toPath(), newDir.resolve(video.getName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return ffmpeg.convertVideoToHls(new File(newDir.toFile() + "/" + video.getName()));
    }

    public File m3u8Index(Long videoId) throws IOException, InterruptedException {
        if (Files.exists(Path.of(AppConstants.VIDEO_PATH + videoId + "/index.m3u8"))) {
            return new File(AppConstants.VIDEO_PATH + videoId + "/index.m3u8");
        } else return null;
    }

    public File ts(Long id,String filename){
        return new File(AppConstants.VIDEO_PATH + id + "/" + filename);
    }



    public Optional<VideoEntity> create(VideoCreateRequest video, Long userId)  throws Exception{
        return create(video.title(), video.description(), video.category(), video.thumbnail().getBytes(), video.video().getBytes(), userId);
    }

    public Optional<VideoEntity> create(String title, String description, String category, File thumbnail, File video, Long userId)  throws Exception{
        try(FileInputStream thumbnailInputStream = new FileInputStream(thumbnail);
            FileInputStream videoInputStream = new FileInputStream(video)){
            return create(title, description, category, thumbnailInputStream.readAllBytes(), videoInputStream.readAllBytes(), userId);
        }
    }

    @Transactional
    private Optional<VideoEntity> create(String title, String description, String category, byte[] thumbnail, byte[] video, Long userId) throws Exception{
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not found");
        LanguageDetector languageDetector = new OptimaizeLangDetector().loadModels();
        Path folder = null;
        try {
            Integer duration = (int)Float.parseFloat(MediaUtils.getDuration(video));
            String language = languageDetector.detect(title).getLanguage();
            VideoEntity savedEntity = videoRepository.save(Video.toEntity(title, description, optionalUserEntity.get()));
            videoMetadataRepository.save(new VideoMetadata(savedEntity, language, duration, category));

            folder = Path.of(AppConstants.VIDEO_PATH + savedEntity.getId());
            Files.createDirectory(folder);
            ImageUtils.compressAndSave(thumbnail, new File(folder.toString(), Video.THUMBNAIL_FILENAME));
            Path videoFile = Path.of(folder + "/" + "index.mp4");
            Files.write(videoFile, video);
            ffmpeg.convertVideoToHls(videoFile.toFile());
            return Optional.of(savedEntity);
        } catch (Exception e) {
            if(folder != null){
                try {
                    FileUtils.deleteDirectory(folder.toFile());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new Exception("Could not create video uploaded from client cause: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteById(Long id) throws NotFoundException, IOException {
        if(!videoRepository.existsById(id)) throw new NotFoundException("Video not found");
        VideoEntity videoEntity = videoRepository.getReferenceById(id);
        likeRepository.deleteAllById(videoEntity.getLikes().stream().map(Like::getId).toList());
        watchHistoryRepository.deleteAllByVideoId(id);
        videoRepository.deleteById(id);
        Files.deleteIfExists(Path.of(AppConstants.VIDEO_PATH + Video.THUMBNAIL_FILENAME));
        FileUtils.deleteDirectory(new File(AppConstants.VIDEO_PATH + videoEntity.getId()));
    }

    public void update(VideoUpdateRequest updateRequest) throws NotFoundException, IOException, InterruptedException {
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
            ImageUtils.compressAndSave(updateRequest.thumbnail().getBytes(), new File(AppConstants.VIDEO_PATH + videoEntity.getId() + Video.THUMBNAIL_FILENAME));
        }
        if(updateRequest.video() != null){
            for(File file : Objects.requireNonNull(new File(AppConstants.VIDEO_PATH + videoEntity.getId()).listFiles())){
                if(!file.getName().equals(Video.THUMBNAIL_FILENAME)){
                    Files.deleteIfExists(file.toPath());
                }
            }
            Path videoFile = Path.of(AppConstants.VIDEO_PATH + videoEntity.getId() + "index.mp4");
            updateRequest.video().transferTo(videoFile);
            ffmpeg.convertVideoToHls(videoFile.toFile());
        }
        if(updateRequest.category() != null){
            videoEntity.getVideoMetadata().setCategory(updateRequest.category());
        }
        videoRepository.save(videoEntity);
    }

    public void testMethod(){
        //преобразовать в новый формат все видео
        long start = System.currentTimeMillis();
        videoRepository.findAll().forEach(ve -> {
//            if(!Files.exists(Path.of(AppConstants.VIDEO_PATH + ve.getId()))){
//                try {
//                    logger.trace("Processing vide entity id: " + ve.getId());
//                    File videoDir = new File(AppConstants.VIDEO_PATH + StringUtils.stripFilenameExtension(ve.getVideoPath()));
//                    if(!videoDir.exists()){
//                        Files.createDirectory(Path.of(AppConstants.VIDEO_PATH + ve.getId()));
//                    }
//                    else{
//                        logger.trace("Dir renamed: " + videoDir.renameTo(new File(AppConstants.VIDEO_PATH + ve.getId())));
//                    }
//                    File thumbnailFile = Files.copy(Path.of(AppConstants.THUMBNAIL_PATH + ve.getThumbnail()), Path.of(AppConstants.VIDEO_PATH + ve.getId() + "/" + ve.getThumbnail())).toFile();
//                    logger.trace("Thumbnail file renamed: " + thumbnailFile.renameTo(new File(AppConstants.VIDEO_PATH + ve.getId() + "/" + Video.THUMBNAIL_FILENAME)));
//                    File videoFile = new File(AppConstants.VIDEO_PATH + ve.getId() + "/" + ve.getVideoPath());
//                    logger.trace("Video file " + videoFile.getName() + " : " + videoFile.renameTo(new File(AppConstants.VIDEO_PATH + ve.getId() + "/" + "index.mp4")));
//                    File m3u8File = new File(AppConstants.VIDEO_PATH + ve.getId() + "/" + StringUtils.stripFilenameExtension(ve.getVideoPath()) + ".m3u8");
//                    logger.trace("Video file renamed: " + m3u8File.renameTo(new File(AppConstants.VIDEO_PATH + ve.getId() + "/" + "index.m3u8")));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

        });
        logger.info("Refactoring completed in " + (System.currentTimeMillis() - start) + "ms");
    }
    @Transactional
    public String addVideos(int amount) throws InterruptedException {
        long start = System.currentTimeMillis();
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
        Runnable task = () -> {
            TransactionTemplate template = new TransactionTemplate(platformTransactionManager);
            template.execute(status -> {
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
                    logger.error(e.getMessage());
                }
                return null;
            });
        };
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        for(int i = 0; i< amount; i++){
            executor.execute(task);
            Thread.sleep(1);
        }
        executor.shutdown();
        executor.awaitTermination(200, TimeUnit.HOURS);
        return "Completed in " + ((float) (System.currentTimeMillis() - start) / 1000) + "s";
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
