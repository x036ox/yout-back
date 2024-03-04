package com.artur.youtback.model.user;

import com.artur.youtback.model.video.Video;
import com.artur.youtback.utils.AppAuthorities;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class User implements UserDetails, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(User.class);
    public static String DEFAULT_USER_PICTURE = "Prewievs/Default.png";

    private Long id;
    private String email;
    private String username;
    private String password;
    private String picture;
    private String subscribers;


    private List<Video> userVideos;
    private List<String> searchHistory;

    private final transient boolean accountNonExpired = true;
    private final transient boolean accountNonLocked = true;
    private final transient boolean credentialsNonExpired = true;
    private final transient boolean enabled = true;

    private String authorities;


    @JsonCreator
    public User(@JsonProperty("username")String username, @JsonProperty("password")String password, @JsonProperty("id")Long id, @JsonProperty("email")String email, @JsonProperty("picture")String picture, @JsonProperty("subscribers")String subscribers, @JsonProperty("userVideos")List<Video> userVideos, @JsonProperty("searchHistory")List<String> searchHistory, @JsonProperty("authorities") String authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.picture = picture;
        this.subscribers = subscribers;
        this.userVideos = userVideos;
        this.searchHistory = searchHistory;
        this.authorities = authorities;
    }

    public User(Long id, String email, String username, String password, String picture, String subscribers, List<Video> userVideos, List<String> searchHistory, String authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.picture = picture;
        this.subscribers = subscribers;
        this.userVideos = userVideos;
        this.searchHistory = searchHistory;
        this.authorities = authorities;
    }


    public static User deserialize(String serialized){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(serialized, User.class);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static User create(String email, String username, String password, String authorities){
        return new User(null, email,username, password, null, null, new ArrayList<>(), new ArrayList<>(), authorities);
    }
    public static User create(String email, String username, String password, AppAuthorities authorities){
       return create(email, username, password, authorities.name());
    }

    public String serialize(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(String subscribers) {
        this.subscribers = subscribers;
    }

    public List<Video> getUserVideos() {
        return userVideos;
    }

    public void setUserVideos(List<Video> userVideos) {
        this.userVideos = userVideos;
    }

    public List<String> getSearchHistory() {
        return searchHistory;
    }

    public void setSearchHistory(List<String> searchHistory) {
        this.searchHistory = searchHistory;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return enabled;
    }


    @Override
    @JsonIgnore
    public Collection<GrantedAuthority> getAuthorities() {
        return AuthorityUtils.commaSeparatedStringToAuthorityList(this.authorities);
    }

    @JsonGetter(value = "authorities")
    public String getAuthoritiesAsString(){
        return this.authorities;
    }


    public void addAuthority(String authority){
        this.authorities += "," + authority;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }
}
