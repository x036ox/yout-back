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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RecommendationService {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    UserRepository userRepository;
    @Autowired
    LikeRepository likeRepository;
    @Autowired
    VideoRepository videoRepository;



    public List<VideoEntity> getRecommendationsFor(@Nullable Long userId,@NotNull Set<Long> excludes, @NotEmpty String[] browserLanguages, int size) throws UserNotFoundException {
        final int RECS_SIZE = Math.min(size, AppConstants.MAX_VIDEOS_PER_REQUEST);
        List <VideoEntity> videos = new ArrayList<>();

        //find RECS_SIZE with categories, RECS_SIZE without categories if resulting list >= RECS_SIZE shuffle limit to RECS_SIZE and return
        //if resulting list < RECS_SIZE find just some popular videos, shuffle and return
        if(userId != null && userRepository.existsById(userId)){
            videos.addAll(getByCategoriesAndLanguages(userId, excludes, RECS_SIZE));
        }
        if(videos.size() < RECS_SIZE){
            //finding with browser language (if it's not necessary we ain't going to be there)
            logger.warn("FINDING WITH BROWSER LANGUAGE");
            videos.addAll(getByLanguages(
                    Stream.concat(excludes.stream(), videos.stream().map(VideoEntity::getId)).collect(Collectors.toSet()),
                    browserLanguages, RECS_SIZE - videos.size()));
        }
        //finding random popular videos
        if(videos.size() < RECS_SIZE){
            logger.warn("Recommendation not found with user and browser languages for user: " + userId);
            videos.addAll(getSomePopularVideos(
                    Stream.concat(excludes.stream(), videos.stream().map(VideoEntity::getId)).collect(Collectors.toSet()),
                    RECS_SIZE - videos.size()));
        }
        Collections.shuffle(videos);
        return videos.stream().limit(RECS_SIZE).toList();
    }


    private List<VideoEntity> getByCategoriesAndLanguages(Long userId, Set<Long> exceptions, int size){
        return likeRepository.findRecommendationsTest(userId, Instant.now().minus(AppConstants.POPULARITY_DAYS, ChronoUnit.DAYS), exceptions, Pageable.ofSize(size));
    }


    //array languages should be ordered by priority
    private List<VideoEntity> getByLanguages(Set<Long> exceptions, String[] languages, int size){
        List<VideoEntity> result = new ArrayList<>(size);
        for (String language:languages) {
              result.addAll(getSome(
                      language,
                      Stream.concat(exceptions.stream(), result.stream().map(VideoEntity::getId)).collect(Collectors.toSet()),
                      size - result.size()));
              if(result.size() == size) break;
        }
        return result;
    }

    private List<VideoEntity> getSome(String language, Set<Long> exceptions, int size){
        return likeRepository.findRecommendations(
                Instant.now().minus(AppConstants.POPULARITY_DAYS, ChronoUnit.DAYS),
                language,
                exceptions,
                Pageable.ofSize(size));
    }

    private List<VideoEntity> getSomePopularVideos(Set<Long> exceptions, int size){
        return likeRepository.findFastestGrowingVideosByLikes(
                Instant.now().minus(AppConstants.POPULARITY_DAYS, ChronoUnit.DAYS),
                exceptions,
                Pageable.ofSize(size)
        );

    }
}
