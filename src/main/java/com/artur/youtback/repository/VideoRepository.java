package com.artur.youtback.repository;

import com.artur.youtback.entity.VideoEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends ListCrudRepository<VideoEntity, Long>{
    @Modifying
    @Query("UPDATE VideoEntity SET views = views + 1 WHERE id = :videoId")
    void incrementViewsById(@Param("videoId") Long videoId);

}
