package com.artur.youtback.service;

import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.LikeRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    LikeRepository likeRepository;
    @Autowired
    VideoRepository videoRepository;



    public Collection<VideoEntity> getRecommendationsFor(@NotNull Long userId,@NotEmpty String... browserLanguages) throws UserNotFoundException {
        Map<Long, VideoEntity> videos = new HashMap<>(AppConstants.MAX_VIDEOS_PER_REQUEST);
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        for(int i = 0; i < AppConstants.MAX_VIDEOS_PER_REQUEST; i++){
            //finding most popular videos by likes with supported user languages
            Optional<VideoEntity> recommendation;
            String[] userLanguages = userEntity.getUserMetadata().getLanguagesDec().keySet().toArray(String[]::new);
            recommendation = getOneWithAllLanguages(videos.keySet(), userLanguages);
            if(recommendation.isPresent()) {
                VideoEntity video = recommendation.get();
                videos.put(video.getId(), video);
                continue;
            }

            //finding with browser language (if it's not necessary we ain't going to be there)
            recommendation = getOneWithAllLanguages(videos.keySet(), browserLanguages);
            if(recommendation.isPresent()) {
                VideoEntity video = recommendation.get();
                videos.put(video.getId(), video);
                continue;
            }
            System.out.println("NOT FOUND WITH USER AND BROWSER LANGUAGE");
        }
        return videos.values();
    }




    public Collection<VideoEntity> getRecommendations(@NotEmpty String... languages){
        Map<Long, VideoEntity> videos = new HashMap<>(AppConstants.MAX_VIDEOS_PER_REQUEST);
        for(int i = 0; i < AppConstants.MAX_VIDEOS_PER_REQUEST; i++) {
            Optional<VideoEntity> recommendation = getOneWithAllLanguages(videos.keySet(), languages);
            if(recommendation.isPresent()) {
                VideoEntity video = recommendation.get();
                videos.put(video.getId(), video);
                continue;
            }
            //there if not found with every language (maybe we need to expand popularity boundaries)
            System.out.println("NOT FOUND WITH EVERY LANGUAGE " + String.join(" ", languages));

            break;
        }
        return videos.values();
    }

    //array languages should be ordered by priority
    private Optional<VideoEntity> getOneWithAllLanguages(Set<Long> exceptions, String[] languages){
        final int MAX_POPULARITY_DAYS_EXTENSION = 30;
       for(int i = 0;i < MAX_POPULARITY_DAYS_EXTENSION; i++){
           Optional<VideoEntity> video = getOneWithAllLanguages(exceptions, i, languages);
           if(video.isPresent()) return video;
       }
       return Optional.empty();
    }

    private Optional<VideoEntity> getOneWithAllLanguages(Set<Long> exceptions, int popularityDaysExtension, String[] languages){
        for (String language:languages) {
            Optional<VideoEntity> recommendation = getOne(language, popularityDaysExtension, exceptions);
            if(recommendation.isPresent()) return recommendation;
        }
        return Optional.empty();
    }
    private Optional<VideoEntity> getOne(String language, int popularityDaysExtension, Set<Long> exceptions){
        return likeRepository.findFastestGrowingVideosWithExceptions(
                Instant.now().minus(AppConstants.POPULARITY_DAYS + popularityDaysExtension, ChronoUnit.DAYS),
                language,
                exceptions,
                Pageable.ofSize(1)).stream().findFirst();
    }
}
