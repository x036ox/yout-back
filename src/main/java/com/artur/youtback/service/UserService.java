package com.artur.youtback.service;

import com.artur.youtback.converter.UserConverter;
import com.artur.youtback.converter.VideoConverter;
import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.WatchHistory;
import com.artur.youtback.exception.*;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.repository.*;
import com.artur.youtback.service.minio.MinioService;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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
    @Autowired
    UserMetadataRepository userMetadataRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    UserConverter userConverter;
    @Autowired
    VideoConverter videoConverter;
    @Autowired
    MinioService minioService;
    @Autowired
    PasswordEncoder passwordEncoder;


    public List<User> findAll() throws NotFoundException {
        List<User> userList = userRepository.findAll().stream().map(
                userConverter::convertToModel
        ).toList();
        if(userList.isEmpty()) throw new NotFoundException("No users was found");
        return userList;
    }

    public User findById(Long id) throws NotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not Found");

        return userConverter.convertToModel(optionalUserEntity.get());
    }

    /**
     * Find all user videos. Can be sorted by specified sort option, that can be null.
     * If null, result would be sorted by upload date.
     * @param userId user id
     * @param sortOption sort option, can be null.
     * @return List of founded videos.
     * @throws NotFoundException if user with specified id was not found.
     */
    public List<Video> getAllUserVideos(Long userId, @Nullable SortOption sortOption) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(sortOption != null){
            return userEntity.getUserVideos().stream().sorted(SortOptionsComparators.get(sortOption)).map(videoConverter::convertToModel).toList();
        }

        return userEntity.getUserVideos().stream().map(videoConverter::convertToModel).toList();
    }


    /**Find users by specified options in {@link Tools} class.
     * @param option option to search by. Options specified in {@link com.artur.youtback.utils.FindOptions.UserOptions}
     * @param value value for the options, can be null. Range should be indicated like "1/100" for range from 1 to 100.
     *             For example for option ADMINS does not need a value.
     * @return List of users founded by specified options
     * @throws IllegalArgumentException if range is specified incorrectly
     */
    public List<User> findByOption(String option, String value)throws IllegalArgumentException{
        return Tools.findByOption(option, value, userRepository).stream().map(userConverter::convertToModel).toList();
    }

    /** Indicates that the specified video is not interesting for user. Takes category of this video and
     * decreases user`s number of watched videos with this category by 0.25 times. If after this user has 0
     * repetitions in this category, this category will be deleted.
     * @param videoId video id
     * @param userId user id
     * @throws NotFoundException if user or video is not found
     */
    public void notInterested(Long videoId, Long userId) throws NotFoundException {
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        String category = videoEntity.getVideoMetadata().getCategory();
        if(Objects.equals(userEntity.getUserMetadata().getCategories().computeIfPresent(category, (key, value) -> (int) (value * 0.25f)), 0)){
            userEntity.getUserMetadata().getCategories().remove(category);
        }
        userMetadataRepository.save(userEntity.getUserMetadata());
    }

    /**Delete user from database and all user data from {@link MinioService}.
     * @param id user id
     * @throws Exception if user not found or {@link MinioService} can not remove user data
     */
    @Transactional
    public void deleteById(Long id) throws Exception {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));
        userRepository.delete(userEntity);
        minioService.removeObject(AppConstants.USER_PATH + userEntity.getId() + AppConstants.PROFILE_PIC_FILENAME_EXTENSION);
    }

    /**Update {@link UserEntity}. The fields that could be updated:
     * <ul>
     *     <li>Username - user`s username. Can be null
     *     <li>Password - user`s password. Can be null
     *     <li>Email - user`s email. Can be null
     *     <li>Picture - user`s profile picture. Can be null
     * </ul>
     * If anything of this is null, it wouldn't be changed.
     * @param user instance of {@link UserUpdateRequest}, that contains user data to update
     * @throws Exception if user was not found or if exceptions occurred in {@link MinioService}
     */
    public void update(UserUpdateRequest user) throws Exception {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(user.userId());
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not Found");

        UserEntity userEntity = optionalUserEntity.get();
        if(user.username() != null){
            userEntity.setUsername(user.username());
        }
        if(user.password() != null){
            userEntity.setPassword(passwordEncoder.encode(user.password()));
        }
        if(user.email() != null){
            userEntity.setEmail(user.email());
        }
        if(user.picture() != null){
            byte[] bytes = ImageUtils.compressAndSave(user.picture().getBytes());
            minioService.putObject(bytes, AppConstants.USER_PATH + userEntity.getId() + AppConstants.PROFILE_PIC_FILENAME_EXTENSION);
        }
        userRepository.save(userEntity);
    }

    public User findByEmail(String email) throws NotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        return userConverter.convertToModel(userEntity);
    }

    public void confirmEmail(String email) throws NotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        userEntity.setEmailConfirmed(true);
        userRepository.save(userEntity);
    }

    public User registerUser(UserCreateRequest user) throws Exception {
        return registerUser(userConverter.convertToEntity(user.email(), user.username(), user.password()), user.picture().getBytes());

    }

    /**Saves {@link UserEntity} to database, compresses and uploads picture to {@link MinioService}.
     * @param userEntity user entity to create.
     * @param picture user`s picture
     * @return created user, converted to DTO {@link User}.
     * @throws Exception if user with this email already exists or if {@link MinioService} can not save picture.
     */
    @Transactional
    private User registerUser(UserEntity userEntity, byte[] picture) throws Exception {
        if(userRepository.findByEmail(userEntity.getEmail()).isPresent()) throw new AlreadyExistException("User with this email already existed");

        UserEntity savedEntity = userRepository.save(userEntity);
        saveImage(picture, userEntity.getId());
        return userConverter.convertToModel(savedEntity);
    }

    /**Compress specified image and save to {@link MinioService}.
     * @param bytes image bytes
     * @param userId user`s id
     * @throws Exception - if can not compress this image or if {@link MinioService} can not upload this image.
     */
    public void saveImage(byte[] bytes, Long userId) throws Exception {
        byte[] resultBytes = ImageUtils.compressAndSave(bytes);
        minioService.putObject(resultBytes, AppConstants.USER_PATH + userId + AppConstants.PROFILE_PIC_FILENAME_EXTENSION);
    }

    /**Adds search option in search history. Removes extra options if there are more than specified
     *  in {@code MAX_SEARCH_HISTORY_OPTIONS}. If contains the same search option, need to update date of this option
     * @param id user id
     * @param searchOption search option. Anything that user searched.
     * @throws NotFoundException if user with this id was not found.
     */
    public void addSearchOption(Long id, String searchOption) throws NotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getSearchHistory() == null) userEntity.setSearchHistory(new ArrayList<>());                               //if adding at the first time

        List<SearchHistory> searchHistoryList = userEntity.getSearchHistory();
        /*if we have the same value, we have to update date added in database*/
        for (SearchHistory searchHistory: searchHistoryList) {
            if(searchHistory.getSearchOption().equals(searchOption)){
                searchHistory.setDateAdded();
                searchHistoryRepository.save(searchHistory);
            }
        }
        /*remove extra option*/

        if(searchHistoryList.size() > AppConstants.MAX_SEARCH_HISTORY_OPTIONS - 1){
            /*sorting list by date added and deleting the latest by his id*/
            searchHistoryRepository.deleteById(searchHistoryList.stream().sorted(new SearchHistoryComparator()).toList().get(AppConstants.MAX_SEARCH_HISTORY_OPTIONS - 1).getId());
        }
        searchHistoryRepository.save(new SearchHistory(null, searchOption, userEntity));
    }


    /**Likes a video. If like already exists, remove this like, otherwise add.
     * @param userId user that likes video
     * @param videoId video that liked user
     * @throws NotFoundException if user or video not found
     */
    public void likeVideo(Long userId, Long videoId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        Set<Like> likedVideos = userEntity.getLikes();
        //we cant use contains() because we don't have like's id
        Optional<Like> optionalLike = likedVideos.stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst();
        if(optionalLike.isEmpty()){
            likeRepository.save(Like.create(userEntity, videoEntity));
        }
        else {
            Like like = optionalLike.get();
            like.getUserEntity().getLikes().remove(like);
            like.getVideoEntity().getLikes().remove(like);
            likeRepository.delete(like);
        }
    }

    /**Dislike this video. If liked, removes it.
     * @param userId user id that dislike video
     * @param videoId video id that disliked by user.
     * @throws NotFoundException - if user or video not found
     */
    public void dislikeVideo(Long userId, Long videoId) throws NotFoundException {
        if(!userRepository.existsById(userId)) throw new NotFoundException("User not found");
        if(!videoRepository.existsById(videoId)) throw new NotFoundException("Video not found");
        UserEntity userEntity = userRepository.getReferenceById(userId);
        userEntity.getLikes().stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst().ifPresent(like -> {
            try{
                like.getUserEntity().getLikes().remove(like);
                like.getVideoEntity().getLikes().remove(like);
                likeRepository.delete(like);
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        });
    }

    /** Gets user search history.
     * @param userId user id
     * @return videos that user have watched.
     * @throws NotFoundException if user not found
     */
    public List<Video> getWatchHistory(Long userId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(()-> new NotFoundException("User not found, id: " + userId));
        List<Video> result = new ArrayList<>();
        for (WatchHistory watchHistory :userEntity.getWatchHistory()) {
            videoRepository.findById(watchHistory.getVideoId()).ifPresent(v -> result.add(videoConverter.convertToModel(v)));
        }
        return result;
    }

    /**Deletes specified search option
     * @param userId user id
     * @param searchOption search option to delete
     * @throws NotFoundException if user or search option not found
     */
    public void deleteSearchOption(Long userId, String searchOption) throws NotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getSearchHistory() == null) throw new NotFoundException("Search option not found");

        for (SearchHistory searchHistory:
             userEntity.getSearchHistory()) {
            if(searchHistory.getSearchOption().equals(searchOption)){
                searchHistoryRepository.delete(searchHistory);
                return;
            }
        }
        throw new NotFoundException("Search option not found");
    }

    /**Checks if user liked video.
     * @param userId user id
     * @param videoId video id
     * @return true if user liked video, otherwise false
     * @throws NotFoundException - if user or video not found
     */
    public boolean hasUserLikedVideo(Long userId, Long videoId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(!videoRepository.existsById(videoId)) throw new NotFoundException("Video not found");
        return userEntity.getLikes().stream().anyMatch(like -> like.getVideoEntity().getId().equals(videoId));
    }

    /**Add user in subscribes to another user.
     * @param userId user id that subscribe another
     * @param subscribedChannelId user id that being subscribed
     * @throws NotFoundException if one of users not found
     */
    public void subscribeById(Long userId, Long subscribedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserEntity subscribedChannel = userRepository.findById(subscribedChannelId).orElseThrow(() ->  new NotFoundException("Subscribed channel not found"));

        userEntity.getSubscribes().add(subscribedChannel);

        userRepository.save(userEntity);
    }

    /**Unsubscribe from this user
     * @param userId user id that will unsubscribe
     * @param subscribedChannelId user id that will be unsubscribed
     * @throws NotFoundException if one of users not found
     */
    public void unsubscribeById(Long userId, Long subscribedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(!userRepository.existsById(userId)) throw new NotFoundException("User not found");

        userEntity.setSubscribes(userEntity.getSubscribes().stream().filter((subbedChannel) ->
                !subbedChannel.getId().equals(subscribedChannelId)
                ).collect(Collectors.toSet()));

        userRepository.save(userEntity);
    }

    /**Checks if user subscribes to another user.
     * @param userId user id that will be checked
     * @param subbedChannelId another user id
     * @return true if user subscribed on another, otherwise false
     * @throws NotFoundException - if user not found
     */
    public boolean hasUserSubscribedChannel(Long userId, Long subbedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        return userEntity.getSubscribes().stream().anyMatch((subbedChannel) -> subbedChannel.getId().equals(subbedChannelId));
    }

    /**Creates specified amount of users. Users data will be picked randomly of
     * already specified lists of usernames, pictures, etc. Pictures stored in file system
     * in path {@code userPictureFolderPath}.
     * @param amount num of users to create
     * @return how many users was created
     * @throws Exception if {@link MinioService} can not upload picture or if IOException happened
     */
    public int addUsers(int amount) throws Exception {
        AtomicInteger createdUsers = new AtomicInteger(amount);
        String userPictureFolderPath = "user-pictures-to-create";

        String[] names = "Liam Noah Oliver James Elijah William Henry Lucas Benjamin Theodore Mateo Levi Sebastian Daniel Jack Michael Alexander Owen Asher Samuel Ethan Leo Jackson Mason Ezra John Hudson Luca Aiden Joseph David Jacob Logan Luke Julian Gabriel Grayson Wyatt Matthew Maverick Dylan Isaac Elias Anthony Thomas Jayden Carter Santiago Ezekiel Charles Josiah Caleb Cooper Lincoln Miles Christopher Nathan Isaiah Kai Joshua Andrew Angel Adrian Cameron Nolan Waylon Jaxon Roman Eli Wesley Aaron Ian Christian Ryan Leonardo Brooks Axel Walker Jonathan Easton Everett Weston Bennett Robert Jameson Landon Silas Jose Beau Micah Colton Jordan Jeremiah Parker Greyson Rowan Adam Nicholas Theo Xavier".split(" ");
        File[] profilePics = new File(userPictureFolderPath).listFiles();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(12);
        Runnable task = () -> {
            int index = (int)Math.floor(Math.random() * names.length);
            String username = names[index];
            String email = (System.currentTimeMillis() + index) + username + "@gmail.com";
            File profilePic = profilePics[(int)Math.floor(Math.random() * profilePics.length)];
            String password =  passwordEncoder.encode("password");
            try {
                registerUser(userConverter.convertToEntity(email, username, password), Files.readAllBytes(profilePic.toPath()));
            } catch (Exception e) {
                createdUsers.decrementAndGet();
                logger.error(e.getMessage());
            }
        };
        for(int i = 0; i< amount; i++){
            executorService.execute(task);
        }
        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.HOURS);
        return createdUsers.get();
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userConverter.convertToModel(userEntity);
    }

    private static class Tools{

        /**Finds user by specified option. Option is accepted as string
         *  and converted to {@link com.artur.youtback.utils.FindOptions.UserOptions}.
         * @param option option to search by. Options specified in {@link com.artur.youtback.utils.FindOptions.UserOptions}
         * @param value value for the options, can be null. Range should be indicated like "1/100" for range from 1 to 100.
         *             For example for option ADMINS does not need a value and there should be null.
         * @return List of users founded by specified options
         * @throws IllegalArgumentException if {@link com.artur.youtback.utils.FindOptions.UserOptions} has not
         * specified option ot range is specified incorrectly
         */
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
