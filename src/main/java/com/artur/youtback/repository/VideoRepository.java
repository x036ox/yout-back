package com.artur.youtback.repository;

import com.artur.youtback.entity.VideoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VideoRepository extends JpaRepository<VideoEntity, Long> {

    @Modifying
    @Query("UPDATE VideoEntity SET views = views + 1 WHERE id = :videoId")
    void incrementViewsById(@Param("videoId") Long videoId);

    @Query("SELECT video FROM VideoEntity video JOIN VideoMetadata metadata GROUP BY video.id ORDER BY metadata.duration DESC")
    List<VideoEntity> findMostDuration(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.views BETWEEN :from AND :to")
    List<VideoEntity> findByViews(@Param("from") String from, @Param("to") String to, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN likes GROUP BY video.id HAVING COUNT(*) BETWEEN :from AND :to")
    List<VideoEntity> findByLikes(@Param("from") String from, @Param("to") String to, Pageable pageable);

    @Query("SELECT video FROM VideoEntity video INNER JOIN likes GROUP BY video.id ORDER BY COUNT(video.id) DESC")
    List<VideoEntity> findMostLikes(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video GROUP BY video.id ORDER BY video.views DESC")
    List<VideoEntity> findMostViews(Pageable pageable);

    @Query("SELECT video FROM VideoEntity video WHERE video.title LIKE %:title%")
    List<VideoEntity> findByTitle(@Param("title")String title, Pageable pageable);

    @Query("SELECT v.videoPath FROM VideoEntity v WHERE v.id = :id")
    String findVideoFilenameById(@Param("id") Long videoId);

}
