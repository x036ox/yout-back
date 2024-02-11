package com.artur.youtback.utils;

import com.artur.youtback.YoutBackApplicationTests;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    @Test
    void compressAndSave() throws IOException {
        try (InputStream fileInputStream = new FileInputStream(YoutBackApplicationTests.TEST_IMAGE_FILE)){
            assertNotNull(ImageUtils.compressAndSave(fileInputStream));
        }
    }
}