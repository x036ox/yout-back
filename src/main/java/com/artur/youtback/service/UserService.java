package com.artur.youtback.service;

import com.artur.youtback.entity.Like;
import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserMetadata;
import com.artur.youtback.entity.user.WatchHistory;
import com.artur.youtback.exception.*;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.repository.*;
import com.artur.youtback.utils.*;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import com.artur.youtback.utils.comparators.SortOptionsComparators;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

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



    public List<User> findAll() throws NotFoundException {
        List<User> userList = userRepository.findAll().stream().map(
                User::toModel
        ).toList();
        if(userList.isEmpty()) throw new NotFoundException("No users was found");
        return userList;
    }

    public User findById(Long id) throws NotFoundException {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not Found");

        return User.toModel(optionalUserEntity.get());
    }

    public List<Video> getAllUserVideos(Long userId, SortOption sortOption) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(sortOption != null){
            return userEntity.getUserVideos().stream().sorted(SortOptionsComparators.get(sortOption)).map(Video::toModel).toList();
        }

        return userEntity.getUserVideos().stream().map(Video::toModel).toList();
    }


    public List<User> findByOption(String option, String value) throws NullPointerException, IllegalArgumentException{
        return Objects.requireNonNull(Tools.findByOption(option, value, userRepository)).stream().map(User::toModel).toList();
    }

    public void notInterested(Long videoId, Long userId) throws NotFoundException, NotFoundException {
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        String category = videoEntity.getVideoMetadata().getCategory();
        if(Objects.equals(userEntity.getUserMetadata().getCategories().computeIfPresent(category, (key, value) -> (int) (value * 0.25f)), 0)){
            userEntity.getUserMetadata().getCategories().remove(category);
        }
        userMetadataRepository.save(userEntity.getUserMetadata());
    }

    @Transactional
    public void deleteById(Long id) throws NotFoundException, IOException {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not Found"));
        Path picturePath = Path.of(AppConstants.IMAGE_PATH + userEntity.getId());
        userRepository.delete(userEntity);
        Files.deleteIfExists(picturePath);
    }

    public void update(UserUpdateRequest user, PasswordEncoder passwordEncoder) throws NotFoundException, IOException {
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
            Path picturePath = Path.of(AppConstants.IMAGE_PATH + userEntity.getId());
            Files.deleteIfExists(picturePath);
            ImageUtils.compressAndSave(user.picture().getBytes(), picturePath.toFile());
        }
        userRepository.save(userEntity);
    }

    public User findByEmail(String email) throws NotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        return User.toModel(userEntity);
    }

    public void confirmEmail(String email) throws NotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        userEntity.setEmailConfirmed(true);
        userRepository.save(userEntity);
    }

    public User loginUser(String email, String password) throws NotFoundException, IncorrectPasswordException {
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(email);
        if(optionalUserEntity.isEmpty()) throw new NotFoundException("User not found");

        UserEntity userEntity = optionalUserEntity.get();
        if(userEntity.getPassword().equals(password)){
            return User.toModel(userEntity);
        }
        else throw new IncorrectPasswordException("Incorrect password");
    }

    public User registerUser(UserCreateRequest user) throws Exception {
        return registerUser(User.toEntity(user), user.picture().getBytes());

    }

    public User registerUser(User user, File picture) throws Exception {
        return registerUser(User.toEntity(user), Files.readAllBytes(picture.toPath()));
    }

    @Transactional
    private User registerUser(UserEntity userEntity, byte[] picture) throws AlreadyExistException {
        if(userRepository.findByEmail(userEntity.getEmail()).isPresent()) throw new AlreadyExistException("User with this email already existed");
        saveImage(picture);
        return User.toModel(userRepository.save(userEntity));
    }

    public String saveImage(byte[] bytes){
        String filename = System.currentTimeMillis() + "." + ImageUtils.IMAGE_FORMAT;
        ImageUtils.compressAndSave(bytes, new File(AppConstants.IMAGE_PATH + filename));
        return filename;
    }

    public void addSearchOption(Long id, String searchOption) throws NotFoundException, AlreadyExistException {
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
                throw new AlreadyExistException("The same search option exists");
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


    public void likeVideo(Long userId, Long videoId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        VideoEntity videoEntity = videoRepository.findById(videoId).orElseThrow(() -> new NotFoundException("Video not found"));
        Set<Like> likedVideos = userEntity.getLikes();
        //we need delete existed like if we have or add a new if we don`t have
        //we cant use contains() because we don't have like's id
        Optional<Like> optionalLike = likedVideos.stream().filter(like -> like.getVideoEntity().getId().equals(videoId)).findFirst();
        //if not found, we have to add like, otherwise delete this like
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

    public void dislikeVideo(Long userId, Long videoId) throws NotFoundException {
        if(!userRepository.existsById(userId)) throw new NotFoundException("User not found");
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

    public List<Video> getWatchHistory(Long userId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(()-> new NotFoundException("User not found, id: " + userId));
        List<Video> result = new ArrayList<>();
        for (WatchHistory watchHistory :userEntity.getWatchHistory()) {
            videoRepository.findById(watchHistory.getVideoId()).ifPresent(v -> result.add(Video.toModel(v)));
        }
        return result;
    }
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

    public boolean hasUserLikedVideo(Long userId, Long videoId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return userEntity.getLikes().stream().anyMatch(like -> like.getVideoEntity().getId().equals(videoId));
    }

    public void subscribeById(Long userId, Long subscribedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserEntity subscribedChannel = userRepository.findById(subscribedChannelId).orElseThrow(() ->  new NotFoundException("Subscribed channel not found"));

        userEntity.getSubscribes().add(subscribedChannel);

        userRepository.save(userEntity);
    }

    public void unsubscribeById(Long userId, Long subscribedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        userEntity.setSubscribes(userEntity.getSubscribes().stream().filter((subbedChannel) ->
                !subbedChannel.getId().equals(subscribedChannelId)
                ).collect(Collectors.toSet()));

        userRepository.save(userEntity);
    }

    public boolean hasUserSubscribedChannel(Long userId, Long subbedChannelId) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        return userEntity.getSubscribes().stream().anyMatch((subbedChannel) -> subbedChannel.getId().equals(subbedChannelId));
    }

    public int addUsers(int amount, PasswordEncoder passwordEncoder) throws Exception {
        AtomicInteger createdUsers = new AtomicInteger(amount);

        String[] names = "Liam Noah Oliver James Elijah William Henry Lucas Benjamin Theodore Mateo Levi Sebastian Daniel Jack Michael Alexander Owen Asher Samuel Ethan Leo Jackson Mason Ezra John Hudson Luca Aiden Joseph David Jacob Logan Luke Julian Gabriel Grayson Wyatt Matthew Maverick Dylan Isaac Elias Anthony Thomas Jayden Carter Santiago Ezekiel Charles Josiah Caleb Cooper Lincoln Miles Christopher Nathan Isaiah Kai Joshua Andrew Angel Adrian Cameron Nolan Waylon Jaxon Roman Eli Wesley Aaron Ian Christian Ryan Leonardo Brooks Axel Walker Jonathan Easton Everett Weston Bennett Robert Jameson Landon Silas Jose Beau Micah Colton Jordan Jeremiah Parker Greyson Rowan Adam Nicholas Theo Xavier".split(" ");
        File[] profilePics = new File("user-pictures-to-create").listFiles();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(12);
        Runnable task = () -> {
            int index = (int)Math.floor(Math.random() * names.length);
            String name = names[index];
            String email = (System.currentTimeMillis() + index) + name + "@gmail.com";
            File profilePic = profilePics[(int)Math.floor(Math.random() * profilePics.length)];
            try {
                registerUser(User.create(email, name, passwordEncoder.encode("password"), AppAuthorities.USER), profilePic);
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
