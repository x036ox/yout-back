package com.artur.youtback;


import com.artur.youtback.utils.ImageUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MethodsTest {

@Test
    public void imageTest(){
    try {
        ImageUtils.compressAndSave(Files.readAllBytes(Path.of("image/first file.png")), new File(System.currentTimeMillis() + ".jpg"));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
}
