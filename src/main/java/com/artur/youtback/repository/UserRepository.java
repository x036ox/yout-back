package com.artur.youtback.repository;

import com.artur.youtback.entity.UserEntity;
import com.artur.youtback.utils.AppAuthorities;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends ListCrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByUsername(String username, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN user.subscribers s GROUP BY user.id HAVING COUNT(*) <= :value")
    List<UserEntity> findBySubscribersLessThen(@Param("value") String value, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN user.subscribers s GROUP BY user.id HAVING COUNT(*) >= :value")
    List<UserEntity> findBySubscribersMoreThen(@Param("value") String value, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN userVideos GROUP BY user.id HAVING COUNT(*) >= :value")
    List<UserEntity> findByVideosMoreThen(@Param("value") String value, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN userVideos GROUP BY user.id HAVING COUNT(*) <= :value")
    List<UserEntity> findByVideosLessThen(@Param("value") String value, Pageable pageable);

    @Query("SELECT user FROM UserEntity user INNER JOIN subscribers s GROUP BY user.id ORDER BY COUNT(s.id) DESC")
    List<UserEntity> findMostSubscribes(Pageable pageable);

    @Query("SELECT user FROM UserEntity user WHERE user.authorities= :authority")
    List<UserEntity> findByAuthority(@Param("authority")String authority, Pageable pageable);
}
