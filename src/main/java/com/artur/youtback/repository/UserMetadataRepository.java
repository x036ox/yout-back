package com.artur.youtback.repository;

import com.artur.youtback.entity.user.UserMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMetadataRepository extends JpaRepository<UserMetadata, Long> {
}
