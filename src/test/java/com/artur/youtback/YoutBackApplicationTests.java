package com.artur.youtback;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = YoutBackApplication.class)
@ActiveProfiles("dev")
class YoutBackApplicationTests {
//	private static final Logger logger = LoggerFactory.getLogger(YoutBackApplicationTests.class);
//
//	@Autowired
//	EmailService emailService;
//
//	@Autowired
//	UserRepository userRepository;
//	@Autowired
//	VideoRepository videoRepository;
//	@Autowired
//	VideoService videoService;
//	@Autowired
//    UserService userService;
//	@Autowired
//	LikeRepository likeRepository;
//	@Autowired
//	VideoMetadataRepository videoMetadataRepository;
//	@Autowired
//	UserMetadataRepository userMetadataRepository;
//	@Autowired
//	RecommendationService recommendationService;
//
//	@Test
//	void contextLoads() {
//	}
//
//	@Test
//	@Transactional
//	@Rollback
//	public void testEmail(){
//		emailService.sendEmail();
//	}
//	@Test
//	public void userRepoTests(){
//		userRepository.findMostSubscribes(Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
//		userRepository.findByAuthority(AppAuthorities.ADMIN.name(), Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
//	}
//
//	@Test
//	public void videoRepoTests(){
//		videoRepository.findMostDuration(Pageable.ofSize(2)).forEach(userEntity -> System.out.println(userEntity.getId()));
//		videoRepository.findMostLikes(Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
//		videoRepository.findMostViews(Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
//		videoRepository.findByTitle("r",Pageable.ofSize(10)).forEach(userEntity -> System.out.println(userEntity.getId()));
//	}
//
//	@Test
//	public void likeTest(){
//
//	}
//	@Transactional
//	@Test
//	public void recommendationsTest(){
//		Set<Long> exc = new HashSet<>();
//		exc.add(2L);
//		long start = System.currentTimeMillis();
//		List<Long> ids =  likeRepository.getFindRecommendationsTestIds(20L, Instant.now().minus(20, ChronoUnit.DAYS),exc , Pageable.ofSize(15));
//		long finish = System.currentTimeMillis() - start;
//		System.out.println("First request done in: " + finish + "ms");
//		start = System.currentTimeMillis();
//		List<VideoEntity> result = new ArrayList<>();
//		ids.forEach(id -> videoRepository.findById(id).ifPresent(result::add));
//		System.out.println("Second request done in: " + (System.currentTimeMillis() - start) + "ms");
//		//assertEquals(1, result.size());
//		start = System.currentTimeMillis();
//		UserEntity userEntity = userRepository.findById(20L).orElseThrow(() -> new RuntimeException("User not found"));
//		List<VideoEntity> secondResult =  likeRepository.getFindRecommendationsTest(20L, Instant.now().minus(20, ChronoUnit.DAYS),exc , Pageable.ofSize(15));
//		System.out.println("Third request done in: " + (System.currentTimeMillis() - start) + "ms");
//		assertEquals(result.stream().map(VideoEntity::getId).collect(Collectors.toList()), secondResult.stream().map(VideoEntity::getId).collect(Collectors.toList()));
//}
//
//
//	@Test
//	public void videoMetadataTest(){
//		Iterator<Map.Entry<String, Integer>> languages = userMetadataRepository.findById(20L).get().getLanguagesDec().entrySet().iterator();
//		while (languages.hasNext()){
//			System.out.println(languages.next());
//		}
//	}
//
//    @Test
//	@Transactional
//	@Rollback
//    public void shouldDecreaseCategoryPoints(){
//        var optionalVideoEntity = videoRepository.findById(131L);
//        var optionalUserEntity = userRepository.findById(20L);
//        assertTrue(optionalUserEntity.isPresent() && optionalVideoEntity.isPresent());
//        UserEntity userEntity = optionalUserEntity.get();
//        VideoEntity videoEntity = optionalVideoEntity.get();
//		int oldValue = userEntity.getUserMetadata().getCategories().get(videoEntity.getVideoMetadata().getCategory());
//        try {
//            userService.notInterested(videoEntity.getId(), userEntity.getId());
//        } catch (VideoNotFoundException | UserNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        String category = optionalVideoEntity.get().getVideoMetadata().getCategory();
//        Map<String, Integer> userCategories = userEntity.getUserMetadata().getCategories();
//        assertEquals((int)(oldValue * 0.25f), userRepository.findById(userEntity.getId()).get().getUserMetadata().getCategories().get(category));
//		logger.trace("old value: " + oldValue + " new value: " + (int)(oldValue * 0.25));
//    }
//
//	@Test
//	@Transactional
//	public void categoriesTest(){
//		UserMetadata userMetadata = userMetadataRepository.findById(20L).orElseThrow( () -> new RuntimeException("User not found"));
//		assertFalse(userMetadata.getCategories().isEmpty());
//	}
//@Test
//public void profilesTest(@Autowired Ffmpeg ffmpeg){
//    try {
//        ffmpeg.convertVideoToHls(new File(AppConstants.VIDEO_PATH + "Today.mp4"));
//    } catch (IOException | InterruptedException e) {
//        e.printStackTrace();
//    }
//}
}
