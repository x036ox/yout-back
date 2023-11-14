package com.artur.youtback;

import com.artur.youtback.config.RsaKeyProperties;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.VideoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.util.Arrays;

@SpringBootApplication
@EnableTransactionManagement
@EnableConfigurationProperties(RsaKeyProperties.class)
public class YoutBackApplication {

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(YoutBackApplication.class, args);
//		Arrays.stream(context.getBeanDefinitionNames()).forEach(System.out::println);
	}

	//@Bean


}
