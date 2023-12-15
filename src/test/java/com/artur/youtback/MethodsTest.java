package com.artur.youtback;


import com.artur.youtback.service.EmailService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.ImageUtils;
import com.artur.youtback.utils.MediaUtils;
import com.artur.youtback.utils.TimeOperations;
import com.sun.java.accessibility.util.Translator;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.translate.DefaultTranslator;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MethodsTest {


//@Test
//    public void imageTest(){
//        try {
//            ImageUtils.compressAndSave(Files.readAllBytes(Path.of("image/first file.png")), new File(System.currentTimeMillis() + ".jpg"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Test
//    public void shouldConvertSecondsToString(){
//        String result = TimeOperations.seccondsToString(122, "HH:mm:ss");
//        Assertions.assertEquals(result, "00:02:02");
//    }
//
//    @Test
//    public void videoTests(){
//        File video = new File("C:\\Users\\Artur\\Videos\\Optimus Gang - Новогодний сериал 2 серия_Trim_Trim (2).mp4");
//    }
//
//    @Test
//    public void languageDetectorTest(){
//        LanguageDetector languageDetector = new  OptimaizeLangDetector().loadModels();
//        System.out.println(languageDetector.detect("ez kínai"));
//    }
//
//    @Test
//    public void metadataTest(){
//        try {
//            System.out.println(Float.parseFloat(MediaUtils.getDuration(new File(AppConstants.VIDEO_PATH + "1701086211736.mp4"))));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (TikaException e) {
//            throw new RuntimeException(e);
//        } catch (SAXException e) {
//            throw new RuntimeException(e);
//        }
//    }
////
//    @Test
//    public void HlsTest(){
//        try{
//            MediaUtils.convertVideoToHls(new File(AppConstants.VIDEO_PATH + "Today.mp4"), true);
//        } catch(Exception e){
//            e.printStackTrace();
//        }
//    }
}
