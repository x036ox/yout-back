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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public void create(Video video, Long userId)  throws UserNotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        videoRepository.save(Video.toEntity(video, optionalUserEntity.get()));
    }

    public void create(String title, String description, String duration, MultipartFile thumbnail, Long userId)  throws Exception{
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
        try {
            ImageUtils.compressAndSave(thumbnail.getBytes(), new File(AppConstants.THUMBNAIL_PATH + filename));
        } catch (IOException e) {
            throw new Exception("Could not save file " + thumbnail.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }

        videoRepository.save(Video.toEntity(title, description, duration, filename, optionalUserEntity.get()));
    }

    public void deleteById(Long id) throws VideoNotFoundException{
        if(!videoRepository.existsById(id)) throw new VideoNotFoundException("Video Not Found");
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
            } else if(option.equals(FindOptions.VideoOptions.BY_LIKES_LESS_THEN.name()) && value != null){
                return videoRepository.findByLikesLessThen(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.BY_LIKES_MORE_THEN.name()) && value != null){
                return videoRepository.findByLikesMoreThen(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.BY_VIEWS_LESS_THEN.name()) && value != null){
                return videoRepository.findByViewsLessThen(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.BY_VIEWS_MORE_THEN.name()) && value != null){
                return videoRepository.findByViewsMoreThen(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.VideoOptions.BY_TITLE.name()) && value != null){
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
