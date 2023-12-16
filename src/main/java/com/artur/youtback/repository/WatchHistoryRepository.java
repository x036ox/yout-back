package com.artur.youtback.repository;

import com.artur.youtback.entity.user.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    @Modifying
    @Query("DELETE FROM WatchHistory wh WHERE wh.videoId = :videoId")
    void deleteAllByVideoId(@Param("videoId")Long videoId);
}
