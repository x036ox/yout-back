package com.artur.youtback.converter;

import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.service.minio.MinioService;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.ImageUtils;
import com.artur.youtback.utils.comparators.SearchHistoryComparator;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserConverter {
    private static final Logger logger = LoggerFactory.getLogger(UserConverter.class);

    @Autowired
    MinioService minioService;
    @Autowired
    VideoConverter videoConverter;

    public User convertToModel(UserEntity userEntity){
        Set<UserEntity> subscribers = userEntity.getSubscribers();
        /*sorting search history by date added (from present to past)*/
        List<String> searchOptionList = userEntity.getSearchHistory().stream()
                .sorted(new SearchHistoryComparator()).map(SearchHistory::getSearchOption).toList();
        String encodedPicture = null;
        try {
            encodedPicture = ImageUtils.encodeImageBase64(minioService.getObject(userEntity.picturePath()));
        } catch (Exception e) {
            logger.error("Cant get user picture (path: "
                    + userEntity.picturePath() +
                    ") from " + minioService.getClass() + "!! User has empty thumbnail displayed");
        }
        return new User(
                userEntity.getId(),
                userEntity.getEmail(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                encodedPicture,
                Integer.toString(subscribers.size()).concat(subscribers.size() == 1 ? " subscriber" : " subscribers"),
                userEntity.getUserVideos().stream().map(videoConverter::convertToModel).collect(Collectors.toList()),
                searchOptionList,
                userEntity.getAuthorities()
        );
    }

    public UserEntity convertToEntity(String email, String username, String password){
        return new UserEntity(
                null,
                email,
                username,
                password,
                AppAuthorities.USER.toString()
        );
    }
}
