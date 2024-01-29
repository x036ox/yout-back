package com.artur.youtback.service.minio;

import com.artur.youtback.YoutBackApplicationTests;
import com.artur.youtback.utils.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class MinioServiceImplTest  {
    @Autowired
    MinioService minioService;

    @Test
    void removeFolder() throws Exception {
        minioService.removeFolder(AppConstants.VIDEO_PATH + "test/");
    }
}