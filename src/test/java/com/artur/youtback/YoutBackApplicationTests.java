package com.artur.youtback;

import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.repository.*;
import com.artur.youtback.service.EmailService;
import com.artur.youtback.service.RecommendationService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.AppAuthorities;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SpringBootTest(classes = YoutBackApplication.class)
class YoutBackApplicationTests {

	@Autowired
	EmailService emailService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	VideoRepository videoRepository;
	@Autowired
	VideoService videoService;
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
		likeRepository.findFastestGrowingVideosByLikesAndLanguage(Instant.now().minus(2, ChronoUnit.DAYS), "en", Pageable.ofSize(10 )).forEach((videoEntity -> System.out.println(videoEntity.getId())));
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

	@Transactional
	@Test
	public void generationTest(){
//		List<VideoEntity> videoEntities = videoRepository.findAll();
//		List<UserEntity> userEntities = userRepository.findAll();
//		String[] languages = {"ru", "en", "uk"};
//		videoEntities.forEach(videoEntity -> {
//			if(videoEntity.getVideoMetadata() == null){
//				System.out.println("metadata is null for video ID: " + videoEntity.getId());
//				int index = (int)Math.floor(Math.random() * languages.length);
//				VideoEntity saved = videoRepository.save(videoEntity);
//				videoMetadataRepository.save(new VideoMetadata(saved, languages[index]));
//			}
//
//			userEntities.forEach(userEntity -> {
//				int sec = (int)Math.floor(Math.random() * 345600);
//				likeRepository.save(Like.create(userEntity, videoEntity, Instant.now().minusSeconds(sec)));
//			});
//		});
	}

	@Test
	public void findMostPopularLanguages(){
		userRepository.findById(20L).ifPresent(entity -> System.out.println(entity.getUserMetadata().findMostPopularLanguage()));
	}

	@Test
	public void videoMetadataTest(){
		Iterator<Map.Entry<String, Integer>> languages = userMetadataRepository.findById(20L).get().getLanguagesDec().entrySet().iterator();
		while (languages.hasNext()){
			System.out.println(languages.next());
		}
	}

}
