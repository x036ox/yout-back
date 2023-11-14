package com.artur.youtback.entity;

import com.artur.youtback.model.Video;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.security.core.userdetails.UserDetails;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String picture;

   @OneToMany(cascade = CascadeType.ALL,mappedBy = "user")
   private List<VideoEntity> userVideos = new ArrayList<>();
   @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "historyOwner")
   private  List<SearchHistory> searchHistory = new ArrayList<>();
   @ManyToMany
   @JoinTable(name = "video_like",
           joinColumns = @JoinColumn(name = "user_id"),
           inverseJoinColumns = @JoinColumn(name = "video_id")
   )
   private Set<VideoEntity> likedVideos = new HashSet<>();
   @ManyToMany
   @JoinTable(
           name = "user_subscribes",
           joinColumns = @JoinColumn(name = "subscribes_id"),
           inverseJoinColumns = @JoinColumn(name = "subscribers_id")
   )
   private Set<UserEntity> subscribes = new HashSet<>();
   @ManyToMany(mappedBy = "subscribes")
   private  Set<UserEntity> subscribers = new HashSet<>();

    public UserEntity(Long id, String email, String username, String password, String picture) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.picture = picture;
    }

    public UserEntity() {
    }

    public Set<VideoEntity> getLikedVideos() {
        return likedVideos;
    }

    public void setLikedVideos(Set<VideoEntity> likedVideos) {
        this.likedVideos = likedVideos;
    }

    public List<SearchHistory> getSearchHistory() {
        return searchHistory;
    }

    public void setSearchHistory(List<SearchHistory> searchHistory) {
        this.searchHistory = searchHistory;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<VideoEntity> getUserVideos() {
        return userVideos;
    }

    public void setUserVideos(List<VideoEntity> userVideos) {
        this.userVideos = userVideos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public Set<UserEntity> getSubscribes() {
        return subscribes;
    }

    public void setSubscribes(Set<UserEntity> subscribes) {
        this.subscribes = subscribes;
    }

    public Set<UserEntity> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<UserEntity> subscribers) {
        this.subscribers = subscribers;
    }

}
