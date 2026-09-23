package com.inu.jeongbobada;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// test 프로필: CI에는 application.yml이 없어서 jwt.secret 등 필수 설정을 application-test.properties에서 받는다
@SpringBootTest
@ActiveProfiles("test")
class JeongbobadaApplicationTests {

	@Test
	void contextLoads() {
	}

}
