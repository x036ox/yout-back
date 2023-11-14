package com.artur.youtback.service;

import com.artur.youtback.entity.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.SortOption;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public void deleteById(Long id) throws VideoNotFoundException{
        if(!videoRepository.existsById(id)) throw new VideoNotFoundException("Video Not Found");
        videoRepository.deleteById(id);
    }

    public void update(Video video, Long id) throws  VideoNotFoundException{
        Optional<VideoEntity> optionalVideoEntity = videoRepository.findById(id);
        if(optionalVideoEntity.isEmpty()) throw new VideoNotFoundException("Video not Found");

        //data allowed to update
        VideoEntity videoEntity = optionalVideoEntity.get();
        if(video.description() != null){
            videoEntity.setDescription(video.description());
        }
        if(video.duration() != null){
            videoEntity.setDuration(video.duration());
        }
        if(video.title() != null){
            videoEntity.setTitle(video.title());
        }
        if(video.thumbnail() != null){
            videoEntity.setThumbnail(video.thumbnail());
        }

        videoRepository.save(videoEntity);
    }



}
