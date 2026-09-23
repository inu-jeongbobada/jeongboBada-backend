package com.inu.jeongbobada.domain.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #129: 로그아웃이 서버(DB)의 refresh token을 실제로 무효화하는지. 로그아웃 뒤 그 refresh로 재발급이 막혀야 한다.
// 예전에는 access token이 만료되면 사용자를 특정하지 못해 refresh가 14일 동안 남았다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LogoutIntegrationTest {

    // 서명은 맞지만 이미 만료된 토큰을 만들 수 없어서, 만료·위조처럼 "검증에 실패하는 access token"으로 대신한다
    private static final String INVALID_ACCESS_TOKEN = "expired.or.invalid";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String studentId;

    @BeforeEach
    void signUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1","nickname":"lg%d","email":"lg%d@example.com"}
                    """.formatted(studentId, n % 1_000_000, n)))
            .andExpect(status().isCreated());
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void access_token이_무효해도_refresh_token으로_로그아웃하면_그_refresh로_재발급할_수_없다() throws Exception {
        JsonNode tokens = login();

        logout(tokens.path("refreshToken").asText(), INVALID_ACCESS_TOKEN)
            .andExpect(status().isOk());

        reissue(tokens.path("refreshToken").asText())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 예전_방식대로_유효한_access_token_헤더만_보내도_로그아웃된다() throws Exception {
        JsonNode tokens = login();

        logout(null, tokens.path("accessToken").asText())
            .andExpect(status().isOk());

        reissue(tokens.path("refreshToken").asText())
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 이미_교체된_예전_refresh로_로그아웃해도_현재_refresh는_지워지지_않는다() throws Exception {
        JsonNode first = login();
        // JWT 발급 시각은 초 단위이고 토큰에 고유 ID가 없어서, 같은 초에 재발급하면 예전 토큰과 똑같은 문자열이 된다
        Thread.sleep(1_100);
        JsonNode rotated = read(reissue(first.path("refreshToken").asText())
            .andExpect(status().isOk()));

        // 재발급으로 교체된 예전 refresh — 다른 기기의 현재 로그인을 끊으면 안 된다
        logout(first.path("refreshToken").asText(), null)
            .andExpect(status().isOk());

        reissue(rotated.path("refreshToken").asText())
            .andExpect(status().isOk());
    }

    @Test
    void refresh_token도_헤더도_없으면_400() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.message").value("로그아웃할 refreshToken(body) 또는 Authorization 헤더가 필요합니다"));
    }

    private JsonNode login() throws Exception {
        return read(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1"}
                    """.formatted(studentId)))
            .andExpect(status().isOk()));
    }

    private ResultActions logout(String refreshToken, String accessToken) throws Exception {
        var request = post("/api/auth/logout");
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        if (refreshToken != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"%s"}
                    """.formatted(refreshToken));
        }
        return mockMvc.perform(request);
    }

    private ResultActions reissue(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/reissue")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken":"%s"}
                """.formatted(refreshToken)));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }
}
