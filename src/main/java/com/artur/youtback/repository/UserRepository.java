package com.artur.youtback.repository;

import com.artur.youtback.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends ListCrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByUsername(String username);
}
