package com.artur.youtback.entity;

import com.artur.youtback.entity.user.UserEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

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

    @PreRemove
    private void preRemove() {
        this.userEntity.getLikes().remove(this);
        this.videoEntity.getLikes().remove(this);
    }

    public static Like create(UserEntity userEntity, VideoEntity videoEntity){
        return new Like(userEntity, videoEntity, Instant.now());
    }

    public static Like create(UserEntity userEntity, VideoEntity videoEntity, Instant instant){
        return new Like(userEntity, videoEntity, instant);
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
