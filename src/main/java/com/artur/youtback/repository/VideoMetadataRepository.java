package com.artur.youtback.repository;

import com.artur.youtback.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {
}
