package com.artur.youtback.service;

import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.VideoMetadata;
import com.artur.youtback.entity.user.UserMetadata;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.*;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {

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

    public Collection<Video> recommendations(Long userId, String... languages) throws IllegalArgumentException{
        InputStream inputStream;
        if(languages.length == 0) throw new IllegalArgumentException("Should be at least one language");
        try {
            List<VideoEntity> result;
            if(userId != null){
                result = new ArrayList<>(recommendationService.getRecommendationsFor(userId,languages));
            } else {
                result = new ArrayList<>(recommendationService.getRecommendations(languages));
            }
            Collections.shuffle(result);
            return result.stream().map(Video::toModel).collect(Collectors.toList());
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Transactional
    public Video watchById(Long videoId, String userId) throws VideoNotFoundException{
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new VideoNotFoundException("User not found"));
        videoRepository.incrementViewsById(videoId);
        if(userId != null && !userId.isEmpty()){
            userRepository.findById(Long.parseLong(userId)).ifPresent(userEntity -> {
                UserMetadata userMetadata;
                if(userEntity.getUserMetadata() == null){
                    userMetadata = new UserMetadata(userEntity);
                } else {
                    userMetadata = userEntity.getUserMetadata();
                }
                userMetadata.addLanguage(videoEntity.getVideoMetadata().getLanguage());
                userMetadataRepository.save(userMetadata);
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
        System.out.println("CONVERTING " + newDir.toFile() + video.getName());
        return MediaUtils.convertVideoToHls(new File(newDir.toFile() + "/" + video.getName()));
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

    public void create(Video video, String videoPath, Long userId)  throws UserNotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        videoRepository.save(Video.toEntity(video,videoPath, optionalUserEntity.get()));
    }

    public void create(String title, String description, MultipartFile thumbnail, MultipartFile video, Long userId)  throws Exception{
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        String filename = Long.toString(System.currentTimeMillis());
        String thumbnailFilename = filename  + "." + ImageUtils.IMAGE_FORMAT;
        String videoFilename = filename  + "." + video.getContentType().substring(video.getContentType().lastIndexOf("/") + 1);
        LanguageDetector languageDetector = new OptimaizeLangDetector().loadModels();
        try {
            ImageUtils.compressAndSave(thumbnail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + thumbnailFilename));
            Path newDir = Path.of(AppConstants.VIDEO_PATH + filename);
            Files.createDirectory(newDir);
            Path videoFile = Path.of(newDir + "/" + videoFilename);
            video.transferTo(videoFile);
            MediaUtils.convertVideoToHls(videoFile.toFile());
            Integer duration = (int)Float.parseFloat(MediaUtils.getDuration(video));
            String language = languageDetector.detect(title).getLanguage();
            VideoEntity savedEntity = videoRepository.save(Video.toEntity(title, description, thumbnailFilename, videoFilename, optionalUserEntity.get()));
            videoMetadataRepository.save(new VideoMetadata(savedEntity, language, duration));
        } catch (Exception e) {
            throw new Exception("Could not save file " + thumbnail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }

    }

    public void deleteById(Long id) throws VideoNotFoundException, IOException {
        VideoEntity videoEntity = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException("Video Not Found"));
        Files.deleteIfExists(Path.of(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()));
        Files.deleteIfExists(Path.of(AppConstants.VIDEO_PATH + videoEntity.getVideoPath()));
        videoRepository.deleteById(id);
    }

    public String saveThumbnail(MultipartFile thumbnail) throws Exception{

        try{
            String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
            ImageUtils.compressAndSave(thumbnail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + filename));
            return filename;
        } catch (IOException e){
            throw new Exception("Could not save file " + thumbnail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }
    }

    public void update(Long id, String title, String description, String duration, MultipartFile thumbnail) throws VideoNotFoundException, IOException {
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(id);
        if(optionalVideoEntity.isEmpty()) throw new VideoNotFoundException("Video not Found");

        //data allowed to update
        VideoEntity videoEntity = optionalVideoEntity.get();
        if(description != null){
            videoEntity.setDescription(description);
        }
        if(title != null){
            videoEntity.setTitle(title);
        }
        if(thumbnail != null){
            String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
            Files.delete(Path.of(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()));
            ImageUtils.compressAndSave(thumbnail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + filename));
            videoEntity.setThumbnail(filename);
        }

        videoRepository.save(videoEntity);
    }

    public void generationTest(){
//        List<VideoEntity> videoEntities = videoRepository.findAll();
//        List<UserEntity> userEntities = userRepository.findAll();
//        String[] languages = {"ru", "en", "uk"};
//        videoEntities.forEach(videoEntity -> {
//            if(videoEntity.getVideoMetadata() == null){
//                System.out.println("metadata is null for video ID: " + videoEntity.getId());
//                int index = (int)Math.floor(Math.random() * languages.length);
//                videoEntity.setDuration(videoEntity.getDuration() + 1);
//                VideoEntity saved = videoRepository.save(videoEntity);
//                videoMetadataRepository.save(new VideoMetadata(null,saved, languages[index]));
//            }
//
//            userEntities.forEach(userEntity -> {
//                int sec = (int)Math.floor(Math.random() * 345600);
//                likeRepository.save(Like.create(userEntity, videoEntity, Instant.now().minusSeconds(sec)));
//            });
//        });
    }

    public void testMethod(){
        List<VideoEntity> videoEntities = videoRepository.findAll();
        videoEntities.forEach(videoEntity -> {
            try {
                videoEntity.getVideoMetadata().setDuration((int) Float.parseFloat(MediaUtils.getDuration(new File(AppConstants.VIDEO_PATH + videoEntity.getVideoPath()))));
            } catch (TikaException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        });
        videoRepository.saveAll(videoEntities);
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
