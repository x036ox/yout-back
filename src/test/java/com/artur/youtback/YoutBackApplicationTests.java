package com.artur.youtback;

import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.repository.*;
import com.artur.youtback.service.EmailService;
import com.artur.youtback.service.RecommendationService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.AppAuthorities;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = YoutBackApplication.class)
@ActiveProfiles("dev")
class YoutBackApplicationTests {
	private static final Logger logger = LoggerFactory.getLogger(YoutBackApplicationTests.class);

	@Autowired
	EmailService emailService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	VideoRepository videoRepository;
	@Autowired
	VideoService videoService;
	@Autowired
    UserService userService;
	@Autowired
	LikeRepository likeRepository;
	@Autowired
	VideoMetadataRepository videoMetadataRepository;
	@Autowired
	UserMetadataRepository userMetadataRepository;
	@Autowired
	RecommendationService recommendationService;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	@Rollback
	public void testEmail(){
		emailService.sendEmail();
	}
	@Test
	public void userRepoTests(){
		userRepository.findMostSubscribes(Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
		userRepository.findByAuthority(AppAuthorities.ADMIN.name(), Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
	}

	@Test
	public void videoRepoTests(){
		videoRepository.findMostDuration(Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findMostLikes(Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findMostViews(Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findByTitle("r",Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
	}

	@Test
	public void likeTest(){

	}
@Transactional
	@Test
	public void recommendationsTest(){
	try {
		recommendationService.getRecommendationsFor(20L, "ru").forEach(videoEntity -> System.out.println(videoEntity.getId()));
	} catch (UserNotFoundException e) {
		e.printStackTrace();
	}
}


	@Test
	public void videoMetadataTest(){
		Iterator<Map.Entry<String, Integer>> languages = userMetadataRepository.findById(20L).get().getLanguagesDec().entrySet().iterator();
		while (languages.hasNext()){
			System.out.println(languages.next());
		}
	}

    @Test
	@Transactional
	@Rollback
    public void shouldDecreaseCategoryPoints(){
        var optionalVideoEntity = videoRepository.findById(131L);
        var optionalUserEntity = userRepository.findById(20L);
        assertTrue(optionalUserEntity.isPresent() && optionalVideoEntity.isPresent());
        UserEntity userEntity = optionalUserEntity.get();
        VideoEntity videoEntity = optionalVideoEntity.get();
		int oldValue = userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory());
        try {
            userService.notInterested(videoEntity.getId(), userEntity.getId());
        } catch (VideoNotFoundException | UserNotFoundException e) {
            e.printStackTrace();
        }

        String category = optionalVideoEntity.get().getVideoMetadata().getCategory();
        Map<String, Integer> userCategories = userEntity.getUserMetadata().getCategories();
        assertEquals((int)(oldValue * 0.25f), userRepository.findById(userEntity.getId()).get().getUserMetadata().getCategories().get(category));
		logger.trace("old value: " + oldValue + " new value: " + (int)(oldValue * 0.25));
    }
}
