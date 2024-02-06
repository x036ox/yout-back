package com.artur.youtback.repository;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.VideoEntity;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface LikeRepository extends ListCrudRepository<Like, Long> {



}
