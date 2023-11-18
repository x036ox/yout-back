package com.artur.youtback;

import com.artur.youtback.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = YoutBackApplication.class)
class YoutBackApplicationTests {

	@Autowired
	EmailService emailService;
	@Test
	void contextLoads() {
	}

	@Test
	public void testEmail(){
		emailService.sendEmail();
	}

}
