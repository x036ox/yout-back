package com.artur.youtback.service;

import com.artur.youtback.entity.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserRepository userRepository;

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

    @Transactional
    public Video watchById(Long videoId) throws VideoNotFoundException{
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(videoId);
        if(optionalVideoEntity.isEmpty()) throw new VideoNotFoundException("User not found");
        videoRepository.incrementViewsById(videoId);

        return Video.toModel(optionalVideoEntity.get());

    }

    public InputStream getVideoStreamById(Long id) throws FileNotFoundException {
        String videoFilename = videoRepository.findVideoFilenameById(id);
        File video = new File(AppConstants.VIDEO_PATH + videoFilename);
        return new FileInputStream(video);
    }

    public void create(Video video, String videoPath, Long userId)  throws UserNotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        videoRepository.save(Video.toEntity(video,videoPath, optionalUserEntity.get()));
    }

    public void create(String title, String description, String duration, MultipartFile thumbnail, MultipartFile video, Long userId)  throws Exception{
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        String filename = Long.toString(System.currentTimeMillis());
        String thumbnailFilename = filename  + "." + ImageUtils.IMAGE_FORMAT;
        String videoFilename = filename  + "." + video.getContentType().substring(video.getContentType().lastIndexOf("/") + 1);
        try {
            ImageUtils.compressAndSave(thumbnail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + thumbnailFilename));
            video.transferTo(Path.of(AppConstants.VIDEO_PATH + videoFilename));
        } catch (IOException e) {
            throw new Exception("Could not save file " + thumbnail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }

        videoRepository.save(Video.toEntity(title, description, duration, thumbnailFilename, videoFilename, optionalUserEntity.get()));
    }

    public void deleteById(Long id) throws VideoNotFoundException, IOException {
        VideoEntity videoEntity = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException("Video Not Found"));
        Files.deleteIfExists(Path.of(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()));
        Files.deleteIfExists(Path.of(AppConstants.VIDEO_PATH + videoEntity.getVideoPath()));
        videoRepository.deleteById(id);
    }

    public String saveThumbnail(MultipartFile thumbail) throws Exception{
        try{
            String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
            ImageUtils.compressAndSave(thumbail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + filename));
            return filename;
        } catch (IOException e){
            throw new Exception("Could not save file " + thumbail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
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
        if(duration != null){
            videoEntity.setDuration(TimeOperations.toSeconds(duration, "HH:mm:ss"));
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

    private static class Tools{
        static List<VideoEntity> findByOption(String option, String value, VideoRepository videoRepository) throws IllegalArgumentException{
            if(option.equals(FindOptions.VideoOptions.BY_ID.name()) && value != null){
                return videoRepository.findById(Long.parseLong(value)).stream().toList();
            } else if(option.equals(FindOptions.VideoOptions.BY_LIKES.name()) && value != null){
                String[] fromTo = value.split("/");
                if(fromTo.length == 2){
                    return videoRepository.findByLikes(fromTo[0],fromTo[1], Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
                } // else throwing exception below
            } else if(option.equals(FindOptions.VideoOptions.BY_VIEWS.name()) && value != null){
                String[] fromTo = value.split("/");
                if(fromTo.length == 2){
                    return videoRepository.findByViews(fromTo[0],fromTo[1], Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
                } // else throwing exception below
            }  else if(option.equals(FindOptions.VideoOptions.BY_TITLE.name()) && value != null){
                return videoRepository.findByTitle(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.MOST_DURATION.name())){
                return videoRepository.findMostDuration(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.MOST_LIKES.name())){
                return videoRepository.findMostLikes(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.MOST_VIEWS.name())){
                return videoRepository.findMostViews(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            }
            throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
        }
    }

}
