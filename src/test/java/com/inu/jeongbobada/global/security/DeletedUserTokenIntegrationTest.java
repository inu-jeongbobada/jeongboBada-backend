package com.inu.jeongbobada.global.security;

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

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #110: 서명·만료가 유효한 토큰인데 사용자가 DB에서 삭제된 경우. 필터에서 예외가 새어 나가면
// 보호 API뿐 아니라 공개 API까지 500이 되므로, 실제 필터 체인을 거쳐 응답 코드를 확인한다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DeletedUserTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String studentId;
    private String accessToken;

    @BeforeEach
    void signUpThenDeleteUser() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1","nickname":"d%d","email":"del%d@example.com"}
                    """.formatted(studentId, n % 1_000_000, n)))
            .andExpect(status().isCreated());

        accessToken = jwtTokenProvider.createAccessToken(studentId);

        // 토큰을 발급받은 뒤 사용자가 탈퇴/삭제된 상황
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void 삭제된_사용자의_토큰으로_공개_API를_호출하면_토큰을_무시하고_200() throws Exception {
        mockMvc.perform(get("/api/courses")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    void 삭제된_사용자의_토큰으로_보호_API를_호출하면_401() throws Exception {
        mockMvc.perform(patch("/api/users/me/nickname")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nickname":"newnick"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
