package com.artur.youtback;

import com.artur.youtback.entity.UserEntity;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.EmailService;
import com.artur.youtback.utils.AppAuthorities;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@SpringBootTest(classes = YoutBackApplication.class)
class YoutBackApplicationTests {

	@Autowired
	EmailService emailService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	VideoRepository videoRepository;
	@Test
	void contextLoads() {
	}

	@Test
	public void testEmail(){
		emailService.sendEmail();
	}

	@Test
	public void userRepoTests(){
		userRepository.findBySubscribersLessThen("4", Pageable.ofSize(1)).forEach(userEntity -> System.out.println(userEntity.getId()));
		userRepository.findByVideosMoreThen("2", Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
		userRepository.findMostSubscribes(Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
		userRepository.findByAuthority(AppAuthorities.ADMIN.name(), Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
	}

	@Test
	public void videoRepoTests(){
		videoRepository.findMostDuration(Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findByViewsMoreThen("10", Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findByLikesLessThen("2", Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findMostLikes(Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findMostViews(Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
		videoRepository.findByTitle("r",Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
	}

}
