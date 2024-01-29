package com.artur.youtback.utils;

import com.artur.youtback.YoutBackApplicationTests;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    @Test
    void compressAndSave() throws IOException {
        assertNotNull(ImageUtils.compressAndSave(Files.readAllBytes(new File(YoutBackApplicationTests.TEST_IMAGE_FILE).toPath())));
    }
}