package com.inu.jeongbobada.domain.user;

import com.inu.jeongbobada.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 틀린 횟수는 예외가 나도 커밋돼야 하고 동시 요청에서도 정확해야 해서, 실제 DB로 확인한다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LoginIntegrationTest {

    private static final String PASSWORD = "password1";
    private static final String WRONG_PASSWORD = "wrongPassword1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String studentId;

    @BeforeEach
    void signUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"%s","nickname":"li%d","email":"li%d@example.com"}
                    """.formatted(studentId, PASSWORD, n % 10_000, n)))
            .andExpect(status().isCreated());
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    private ResultActions login(String id, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"studentId":"%s","password":"%s"}
                """.formatted(id, password)));
    }

    @Test
    void 올바른_비밀번호면_토큰을_발급한다() throws Exception {
        login(studentId, PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void 가입되지_않은_학번은_404로_구분한다() throws Exception {
        login("8" + studentId.substring(1), PASSWORD)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_ID_NOT_REGISTERED"));
    }

    @Test
    void 학번_형식이_틀리면_400이다() throws Exception {
        login("20230158a", PASSWORD)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("studentId"))
            .andExpect(jsonPath("$.errors[0].message").value("학번은 숫자 9자리로 입력해주세요"));
    }

    @Test
    void 비밀번호가_틀리면_실패_횟수를_알려준다() throws Exception {
        login(studentId, WRONG_PASSWORD)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value(containsString("(1/5회)")));

        login(studentId, WRONG_PASSWORD)
            .andExpect(jsonPath("$.message").value(containsString("(2/5회)")));
    }

    @Test
    void 로그인에_성공하면_실패_횟수가_초기화된다() throws Exception {
        login(studentId, WRONG_PASSWORD);
        login(studentId, PASSWORD).andExpect(status().isOk());

        login(studentId, WRONG_PASSWORD)
            .andExpect(jsonPath("$.message").value(containsString("(1/5회)")));
    }

    @Test
    void 다섯_번_틀리면_잠기고_올바른_비밀번호도_거부한다() throws Exception {
        for (int i = 0; i < 4; i++) {
            login(studentId, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }

        ResultActions fifth = login(studentId, WRONG_PASSWORD)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
        long retryAfter = Long.parseLong(fifth.andReturn().getResponse().getHeader("Retry-After"));
        assertThat(retryAfter).isBetween(1L, 600L);

        login(studentId, PASSWORD)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
    }

    @Test
    void 잠금_시간이_지나면_다시_로그인할_수_있다() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(studentId, WRONG_PASSWORD);
        }
        jdbcTemplate.update(
            "update users set login_locked_until = now() - interval 1 second where student_id = ?", studentId);

        login(studentId, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 동시에_틀려도_실패_횟수를_빠뜨리지_않는다() throws Exception {
        int requests = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return login(studentId, WRONG_PASSWORD).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get());
        }
        pool.shutdown();

        // 앞선 4건만 401이고, 5번째부터는 잠금이어야 한다
        assertThat(statuses).filteredOn(s -> s == 401).hasSize(4);
        assertThat(statuses).filteredOn(s -> s == 429).hasSize(4);
    }
}
