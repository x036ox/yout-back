package com.artur.youtback.entity.user;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.*;

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

    private boolean isEmailConfirmed = false;
    @NotBlank
    private String authorities;

   @OneToMany(cascade = CascadeType.ALL,mappedBy = "user")
   private List<VideoEntity> userVideos = new ArrayList<>();
   @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "historyOwner")
   private  List<SearchHistory> searchHistory = new ArrayList<>();
   @OneToMany(cascade = CascadeType.ALL, mappedBy = "userEntity")
   private Set<Like> likes = new HashSet<>();
   @ManyToMany
   @JoinTable(
           name = "user_subscribes",
           joinColumns = @JoinColumn(name = "subscribes_id"),
           inverseJoinColumns = @JoinColumn(name = "subscribers_id")
   )
   private Set<UserEntity> subscribes = new HashSet<>();
   @ManyToMany(mappedBy = "subscribes")
   private  Set<UserEntity> subscribers = new HashSet<>();
   @OneToMany(cascade = CascadeType.ALL, mappedBy = "userEntity")
   private List<WatchHistory> watchHistory = new ArrayList<>();

   @OneToOne(cascade = CascadeType.ALL)
   @JoinColumn(name = "id")
   private UserMetadata userMetadata;

    public UserEntity(Long id, String email, String username, String password, String authorities) {
        this();
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public UserEntity() {
    }

    @PostLoad
    public void sortWatchHistory(){
        this.watchHistory.sort(Comparator.comparing(WatchHistory::getDate).reversed());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String picturePath(){
        if(this.id == null){
            return null;
        }
        return AppConstants.USER_PATH + this.getId() + AppConstants.PROFILE_PIC_FILENAME_EXTENSION;
    }

    public List<WatchHistory> getWatchHistory() {
        return watchHistory;
    }


    public void setWatchHistory(List<WatchHistory> watchHistory) {
        this.watchHistory = watchHistory;
    }

    public UserMetadata getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(UserMetadata userMetadata) {
        this.userMetadata = userMetadata;
    }

    public Set<Like> getLikes() {
        return likes;
    }

    public void setLikes(Set<Like> likes) {
        this.likes = likes;
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

    public boolean isEmailConfirmed() {
        return isEmailConfirmed;
    }

    public void setEmailConfirmed(boolean emailConfirmed) {
        isEmailConfirmed = emailConfirmed;
    }

    public String getAuthorities() {
        return authorities;
    }

    public void setAuthorities(String authorities) {
        this.authorities = authorities;
    }
}
