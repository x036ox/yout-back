package com.artur.youtback.service;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.*;
import com.artur.youtback.model.User;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.LikeRepository;
import com.artur.youtback.repository.SearchHistoryRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
    @Autowired
    LikeRepository likeRepository;



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


    public List<User> findByOption(String option, String value) throws NullPointerException, IllegalArgumentException{
        return Objects.requireNonNull(Tools.findByOption(option, value, userRepository)).stream().map(User::toModel).toList();
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

    public void confirmEmail(String email) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        userEntity.setEmailConfirmed(true);
        System.out.println("IS email confirmed " + userEntity.isEmailConfirmed());
        userRepository.save(userEntity);
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

    public User registerUser(User user, MultipartFile picture) throws Exception {
        if(userRepository.findByEmail(user.getEmail()).isPresent()) throw new ExistedUserException("User with this email already existed");

        user.setPicture(saveImage(picture));
        userRepository.save(User.toEntity(user));
        return User.toModel(userRepository.findByEmail(user.getEmail()).orElseThrow(() -> new UserNotFoundException("This user not found")));
    }


    public String saveImage(MultipartFile image) throws Exception{
        try{
            String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
            ImageUtils.compressAndSave(image.getBytes(), new File(AppConstants.IMAGE_PATH + filename));
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
        userEntity.getLikes().forEach(System.out::println);
        System.out.println(videoId);
        Set<Like> likedVideos = userEntity.getLikes();
        //we need delete existed like if we have or add a new if we don`t have
        //to avoid checking set two times, we just filtering it and comparing sizes of it
        Optional<Like> optionalLike = likedVideos.stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst();
        //if we have the same size, we have to add like, otherwise delete this like
        if(likedVideos.size() == 0 || optionalLike.isEmpty()){
            likeRepository.save(Like.create(userEntity, videoEntity));
        }
        else {
            likeRepository.deleteById(optionalLike.get().getId());
        }
    }

    public void dislikeVideo(Long userId, Long videoId) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        userEntity.getLikes().stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst().ifPresent(like -> {
            System.out.println(likeRepository.existsById(2L));
            try{
                likeRepository.deleteById(2L);
                likeRepository.delete(like);
            } catch (Exception e){
                e.printStackTrace();
            }
        });
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
        return userEntity.getLikes().stream().anyMatch(like -> like.getVideoEntity().getId().equals(videoId));
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

    private static class Tools{
        static List<UserEntity> findByOption(String option, String value, UserRepository userRepository) throws IllegalArgumentException{
            if(option.equals(FindOptions.UserOptions.ADMINS.name())){
                return userRepository.findByAuthority(AppAuthorities.ADMIN.name(), Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.UserOptions.BY_ID.name()) && value != null){
                return userRepository.findById(Long.parseLong(value)).stream().toList();
            } else if(option.equals(FindOptions.UserOptions.BY_EMAIL.name()) && value != null){
                return userRepository.findByEmail(value).stream().toList();
            } else if(option.equals(FindOptions.UserOptions.BY_SUBSCRIBERS.name()) && value != null){
                String[] fromTo = value.split("/");
                if(fromTo.length == 2){
                    return userRepository.findBySubscribers(fromTo[0],fromTo[1], Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
                } // else throwing exception below
            } else if(option.equals(FindOptions.UserOptions.BY_USERNAME.name()) && value != null){
                return userRepository.findByUsername(value, Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            } else if(option.equals(FindOptions.UserOptions.BY_VIDEO.name()) && value != null){
                String[] fromTo = value.split("/");
                if(fromTo.length == 2){
                    return userRepository.findByVideos(fromTo[0],fromTo[1], Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
                } // else throwing exception below
            }  else if(option.equals(FindOptions.UserOptions.MOST_SUBSCRIBERS.name())){
                return userRepository.findMostSubscribes(Pageable.ofSize(AppConstants.MAX_FIND_ELEMENTS));
            }
            throw new IllegalArgumentException("Illegal arguments option: [" + option + "]" + " value [" + value + "]");
        }
    }
}
