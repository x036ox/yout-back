package com.artur.youtback.entity;

import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.user.WatchHistory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
public class VideoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @NotBlank
    private String title;
    @NotBlank
    private String thumbnail;
    @NotNull
    private Integer views;
    private LocalDateTime uploadDate;
    private String description;
    @NotNull
    private String videoPath;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "videoEntity")
    private Set<Like> likes = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private VideoMetadata videoMetadata;

    public VideoEntity(Long id, String title, String thumbnail, Integer views, LocalDateTime uploadDate, String description, String videoPath, UserEntity user) {
        this.id = id;
        this.title = title;
        this.thumbnail = thumbnail;
        this.views = views;
        this.uploadDate = uploadDate;
        this.description = description;
        this.videoPath = videoPath;
        this.user = user;
    }

    public VideoEntity() {
    }

    @Override
    public String toString() {
        return "id: " + id + "title: " + title + "language " + videoMetadata.getLanguage() + "likes " + likes.size();
    }


    public VideoMetadata getVideoMetadata() {
        return videoMetadata;
    }

    public void setVideoMetadata(VideoMetadata videoMetadata) {
        this.videoMetadata = videoMetadata;
    }

    public Set<Like> getLikes() {
        return likes;
    }

    public void setLikes(Set<Like> likes) {
        this.likes = likes;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnailPath) {
        this.thumbnail = thumbnailPath;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}
