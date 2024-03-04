package com.artur.youtback.config;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class AppConfig {


    @Bean
    @Scope("prototype")
    public LanguageDetector languageDetector(){
        return new OptimaizeLangDetector().loadModels();
    }
}
