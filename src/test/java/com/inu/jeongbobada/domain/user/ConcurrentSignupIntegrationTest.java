package com.inu.jeongbobada.domain.user;

import com.inu.jeongbobada.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

// #109: 가입은 "사전 중복 확인(findBy...) → 저장" 구조라, 확인과 저장 사이에 같은 학번 요청이 끼어들면
// DB UNIQUE 제약 위반이 올라온다(가입 버튼 더블클릭). 그 요청이 500이 아니라 409로 나가는지 확인한다.
// 실제 동시 요청이어야 재현되므로 MockMvc가 아니라 실제 포트로 띄운 서버에 병렬로 보낸다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConcurrentSignupIntegrationTest {

    private static final int REQUESTS = 10;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private UserRepository userRepository;

    private String studentId;

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void 같은_학번으로_동시에_가입하면_1건만_성공하고_나머지는_409() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);

        HttpClient client = HttpClient.newHttpClient();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < REQUESTS; i++) {
            // 학번만 같고 닉네임·이메일은 달라서, 겹치는 건 학번 UNIQUE 하나뿐이다
            String body = """
                {"studentId":"%s","password":"password1","nickname":"cs%d_%d","email":"cs%d_%d@example.com"}
                """.formatted(studentId, n % 10_000, i, n, i);
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/signup"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            results.add(pool.submit(() -> {
                start.await(); // 모든 스레드가 준비된 뒤 동시에 출발
                return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            }));
        }
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s != 201).allMatch(s -> s == 409);
    }
}
