package com.artur.youtback.repository;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.VideoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface LikeRepository extends ListCrudRepository<Like, Long> {

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE v.id NOT IN :exceptions AND l.timestamp >= :timestamp AND m.language = :language  GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByLikesAndLanguage(@Param("timestamp")Instant timestamp,@Param("language") String language,@Param("exceptions") Set<Long> exceptions, Pageable pageable);

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE l.timestamp >= :timestamp AND m.language = :language  GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByLikesAndLanguage(@Param("timestamp")Instant timestamp,@Param("language") String language, Pageable pageable);

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE v.id NOT IN :exceptions AND l.timestamp >= :timestamp AND m.category = :category AND m.language = :language GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByCategoryAndLanguage(@Param("timestamp")Instant timestamp,@Param("category") String category,@Param("language") String language,@Param("exceptions") Set<Long> exceptions, Pageable pageable);

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE l.timestamp >= :timestamp AND m.category = :category AND m.language = :language GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByCategoryAndLanguage(@Param("timestamp")Instant timestamp,@Param("category") String category, @Param("language") String language, Pageable pageable);

    //If exception list can be null or empty, use this
     default List<VideoEntity> findRecommendations(Instant timestamp, String language, Set<Long> exceptions, Pageable pageable){
        if(exceptions.isEmpty()){
            return this.findFastestGrowingVideosByLikesAndLanguage(timestamp, language, pageable);
        } else{
            return this.findFastestGrowingVideosByLikesAndLanguage(timestamp, language, exceptions, pageable);
        }
    }

    default List<VideoEntity> findRecommendations(Instant timestamp,String category, String language, Set<Long> exceptions, Pageable pageable){
        if(exceptions.isEmpty()){
            return this.findFastestGrowingVideosByCategoryAndLanguage(timestamp, category, language, pageable);
        } else{
            return this.findFastestGrowingVideosByCategoryAndLanguage(timestamp, category, language, exceptions, pageable);
        }
    }

}
