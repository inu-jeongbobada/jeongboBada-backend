package com.inu.jeongbobada.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #96: 내 정보 조회. 로그인한 본인 정보(학번·이메일 포함)를 돌려주고, 토큰이 없으면 401이어야 한다.
// #165: 프론트가 관리자 화면을 열지 판단하도록 role도 내려준다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MyInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String studentId;
    private String nickname;
    private String email;

    @BeforeEach
    void signUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);
        nickname = "me" + (n % 10_000);
        email = "me" + n + "@example.com";
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1","nickname":"%s","email":"%s"}
                    """.formatted(studentId, nickname, email)))
            .andExpect(status().isCreated());
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void 로그인한_사용자는_자기_정보를_조회한다() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + login()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.studentId").value(studentId))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.department").isEmpty())
            .andExpect(jsonPath("$.data.role").value("USER"));
    }

    // 관리자 승격은 문서(auth-architecture.md)에 적은 SQL 그대로 한다. 권한은 로그인할 때 읽으므로 승격 뒤에 로그인한다
    @Test
    void DB에서_관리자로_승격한_사용자는_role이_ADMIN이다() throws Exception {
        jdbcTemplate.update("update users set role = 'ADMIN' where student_id = ?", studentId);

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + login()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1"}
                    """.formatted(studentId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }
}
