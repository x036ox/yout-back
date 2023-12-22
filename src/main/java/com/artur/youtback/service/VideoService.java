package com.artur.youtback.service;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.VideoMetadata;
import com.artur.youtback.entity.user.UserMetadata;
import com.artur.youtback.entity.user.WatchHistory;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.repository.*;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
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
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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



    public List<Video> findAll(SortOption sortOption) throws VideoNotFoundException{

        if(sortOption != null){
            return videoRepository.findAll().stream().limit(AppConstants.MAX_VIDEOS_PER_REQUEST)
                    .sorted(SortOptionsComparators.get(sortOption))
                    .map(Video::toModel).toList();
        }

        return videoRepository.findAll().stream().limit(AppConstants.MAX_VIDEOS_PER_REQUEST)
                .map(Video::toModel).toList();
    }

    public Video findById(Long id) throws VideoNotFoundException{
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(id);
        if(optionalVideoEntity.isEmpty()) throw new VideoNotFoundException("Video not Found");

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
        } catch (UserNotFoundException e) {
            logger.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Video watchById(Long videoId, String userId) throws VideoNotFoundException{
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new VideoNotFoundException("Video not found"));
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
                List<WatchHistory> toDelete = new ArrayList<>();
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

    public InputStream getVideoStreamById(Long id) throws FileNotFoundException {
        String videoFilename = videoRepository.findVideoFilenameById(id);
        File video = new File(AppConstants.VIDEO_PATH + videoFilename);
        return new FileInputStream(video);
    }

    public File convertToHls(File video) throws IOException, InterruptedException {
        String withoutExtension = StringUtils.stripFilenameExtension(video.getName());
        Path newDir = Path.of(AppConstants.VIDEO_PATH + withoutExtension);
        if(!Files.exists(newDir)){
            Files.createDirectory(newDir);
            Files.move(video.toPath(), newDir.resolve(video.getName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return MediaUtils.convertVideoToHls(new File(newDir.toFile() + "/" + video.getName()), Arrays.stream(environment.getActiveProfiles()).anyMatch(el -> el.equals("dev")));
    }

    public File m3u8Index(Long videoId) throws IOException, InterruptedException {
        String videoFilename = videoRepository.findVideoFilenameById(videoId);
        String withoutExtension = StringUtils.stripFilenameExtension(videoFilename);
        if (Files.exists(Path.of(AppConstants.VIDEO_PATH + withoutExtension + "/" + withoutExtension + ".m3u8"))) {
            return new File(AppConstants.VIDEO_PATH + withoutExtension + "/" + withoutExtension + ".m3u8");
        }else {
            return convertToHls(new File(AppConstants.VIDEO_PATH + videoFilename));
        }
    }

    public File ts(String filename){
        String dir = filename.substring(0, filename.lastIndexOf("_"));
        return new File(AppConstants.VIDEO_PATH + dir + "/" + filename);
    }



    public void create(VideoCreateRequest video, Long userId)  throws Exception{
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        String filename = Long.toString(System.currentTimeMillis());
        String thumbnailFilename = filename  + "." + ImageUtils.IMAGE_FORMAT;
        String videoFilename = filename  + "." + video.video().getContentType().substring(video.video().getContentType().lastIndexOf("/") + 1);
        LanguageDetector languageDetector = new OptimaizeLangDetector().loadModels();
        try {
            ImageUtils.compressAndSave(video.thumbnail().getBytes(), new File(AppConstants.THUMBNAIL_PATH + thumbnailFilename));
            Path newDir = Path.of(AppConstants.VIDEO_PATH + filename);
            Files.createDirectory(newDir);
            Path videoFile = Path.of(newDir + "/" + videoFilename);
            Files.write(videoFile, video.video().getBytes());
            MediaUtils.convertVideoToHls(videoFile.toFile(), Arrays.stream(environment.getActiveProfiles()).anyMatch(el -> el.equals("dev")));
            Integer duration = (int)Float.parseFloat(MediaUtils.getDuration(video.video()));
            String language = languageDetector.detect(video.title()).getLanguage();
            VideoEntity savedEntity = videoRepository.save(Video.toEntity(video.title(), video.description(), thumbnailFilename, videoFilename, optionalUserEntity.get()));
            videoMetadataRepository.save(new VideoMetadata(savedEntity, language, duration, video.category()));
        } catch (Exception e) {
            throw new Exception("Could not save file " + video.thumbnail().getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }
    }

    public Optional<VideoEntity> create(String title, String description, String category, File thumbnail, File video, Long userId)  throws Exception{
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        logger.debug("Started creating video, title: " + title);
        String filename = Long.toString(System.currentTimeMillis());
        String thumbnailFilename = filename  + "." + ImageUtils.IMAGE_FORMAT;
        String videoFilename = filename  + "." + StringUtils.getFilenameExtension(video.getName());
        LanguageDetector languageDetector = new OptimaizeLangDetector().loadModels();
        try {
            String language = languageDetector.detect(title).getLanguage();
            VideoEntity entityToSave = Video.toEntity(title, description, thumbnailFilename, videoFilename, optionalUserEntity.get());
            VideoEntity savedEntity = videoRepository.save(entityToSave);

            ImageUtils.compressAndSave(Files.readAllBytes(thumbnail.toPath()), new File(AppConstants.THUMBNAIL_PATH + thumbnailFilename));
            Path newDir = Path.of(AppConstants.VIDEO_PATH + filename);
            Files.createDirectory(newDir);
            Path videoFile = Path.of(newDir + "/" + videoFilename);
            Files.write(videoFile, Files.readAllBytes(video.toPath()));
            Integer duration = (int)Float.parseFloat(MediaUtils.getDuration(videoFile.toFile()));
            MediaUtils.convertVideoToHls(videoFile.toFile(), Arrays.stream(environment.getActiveProfiles()).anyMatch(el -> el.equals("dev")));

            videoMetadataRepository.save(new VideoMetadata(savedEntity, language, duration, category));
            logger.debug("Finished creating video, title: " + title);
            return Optional.of(savedEntity);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }

    }

    @Transactional
    public void deleteById(Long id) throws VideoNotFoundException, IOException {
        if(!videoRepository.existsById(id)) throw new VideoNotFoundException("Video not found");
        VideoEntity videoEntity = videoRepository.getReferenceById(id);
        likeRepository.deleteAllById(videoEntity.getLikes().stream().map(Like::getId).toList());
        watchHistoryRepository.deleteAllByVideoId(id);
        videoRepository.deleteById(id);
        Files.deleteIfExists(Path.of(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()));
        FileUtils.deleteDirectory(new File(AppConstants.VIDEO_PATH + StringUtils.stripFilenameExtension(videoEntity.getVideoPath())));
    }

    public String saveThumbnail(MultipartFile thumbnail) throws Exception{
        try{
            String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
            ImageUtils.compressAndSave(thumbnail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + filename));
            return filename;
        } catch (IOException e){
            logger.error("Could not save file " + thumbnail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
            throw new Exception("Could not save file " + thumbnail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }
    }

    public void update(VideoUpdateRequest updateRequest) throws VideoNotFoundException, IOException, InterruptedException {
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(updateRequest.videoId());
        if(optionalVideoEntity.isEmpty()) throw new VideoNotFoundException("Video not Found");

        //data allowed to update
        VideoEntity videoEntity = optionalVideoEntity.get();
        if(updateRequest.description() != null){
            videoEntity.setDescription(updateRequest.description());
        }
        if(updateRequest.title() != null){
            videoEntity.setTitle(updateRequest.title());
        }
        if(updateRequest.thumbnail() != null){
            Files.deleteIfExists(Path.of(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()));
            ImageUtils.compressAndSave(updateRequest.thumbnail().getBytes(), new File(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()));
            videoEntity.setThumbnail(videoEntity.getThumbnail());
        }
        if(updateRequest.video() != null){
            File videoDirectory = new File(AppConstants.VIDEO_PATH + StringUtils.stripFilenameExtension(videoEntity.getVideoPath()));
            FileUtils.cleanDirectory(videoDirectory);
            Path videoFile = Path.of(videoDirectory.getPath() + "/" + videoEntity.getVideoPath());
            updateRequest.video().transferTo(videoFile);
            MediaUtils.convertVideoToHls(videoFile.toFile(), Arrays.stream(environment.getActiveProfiles()).anyMatch(el -> el.equals("dev")));
        }
        if(updateRequest.category() != null){
            videoEntity.getVideoMetadata().setCategory(updateRequest.category());
        }
        videoRepository.save(videoEntity);
    }

    public void testMethod(){

    }
    @Transactional
    public String addVideos(int amount) throws InterruptedException {
        Long start = System.currentTimeMillis();
        String[] categories = {"Sport", "Music", "Education", "Movies", "Games", "Other"};
        String[][] titles = {{"Football", "Basketball", "Hockey", "Golf"}, {"Eminem", "XXXTentacion", "Drake", "Три дня дождя", "Playboi Carti","Yeat"}, {"Java", "Php", "English", "French", "C#", "C++"}, {"Oppenheimer", "American psycho", "Good fellas","Fight club","Breaking bad", "The boys"}, {"GTA V", "GTA San Andreas", "GTA IV", "Fortnite", "Minecraft", "Need For Speed Most Wanted"}, {"Monkeys", "Cars", "Dogs", "Cats", "Nature"}};
        File[][] thumbnails = {new File("video thumbnails to create/sport").listFiles(),
                new File("video thumbnails to create/music").listFiles(),
                new File("video thumbnails to create/education").listFiles(),
                new File("video thumbnails to create/movies").listFiles(),
                new File("video thumbnails to create/games").listFiles(),
                new File("video thumbnails to create/other").listFiles()
        };
        File[][] videos = {new File("videos to create/sport").listFiles(),
                new File("videos to create/music").listFiles(),
                new File("videos to create/education").listFiles(),
                new File("videos to create/movies").listFiles(),
                new File("videos to create/games").listFiles(),
                new File("videos to create/other").listFiles()
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
