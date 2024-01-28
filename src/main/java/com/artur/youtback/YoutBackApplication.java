package com.artur.youtback;

import com.artur.youtback.config.RsaKeyProperties;
import com.artur.youtback.utils.AppConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableTransactionManagement
@EnableConfigurationProperties(RsaKeyProperties.class)
public class YoutBackApplication {
	static {
		try {
			Files.createDirectory(Path.of(AppConstants.VIDEO_PATH));
		} catch (Exception ignored) {
		}
		try {
			Files.createDirectory(Path.of(AppConstants.IMAGE_PATH));
		} catch (Exception ignored) {
		}
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(YoutBackApplication.class, args);
	}
}
