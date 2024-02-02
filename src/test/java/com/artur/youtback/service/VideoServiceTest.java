package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.config.MinioConfig;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.minio.MinioService;
import com.artur.youtback.utils.AppAuthorities;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class VideoServiceTest extends YoutBackApplicationTests {

    @MockBean
    MinioConfig minioConfig;
    @MockBean
    MinioService minioService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    VideoRepository videoRepository;
    @Autowired
    VideoService videoService;

    @Test
    void convertAndUpload() throws Exception {
        File file = new File("videos-to-create\\music\\Today.mp4");
        videoService.convertAndUpload(Files.readAllBytes(file.toPath()), 0L);

        verify(minioService, atLeastOnce()).uploadObject(any(), any());
        clearInvocations();
    }

    @Test
    void createUpdateDeleteTest() throws Exception {
        File videoFile = new File(TEST_VIDEO_FILE);
        File imageFile = new File(TEST_IMAGE_FILE);
        VideoEntity videoEntity = assertDoesNotThrow(() -> videoService.create(
                "Test video" ,
                "Description",
                "Music",
                imageFile,
                videoFile,
                20L).orElseThrow(() -> new RuntimeException("User did not created")));
        long id = videoEntity.getId();
        verify(minioService, times(1)).putObject(any(byte[].class), anyString());       //uploaded picture
        verify(minioService, atLeastOnce()).uploadObject(any(File.class), anyString());         //uploaded videos

        clearInvocations(minioService);
        MockMultipartFile newVideo = new MockMultipartFile("New video", Files.readAllBytes(videoFile.toPath()));
        MockMultipartFile newThumbnail = new MockMultipartFile("New thumbnail", Files.readAllBytes(imageFile.toPath()));
        videoService.update(new VideoUpdateRequest(
                id,
                "Updated",
                "Desctiption updated",
                null,
                newVideo,
                newThumbnail
        ));
        verify(minioService, times(1)).putObject(any(byte[].class), anyString());       //uploaded picture
        verify(minioService, atLeastOnce()).uploadObject(any(File.class), anyString());         //uploaded videos
        assertTrue(videoRepository.existsById(id));
        assertNotEquals("Test video", videoEntity.getTitle());
        assertNotEquals("Description", videoEntity.getDescription());

        clearInvocations(minioService);
        videoService.deleteById(videoEntity.getId());
        assertTrue(videoRepository.findById(id).isEmpty());
        verify(minioService, times(1)).removeFolder(AppConstants.VIDEO_PATH + videoEntity.getId());
    }

    @Test
    public void watchByIdTest(@Autowired EntityManager entityManager) throws NotFoundException {
        long testVideoId = 139L;
        UserEntity userEntity = userRepository.findByAuthority(AppAuthorities.ADMIN.name(), Pageable.ofSize(1)).getFirst();
        VideoEntity videoEntity = videoRepository.findById(testVideoId).orElseThrow(() -> new RuntimeException("Video not found"));
        String videoCategory = videoEntity.getVideoMetadata().getCategory();
        String videoLanguage = videoEntity.getVideoMetadata().getLanguage();
        int categoryBefore = userEntity.getUserMetadata().getCategories().get(videoCategory) != null ?
                userEntity.getUserMetadata().getCategories().get(videoCategory) : 0;
        int languageBefore = userEntity.getUserMetadata().getLanguages().get(videoLanguage);
        int viewsBefore = videoEntity.getViews();

        entityManager.refresh(videoEntity);
        entityManager.refresh(userEntity);
        videoService.watchById(testVideoId, userEntity.getId());

        assertEquals(categoryBefore + 1, userEntity.getUserMetadata().getCategories().get(videoCategory));
        assertEquals(languageBefore + 1, userEntity.getUserMetadata().getLanguages().get(videoLanguage));
        assertTrue(userEntity.getWatchHistory().stream().anyMatch(el -> el.getVideoId() == testVideoId && el.getDate().isAfter(LocalDateTime.now().minusDays(1))));
        assertEquals(viewsBefore + 1, videoEntity.getViews());
    }


}