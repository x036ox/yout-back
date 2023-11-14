package com.artur.youtback.service;

import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.*;
import com.artur.youtback.model.User;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.SearchHistoryRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.Path;
import com.artur.youtback.utils.SortOption;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    SearchHistoryRepository searchHistoryRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    TokenService tokenService;



    public List<User> findAll() throws UserNotFoundException{
        List<User> userList = userRepository.findAll().stream().map(
                User::toModel
        ).toList();
        if(userList.isEmpty()) throw new UserNotFoundException("No users was found");
        return userList;
    }

    public User findById(Long id) throws UserNotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not Found");

        return User.toModel(optionalUserEntity.get());
    }

    public List<Video> getAllUserVideos(Long userId, SortOption sortOption) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        if(sortOption != null){
            return userEntity.getUserVideos().stream().sorted(SortOptionsComparators.get(sortOption)).map(Video::toModel).toList();
        }

        return userEntity.getUserVideos().stream().map(Video::toModel).toList();
    }




    public void deleteById(Long id) throws UserNotFoundException{
        if(!userRepository.existsById(id)) throw new UserNotFoundException("User not Found");

        userRepository.deleteById(id);
    }

    public void update(User user, Long id) throws UserNotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("Video not Found");

        UserEntity userEntity = optionalUserEntity.get();
        userEntity.setUsername(user.getUsername());
        userEntity.setPassword(user.getPassword());
        userEntity.setPicture(user.getPicture());

        userRepository.save(userEntity);
    }

    public User findByEmail(String email) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        return User.toModel(userEntity);
    }

    public User loginUser(String email, String password) throws UserNotFoundException, IncorrectPasswordException {
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(email);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getPassword().equals(password)){
            return User.toModel(userEntity);
        }
        else throw new IncorrectPasswordException("Incorrect password");
    }

    public User registerUser(User user) throws ExistedUserException, UserNotFoundException{
        if(userRepository.findByEmail(user.getEmail()).isPresent()) throw new ExistedUserException("User with this email already existed");

        userRepository.save(User.toEntity(user));
        return User.toModel(userRepository.findByEmail(user.getEmail()).orElseThrow(() -> new UserNotFoundException("This user is not found")));
    }


    public String saveImage(MultipartFile image) throws Exception{
        try{
            String originalFilename = image.getOriginalFilename();
            String filename = System.currentTimeMillis() + originalFilename.substring(originalFilename.lastIndexOf('.'));
            java.nio.file.Path path = java.nio.file.Path.of(Path.IMAGE.toString(), filename);
            image.transferTo(path);
            return filename;
        } catch (IOException e){
            throw new Exception("Could not save file " + image.getOriginalFilename() + " uploaded from client cause: " + e.getMessage());
        }
    }

    public void addSearchOption(Long id, String searchOption) throws UserNotFoundException, ExistingSearchOptionException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getSearchHistory() == null) userEntity.setSearchHistory(new ArrayList<>());                               //if adding at the first time

        List<SearchHistory> searchHistoryList = userEntity.getSearchHistory();
        /*if we have the same value, we have to update date added in database*/
        for (SearchHistory searchHistory: searchHistoryList) {
            if(searchHistory.getSearchOption().equals(searchOption)){
                searchHistory.setDateAdded();
                searchHistoryRepository.save(searchHistory);
                throw new ExistingSearchOptionException("The same search option exists");
            }
        }
        /*if we have more than 9 search options per user, we have to delete the oldest one and then add a new one*/

        if(searchHistoryList.size() > 9){
            /*sorting list by date added and deleting the latest by his id*/
            searchHistoryRepository.deleteById(searchHistoryList.stream().sorted(new SearchHistoryComparator()).toList().get(AppConstants.MAX_SEARCH_HISTORY_OPTIONS - 1).getId());
        }
        /*adding a new search option in database*/
        searchHistoryRepository.save(new SearchHistory(null, searchOption, userEntity));
    }


    public void likeVideo(Long userId, Long videoId) throws UserNotFoundException, VideoNotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new VideoNotFoundException("Video not found"));

        Set<VideoEntity> likedVideos = userEntity.getLikedVideos();
        //we need delete existed like if we have or add a new if we don`t have
        //to avoid checking set two times, we just filtering it and comparing sizes of it
        Set<VideoEntity> filteredLikedVideos = likedVideos.stream().filter(video -> !video.getId().equals(videoId)).collect(Collectors.toSet());
        //if we have the same size, we have to add like, otherwise delete this like
        if(likedVideos.size() == 0 || filteredLikedVideos.size() == likedVideos.size()){
            likedVideos.add(videoEntity);
        }
        else {
            userEntity.setLikedVideos(filteredLikedVideos);
        }
        userRepository.save(userEntity);
    }

    public void dislikeVideo(Long userId, Long videoId) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        userEntity.setLikedVideos(
                userEntity.getLikedVideos().stream().filter(v -> !v.getId().equals(videoId)).collect(Collectors.toSet())
        );

        userRepository.save(userEntity);
    }

    public void deleteSearchOption(Long userId, String searchOption) throws SearchOptionNotFoundException, UserNotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getSearchHistory() == null) throw new SearchOptionNotFoundException("Search option not found");

        for (SearchHistory searchHistory:
             userEntity.getSearchHistory()) {
            if(searchHistory.getSearchOption().equals(searchOption)){
                searchHistoryRepository.delete(searchHistory);
                return;
            }
        }
        throw new SearchOptionNotFoundException("Search option not found");
    }

    public boolean hasUserLikedVideo(Long userId, Long videoId) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        return userEntity.getLikedVideos().stream().anyMatch(vEntity -> vEntity.getId().equals(videoId));
    }

    public void subscribeById(Long userId, Long subscribedChannelId) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        UserEntity subscribedChannel = userRepository.findById(subscribedChannelId).orElseThrow(() ->  new UserNotFoundException("Subscribed channel not found"));

        userEntity.getSubscribes().add(subscribedChannel);

        userRepository.save(userEntity);
    }

    public void unsubscribeById(Long userId, Long subscribedChannelId) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        userEntity.setSubscribes(userEntity.getSubscribes().stream().filter((subbedChannel) ->
                !subbedChannel.getId().equals(subscribedChannelId)
                ).collect(Collectors.toSet()));

        userRepository.save(userEntity);
    }

    public boolean hasUserSubscribedChannel(Long userId, Long subbedChannelId) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        return userEntity.getSubscribes().stream().anyMatch((subbedChannel) -> subbedChannel.getId().equals(subbedChannelId));
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.toModel(userEntity);
    }
}
