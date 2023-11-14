package com.artur.youtback.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class VideoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @NotBlank
    private String title;
    @NotNull
    private Integer duration;
    @NotBlank
    private String thumbnail;
    @NotNull
    private Integer views;
    private LocalDateTime uploadDate;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @ManyToMany(mappedBy = "likedVideos")
    private Set<UserEntity> usersLiked = new HashSet<>();

    public VideoEntity(Long id, String title, Integer duration, String thumbnail, Integer views, LocalDateTime uploadDate, String description, UserEntity user) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.thumbnail = thumbnail;
        this.views = views;
        this.uploadDate = uploadDate;
        this.description = description;
        this.user = user;
    }

    public VideoEntity() {
    }


    public Set<UserEntity> getUsersLiked() {
        return usersLiked;
    }

    public void setUsersLiked(Set<UserEntity> usersLiked) {
        this.usersLiked = usersLiked;
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
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
}
