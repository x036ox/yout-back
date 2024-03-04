package com.artur.youtback;

import com.artur.youtback.entity.user.UserMetadata;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.repository.UserMetadataRepository;
import com.artur.youtback.service.RecommendationService;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Transactional
@Rollback
@EmbeddedKafka
@SpringBootTest(classes = YoutBackApplication.class)
@ActiveProfiles("dev")
public class YoutBackApplicationTests {
	public static final String TEST_VIDEO_FILE = "src/test/files/Video.mp4";
	public static final String TEST_IMAGE_FILE = "src/test/files/Image.jpg";

    @MockBean
    MinioClient minioClient;
	@MockBean
	protected KafkaTemplate<String, String> processingServiceTemplate;
	protected MockConsumer<String, String> mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);

	@Autowired
    UserMetadataRepository userMetadataRepository;
	@Autowired
    RecommendationService recommendationService;

	@Test
	public void recommendationsTest() throws NotFoundException {
        assertFalse(recommendationService.getRecommendationsFor(null, 0, new String[]{"ru"}, 10).isEmpty());
    }

	@Test
	@Transactional
	public void categoriesTest(){
		UserMetadata userMetadata = userMetadataRepository.findById(20L).orElseThrow( () -> new RuntimeException("User not found"));
		assertFalse(userMetadata.getCategories().isEmpty());
	}



}
