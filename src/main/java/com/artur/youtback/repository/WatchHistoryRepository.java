package com.artur.youtback.repository;

import com.artur.youtback.entity.user.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    void deleteAllByVideoId(Long videoId);
}
