package com.artur.youtback.entity;

import com.artur.youtback.entity.user.UserEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "video_like", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"}))
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;
    @ManyToOne
    @JoinColumn(name = "video_id")
    private VideoEntity videoEntity;
    private Instant timestamp;

    private Like(UserEntity userEntity, VideoEntity videoEntity, Instant timestamp) {
        this.id = null;
        this.userEntity = userEntity;
        this.videoEntity = videoEntity;
        this.timestamp = timestamp;
    }

    public Like(){

    }

    public static Like create(UserEntity userEntity, VideoEntity videoEntity){
        return new Like(userEntity, videoEntity, Instant.now());
    }

    public static Like create(UserEntity userEntity, VideoEntity videoEntity, Instant instant){
        return new Like(userEntity, videoEntity, instant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return Objects.equals(id, like.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    public VideoEntity getVideoEntity() {
        return videoEntity;
    }

    public void setVideoEntity(VideoEntity videoEntity) {
        this.videoEntity = videoEntity;
    }


    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "user: " + this.userEntity.getId() + " video: " + videoEntity.getId() + " date: " + timestamp.getEpochSecond();
    }
}
