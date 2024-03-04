package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.listener.ProcessingEventHandler;
import com.artur.youtback.mediator.ProcessingEventMediator;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.minio.ObjectStorageService;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest extends YoutBackApplicationTests {

    @Autowired
    UserService userService;
    @MockBean
    ObjectStorageService objectStorageService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PasswordEncoder passwordEncoder;
    @MockBean
    ProcessingEventMediator processingEventMediator;
    @MockBean
    ProcessingEventHandler processingEventHandler;

    @Test
    @Transactional
    void createUpdateDeleteTest() throws Exception {
        when(processingEventMediator.userPictureProcessingWait(anyString())).thenReturn(true);
        MockMultipartFile picture = new MockMultipartFile("user-picture", Files.newInputStream(Path.of(TEST_IMAGE_FILE)));
        User user = assertDoesNotThrow(() -> userService.registerUser(new UserCreateRequest(
                "example@gmail.com",
                "test-user",
                "password",
                picture
        )));
        long id = user.getId();
        assertTrue(userRepository.existsById(id));
        verify(objectStorageService).putObject(any(InputStream.class), eq(AppConstants.USER_PATH + id + AppConstants.PROFILE_PIC_FILENAME_EXTENSION));
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.USER_PICTURE_INPUT_TOPIC), anyString(), anyString());
        verify(processingEventMediator, times(1)).userPictureProcessingWait(eq(user.getId().toString()));

        clearInvocations(objectStorageService);
        clearInvocations(processingServiceTemplate);
        clearInvocations(processingEventMediator);
        userService.update(new UserUpdateRequest(
                id,
                "newemail@gmail.com",
                "new password",
                "new test-user",
                picture
        ));
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Cannot find user"));
        verify(objectStorageService).putObject(any(), eq(AppConstants.USER_PATH + id + AppConstants.PROFILE_PIC_FILENAME_EXTENSION));
        verify(processingServiceTemplate, times(1)).send(eq(AppConstants.USER_PICTURE_INPUT_TOPIC),anyString(), anyString());
        verify(processingEventMediator, times(1)).userPictureProcessingWait(eq(user.getId().toString()));
        assertEquals("newemail@gmail.com", userEntity.getEmail());
        assertTrue(passwordEncoder.matches("new password", userEntity.getPassword()));
        assertEquals("new test-user", userEntity.getUsername());

        userService.deleteById(id);
        assertTrue(userRepository.findById(id).isEmpty());
        verify(objectStorageService).removeObject( AppConstants.USER_PATH + id + AppConstants.PROFILE_PIC_FILENAME_EXTENSION);
    }

    @Test
    public void notInterestedTest() throws NotFoundException {
        UserEntity userEntity = userRepository.findById(20L).orElseThrow(() -> new RuntimeException("User not found"));
        VideoEntity videoEntity = videoRepository.findById(22L).orElseThrow(() -> new RuntimeException("Video not found"));
        userService.notInterested(videoEntity.getId(), userEntity.getId());
        userRepository.findById(20L);
        int categoriesBefore = userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory());

        entityManager.refresh(userEntity);
        assertNotEquals(categoriesBefore, userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory()));
    }
}