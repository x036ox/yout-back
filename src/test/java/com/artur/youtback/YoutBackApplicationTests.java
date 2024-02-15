package com.artur.youtback;

import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.user.UserMetadata;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.repository.UserMetadataRepository;
import com.artur.youtback.repository.UserRepository;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.RecommendationService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.minio.MinioService;
import com.artur.youtback.utils.AppAuthorities;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@Rollback
@SpringBootTest(classes = YoutBackApplication.class)
@ActiveProfiles("dev")
public class YoutBackApplicationTests {
	public static final String TEST_VIDEO_FILE = "src/test/files/Video.mp4";
	public static final String TEST_IMAGE_FILE = "src/test/files/Image.jpg";

    @MockBean
    MinioClient minioClient;
	@MockBean
	protected KafkaTemplate<String, String> processingServiceTemplate;
	@MockBean
	ConcurrentKafkaListenerContainerFactory<String, Boolean> kafkaListenerContainerFactory;

	@Autowired
    UserMetadataRepository userMetadataRepository;
	@Autowired
    RecommendationService recommendationService;

	@Test
	public void recommendationsTest() throws NotFoundException {
        assertFalse(recommendationService.getRecommendationsFor(null, new HashSet<>(), new String[]{"ru"}, 10).isEmpty());
    }

	@Test
	@Transactional
	public void categoriesTest(){
		UserMetadata userMetadata = userMetadataRepository.findById(20L).orElseThrow( () -> new RuntimeException("User not found"));
		assertFalse(userMetadata.getCategories().isEmpty());
	}



}
