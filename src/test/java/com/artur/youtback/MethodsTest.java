package com.artur.youtback;


import com.artur.youtback.service.EmailService;
import com.artur.youtback.utils.ImageUtils;
import com.artur.youtback.utils.TimeOperations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
@SpringBootTest
public class MethodsTest {


@Test
    public void imageTest(){
        try {
            ImageUtils.compressAndSave(Files.readAllBytes(Path.of("image/first file.png")), new File(System.currentTimeMillis() + ".jpg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldConvertSecondsToString(){
        String result = TimeOperations.seccondsToString(122, "HH:mm:ss");
        Assertions.assertEquals(result, "00:02:02");
    }

    @Test
    public void videoTests(){
        File video = new File("C:\\Users\\Artur\\Videos\\Optimus Gang - Новогодний сериал 2 серия_Trim_Trim (2).mp4");
    }

}
