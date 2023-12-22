package com.artur.youtback.repository;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.VideoEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface LikeRepository extends ListCrudRepository<Like, Long> {

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE v.id NOT IN :exceptions AND l.timestamp >= :timestamp AND m.language = :language  GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByLikesAndLanguage(
            @Param("timestamp")Instant timestamp,
            @Param("language") String language,
            @Param("exceptions") Set<Long> exceptions,
            Pageable pageable);

    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE v.id NOT IN :exceptions AND l.timestamp >= :timestamp GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByLikes(
            @Param("timestamp")Instant timestamp,
            @Param("exceptions") @NotEmpty Set<Long> exceptions,
            Pageable pageable);
    @Query("SELECT v FROM VideoEntity v JOIN likes l JOIN videoMetadata m WHERE l.timestamp >= :timestamp AND m.language = :language  GROUP BY v.id ORDER BY COUNT(v.id) DESC")
    List<VideoEntity> findFastestGrowingVideosByLikesAndLanguage(
            @Param("timestamp")Instant timestamp,
            @Param("language") String language,
            Pageable pageable);



    @Query("SELECT v FROM VideoEntity v " +
            "JOIN videoMetadata vm " +
            "JOIN likes l " +
            "LEFT OUTER JOIN UserMetadata um ON um.id = :userId " +
            "WHERE v.id NOT IN :exceptions " +
            "AND l.timestamp > :timestamp " +
            "AND vm.category = KEY(um.categories) AND vm.language = KEY(um.languages) " +
            "GROUP BY v.id " +
            "ORDER BY VALUE(um.languages) DESC, VALUE(um.categories) DESC, COUNT(*) DESC"
    )
    List<VideoEntity> getFindRecommendationsTest(
            @Param("userId") Long userId,
            @Param("timestamp") Instant timestamp,
            @Param("exceptions") @NotEmpty Set<Long> exceptions,
            Pageable size);

    @Query("SELECT v FROM VideoEntity v " +
            "JOIN videoMetadata vm " +
            "JOIN likes l " +
            "LEFT OUTER JOIN UserMetadata um ON um.id = :userId " +
            "WHERE l.timestamp > :timestamp " +
            "AND vm.category = KEY(um.categories) AND vm.language = KEY(um.languages) " +
            "GROUP BY v.id " +
            "ORDER BY VALUE(um.languages) DESC, VALUE(um.categories) DESC, COUNT(*) DESC"
    )
    List<VideoEntity> getFindRecommendationsTest(
            @Param("userId") Long userId,
            @Param("timestamp") Instant timestamp,
            Pageable size);


// The same as getFindRecommendationsTest(), but uses native query and returns List of ids
    @Query(nativeQuery = true, value = "select v.id from video_entity v " +
            "join video_metadata vm on v.id = vm.id " +
            "join video_like l on l.video_id = v.id " +
            "join user_category uc on uc.metadata_id = :userId and vm.category = uc.category " +
            "join user_language ul on ul.metadata_id = :userId and vm.language = ul.language " +
            "where v.id not in :exceptions and l.timestamp > :timestamp " +
            "group by v.id " +
            "order by ul.repeats desc, uc.repeats desc, COUNT(*) DESC;"
    )
    List<Long> getFindRecommendationsTestIds(@Param("userId") Long userId,
                                                 @Param("timestamp") Instant timestamp,
                                                 @Param("exceptions") @NotEmpty Set<Long> exceptions,
                                                 Pageable size);

    @Query(nativeQuery = true, value = "select v.id from video_entity v " +
            "join video_metadata vm on v.id = vm.id " +
            "join video_like l on l.video_id = v.id " +
            "join user_category uc on uc.metadata_id = :userId and vm.category = uc.category " +
            "join user_language ul on ul.metadata_id = :userId and vm.language = ul.language " +
            "where l.timestamp > :timestamp " +
            "group by v.id " +
            "order by ul.repeats desc, uc.repeats desc, COUNT(*) DESC;"
    )
    List<Long> getFindRecommendationsTestIds(@Param("userId") Long userId,
                                             @Param("timestamp") Instant timestamp,
                                             Pageable size);
    default List<Long> findRecommendationsTestIds(Long userId, Instant timestamp, Set<Long> exceptions, Pageable size){
        if(exceptions.isEmpty()){
            return this.getFindRecommendationsTestIds(userId, timestamp, size);
        } else{
            return this.getFindRecommendationsTestIds(userId, timestamp, exceptions, size);
        }
    }

    default List<VideoEntity> findRecommendationsTest(Long userId, Instant timestamp, Set<Long> exceptions, Pageable size){
        if(exceptions.isEmpty()){
            return this.getFindRecommendationsTest(userId, timestamp, size);
        } else{
            return this.getFindRecommendationsTest(userId, timestamp, exceptions, size);
        }
    }

    default List<VideoEntity> findRecommendations(
            @NotNull Instant timestamp,
            @NotNull String language,
            @NotNull Set<Long> exceptions,
            @NotNull Pageable pageable){
        if(exceptions.isEmpty()){
            return this.findFastestGrowingVideosByLikesAndLanguage(timestamp, language, pageable);
        } else{
            return this.findFastestGrowingVideosByLikesAndLanguage(timestamp, language,exceptions, pageable);
        }
    }

}
