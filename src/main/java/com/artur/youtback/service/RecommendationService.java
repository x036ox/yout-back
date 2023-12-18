package com.artur.youtback.service;

import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.repository.LikeRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.AppConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    UserRepository userRepository;
    @Autowired
    LikeRepository likeRepository;
    @Autowired
    VideoRepository videoRepository;



    public Collection<VideoEntity> getRecommendationsFor(@NotNull Long userId,Set<Long> excludes, @NotEmpty String... browserLanguages) throws UserNotFoundException {
        Set<Long> excludedIds;
        if(excludes != null && !excludes.isEmpty()){
            excludedIds = new HashSet<>(excludes.size() + AppConstants.MAX_VIDEOS_PER_REQUEST);
            excludedIds.addAll(excludes);
        } else{
            excludedIds = new HashSet<>(AppConstants.MAX_VIDEOS_PER_REQUEST);
        }
        Set <VideoEntity> videos = new HashSet<>(AppConstants.MAX_VIDEOS_PER_REQUEST);
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        for(int i = 0; i < AppConstants.MAX_VIDEOS_PER_REQUEST; i++){
            //finding most popular videos by likes with supported user languages
            Optional<VideoEntity> recommendation = null;
            String[] userLanguages = userEntity.getUserMetadata().getLanguagesDec().keySet().toArray(String[]::new);
            Set<String> userCategories = userEntity.getUserMetadata().getCategoriesDec().keySet();
            if(!userCategories.isEmpty()){
                recommendation = getOneByCategoriesAndLanguages(excludedIds, userCategories, userLanguages);
            } else if(recommendation == null || recommendation.isEmpty()) {
                recommendation = getOneByLanguages(excludedIds, userLanguages);
            }
            if(recommendation.isPresent()) {
                VideoEntity video = recommendation.get();
                excludedIds.add(video.getId());
                videos.add(video);
                continue;
            }

            //finding with browser language (if it's not necessary we ain't going to be there)
            recommendation = getOneByLanguages(excludedIds, browserLanguages);
            if(recommendation.isPresent()) {
                VideoEntity video = recommendation.get();
                excludedIds.add(video.getId());
                videos.add(video);
                continue;
            }
            logger.warn("Recommendation not found with user and browser languages for user: " + userId);
        }
        return videos;
    }




    public Collection<VideoEntity> getRecommendations(Set<Long> excludes, @NotEmpty String... languages){
        Set<Long> excludedIds;
        if(excludes != null && !excludes.isEmpty()){
            excludedIds = new HashSet<>(excludes.size() + AppConstants.MAX_VIDEOS_PER_REQUEST);
        } else{
            excludedIds = new HashSet<>(AppConstants.MAX_VIDEOS_PER_REQUEST);
        }
        Set<VideoEntity> videos = new HashSet<>(AppConstants.MAX_VIDEOS_PER_REQUEST);
        for(int i = 0; i < AppConstants.MAX_VIDEOS_PER_REQUEST; i++) {
            Optional<VideoEntity> recommendation = getOneByLanguages(excludedIds, languages);
            if(recommendation.isPresent()) {
                VideoEntity video = recommendation.get();
                excludedIds.add(video.getId());
                videos.add(video);
                continue;
            }
            //there if not found with every language (maybe we need to expand popularity boundaries)
            logger.warn("(Anonymous user)Recommendations not found with every language: " + String.join(", ", languages));
            break;
        }
        return videos;
    }

    private Optional<VideoEntity> getOneByCategoriesAndLanguages(Set<Long> exceptions, Set<String> categories, String[] languages){
        for(String category:categories){
            for (String language:languages) {
                Optional<VideoEntity> video = getOne(category, language,0, exceptions);
                if(video.isPresent()) return video;
            }
        }
        return Optional.empty();
    }

    //array languages should be ordered by priority
    private Optional<VideoEntity> getOneByLanguages(Set<Long> exceptions, String[] languages){
        final int MAX_POPULARITY_DAYS_EXTENSION = 30;
       for(int i = 0;i < MAX_POPULARITY_DAYS_EXTENSION; i++){
           Optional<VideoEntity> video = getOneByLanguages(exceptions, i, languages);
           if(video.isPresent()) return video;
       }
       return Optional.empty();
    }

    private Optional<VideoEntity> getOneByLanguages(Set<Long> exceptions, int popularityDaysExtension, String[] languages){
        for (String language:languages) {
            Optional<VideoEntity> recommendation = getOne(language, popularityDaysExtension, exceptions);
            if(recommendation.isPresent()) return recommendation;
        }
        return Optional.empty();
    }
    private Optional<VideoEntity> getOne(String language, int popularityDaysExtension, Set<Long> exceptions){
        return likeRepository.findRecommendations(
                Instant.now().minus(AppConstants.POPULARITY_DAYS + popularityDaysExtension, ChronoUnit.DAYS),
                language,
                exceptions,
                Pageable.ofSize(1)).stream().findFirst();
    }

    private Optional<VideoEntity> getOne(String category, String language, int popularityDaysExtension, Set<Long> exceptions){
        return likeRepository.findRecommendations(
                Instant.now().minus(AppConstants.POPULARITY_DAYS + popularityDaysExtension, ChronoUnit.DAYS),
                category,
                language,
                exceptions,
                Pageable.ofSize(1)).stream().findFirst();
    }
}
