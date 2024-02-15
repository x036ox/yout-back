package com.artur.youtback.config;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

@Configuration
public class AppConfig {

    @Bean
    @Qualifier("video-cyclic-barrier")
    public CyclicBarrier videoCountCyclicBarrier(){
        return new CyclicBarrier(2);
    }

    @Bean
    @Qualifier("thumbnail-cyclic-barrier")
    public CyclicBarrier thumbnailCountCyclicBarrier(){
        return new CyclicBarrier(2);
    }

    @Bean
    @Qualifier("user-picture-cyclic-barrier")
    public CyclicBarrier userPictureCyclicBarrier(){
        return new CyclicBarrier(2);
    }


    @Bean
    @Scope("prototype")
    public LanguageDetector languageDetector(){
        return new OptimaizeLangDetector().loadModels();
    }
}
