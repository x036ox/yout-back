package com.artur.youtback.repository;

import com.artur.youtback.entity.VideoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends ListCrudRepository<VideoEntity, Long>{

    @Modifying
    @Query("UPDATE VideoEntity SET views = views + 1 WHERE id = :videoId")
    void incrementViewsById(@Param("videoId") Long videoId);

    @Query("SELECT video FROM VideoEntity video GROUP BY video.id ORDER BY video.duration DESC")
    List<VideoEntity> findMostDuration(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.views >= :value")
    List<VideoEntity> findByViewsMoreThen(@Param("value")String value, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.views <= :value")
    List<VideoEntity> findByViewsLessThen(@Param("value")String value, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN usersLiked GROUP BY video.id HAVING COUNT(*) <= :value")
    List<VideoEntity> findByLikesLessThen(@Param("value")String value, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN usersLiked GROUP BY video.id HAVING COUNT(*) >= :value")
    List<VideoEntity> findByLikesMoreThen(@Param("value")String value, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN usersLiked GROUP BY video.id ORDER BY COUNT(video.id) DESC")
    List<VideoEntity> findMostLikes(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video GROUP BY video.id ORDER BY video.views DESC")
    List<VideoEntity> findMostViews(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.title LIKE %:title%")
    List<VideoEntity> findByTitle(@Param("title")String title, Pageable pageable);

}
