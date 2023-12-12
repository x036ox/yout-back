package com.artur.youtback.service;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.WatchHistory;
import com.artur.youtback.exception.*;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.repository.LikeRepository;
import com.artur.youtback.repository.SearchHistoryRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
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

    public void deleteById(Long id) throws UserNotFoundException, IOException {
        if(!userRepository.existsById(id)) throw new UserNotFoundException("User not Found");
        UserEntity userEntity = userRepository.getReferenceById(id);
        Files.deleteIfExists(Path.of(AppConstants.IMAGE_PATH + userEntity.getPicture()));
        userRepository.deleteById(id);
    }

    public void update(UserUpdateRequest user, PasswordEncoder passwordEncoder) throws UserNotFoundException, IOException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(user.userId());
        if(optionalUserEntity.isEmpty()) throw new UserNotFoundException("User not Found");

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
            Path picturePath = Path.of(AppConstants.IMAGE_PATH + userEntity.getPicture());
            Files.deleteIfExists(picturePath);
            ImageUtils.compressAndSave(user.picture().getBytes(), picturePath.toFile());
        }
        userRepository.save(userEntity);
    }

    public User findByEmail(String email) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        return User.toModel(userEntity);
    }

    public void confirmEmail(String email) throws UserNotFoundException{
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
        userEntity.setEmailConfirmed(true);
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
        return User.toModel(userRepository.save(User.toEntity(user)));
    }

    public User registerUser(User user, File picture) throws Exception {
        if(userRepository.findByEmail(user.getEmail()).isPresent()) throw new ExistedUserException("User with this email already existed");

        user.setPicture(saveImage(picture));
        return User.toModel(userRepository.save(User.toEntity(user)));
    }


    public String saveImage(MultipartFile image) throws Exception{
        return saveImage(image.getBytes());
    }

    public String saveImage(File image) throws Exception{
        return saveImage(Files.readAllBytes(image.toPath()));
    }

    private String saveImage(byte[] bytes){
        String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
        ImageUtils.compressAndSave(bytes, new File(AppConstants.IMAGE_PATH + filename));
        return filename;
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
            try{
                likeRepository.deleteById(2L);
                likeRepository.delete(like);
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        });
    }

    public List<Video> getWatchHistory(Long userId) throws UserNotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User not found, id: " + userId));
        List<Video> result = new ArrayList<>();
        for (WatchHistory watchHistory :userEntity.getWatchHistory()) {
            videoRepository.findById(watchHistory.getVideoId()).ifPresent(v -> result.add(Video.toModel(v)));
        }
        return result;
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

    public String addUsers(int amount, PasswordEncoder passwordEncoder) throws Exception {
        String[] names = "Liam Noah Oliver James Elijah William Henry Lucas Benjamin Theodore Mateo Levi Sebastian Daniel Jack Michael Alexander Owen Asher Samuel Ethan Leo Jackson Mason Ezra John Hudson Luca Aiden Joseph David Jacob Logan Luke Julian Gabriel Grayson Wyatt Matthew Maverick Dylan Isaac Elias Anthony Thomas Jayden Carter Santiago Ezekiel Charles Josiah Caleb Cooper Lincoln Miles Christopher Nathan Isaiah Kai Joshua Andrew Angel Adrian Cameron Nolan Waylon Jaxon Roman Eli Wesley Aaron Ian Christian Ryan Leonardo Brooks Axel Walker Jonathan Easton Everett Weston Bennett Robert Jameson Landon Silas Jose Beau Micah Colton Jordan Jeremiah Parker Greyson Rowan Adam Nicholas Theo Xavier".split(" ");
        File[] profilePics = new File("user pictures to create").listFiles();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(12);
        long start = System.currentTimeMillis();
        for(int i = 0; i< amount; i++){
            Runnable task = () -> {
                int index = (int)Math.floor(Math.random() * names.length);
                String name = names[index];
                String email = (System.currentTimeMillis() + index) + name + "@gmail.com";
                File profilePic = profilePics[(int)Math.floor(Math.random() * profilePics.length)];
                try {
                    registerUser(User.create(email, name, passwordEncoder.encode("password"), AppAuthorities.USER), profilePic);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            };
            executorService.execute(task);
        }
        executorService.shutdown();
        executorService.awaitTermination(200, TimeUnit.HOURS);
        return "Completed in " + ((float)(System.currentTimeMillis() - start) / 1000) + "s";
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
