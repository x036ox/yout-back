package com.artur.youtback.service;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.config.MinioConfig;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.minio.MinioService;
import com.artur.youtback.utils.AppConstants;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Scope;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class VideoServiceTest extends YoutBackApplicationTests {

    @MockBean
    MinioConfig minioConfig;
    @MockBean
    MinioService minioService;
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
    @Transactional
    @Rollback
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


}