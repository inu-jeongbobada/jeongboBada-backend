package com.inu.jeongbobada.domain.user;

import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.mail.EmailSender;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 DB(MySQL)에 저장/조회하면서 컨트롤러 → 서비스 → 트랜잭션 → JPA 전체 경로를 검증한다.
// 서비스 단위 테스트(mock)로는 잡히지 않는 것들이 목적:
//  - 틀린 코드 예외가 나도 "실패 횟수"가 DB에 커밋되는가 (noRollbackFor)
//  - Embedded 인증코드가 저장/조회/폐기(null)되는가
//  - SecurityConfig 경로 규칙 (/api/auth/** 공개, /api/users/me/** 인증 필요)
// 테스트는 트랜잭션으로 감싸지 않고, 만든 사용자는 끝나고 직접 지운다.
// CI는 application.yml 없이 datasource 환경변수만 주입하고 ddl-auto를 따로 주지 않는다 (MySQL 기본값은 none).
// 그러면 빈 DB에 테이블이 없어서 이 테스트가 실패하므로, 테이블을 스스로 만들도록 여기서 지정한다.
// (기존 contextLoads 테스트는 테이블에 접근하지 않아서 이 설정 없이도 통과했다.)
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {

    private static final String PASSWORD = "password1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailSender emailSender;

    private final List<String> createdStudentIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdStudentIds.forEach(id -> userRepository.findByStudentId(id).ifPresent(userRepository::delete));
    }

    // 테스트끼리 / 기존 DB 데이터와 겹치지 않도록 매번 랜덤 값을 쓴다
    private record Account(String studentId, String nickname, String email) {
    }

    private Account signup() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        Account account = new Account(
            String.format("9%08d", n), "t" + String.format("%08d", n), "it" + n + "@example.com");
        createdStudentIds.add(account.studentId());

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"%s","nickname":"%s","email":"%s"}
                    """.formatted(account.studentId(), PASSWORD, account.nickname(), account.email())))
            .andExpect(status().isCreated());
        return account;
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String sentCode(String to) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(to), anyString(), body.capture());
        Matcher matcher = Pattern.compile("\\d{6}").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private String login(Account account, String password) throws Exception {
        String body = postJson("/api/auth/login",
            """
            {"studentId":"%s","password":"%s"}
            """.formatted(account.studentId(), password))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.accessToken");
    }

    @Test
    void 비밀번호_찾기_전체_흐름_코드발송_확인_재설정_새비밀번호로_로그인() throws Exception {
        Account account = signup();

        postJson("/api/auth/password-reset/send-code",
            """
            {"studentId":"%s","email":"%s"}
            """.formatted(account.studentId(), account.email()))
            .andExpect(status().isOk());
        String code = sentCode(account.email());

        postJson("/api/auth/password-reset/verify-code",
            """
            {"studentId":"%s","code":"%s"}
            """.formatted(account.studentId(), code))
            .andExpect(status().isOk());

        String resetJson = """
            {"studentId":"%s","code":"%s","newPassword":"newPassword1"}
            """.formatted(account.studentId(), code);
        postJson("/api/auth/password-reset", resetJson).andExpect(status().isOk());

        // 새 비밀번호로만 로그인된다
        login(account, "newPassword1");
        postJson("/api/auth/login",
            """
            {"studentId":"%s","password":"%s"}
            """.formatted(account.studentId(), PASSWORD))
            .andExpect(status().isUnauthorized());

        // 코드는 1회용 — DB에서 폐기돼 같은 코드로 다시 재설정할 수 없다
        postJson("/api/auth/password-reset", resetJson)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("USER_400"));
    }

    @Test
    void 코드를_다섯_번_틀리면_DB에서_폐기돼서_올바른_코드도_통하지_않는다() throws Exception {
        Account account = signup();
        postJson("/api/auth/password-reset/send-code",
            """
            {"studentId":"%s","email":"%s"}
            """.formatted(account.studentId(), account.email()))
            .andExpect(status().isOk());
        String code = sentCode(account.email());
        String wrong = code.equals("000000") ? "111111" : "000000";

        // 요청 하나하나가 별도 트랜잭션 — 예외로 롤백됐다면 횟수가 쌓이지 않아 여기서 계속 400만 나오고
        // 마지막의 "올바른 코드"가 통과해버린다.
        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/password-reset/verify-code",
                """
                {"studentId":"%s","code":"%s"}
                """.formatted(account.studentId(), wrong))
                .andExpect(status().isBadRequest());
        }

        postJson("/api/auth/password-reset/verify-code",
            """
            {"studentId":"%s","code":"%s"}
            """.formatted(account.studentId(), code))
            .andExpect(status().isBadRequest());
    }

    @Test
    void 없는_학번과_틀린_이메일도_성공과_똑같이_200이고_메일은_안_간다() throws Exception {
        Account account = signup();

        postJson("/api/auth/password-reset/send-code",
            """
            {"studentId":"%s","email":"someone@example.com"}
            """.formatted(account.studentId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
        postJson("/api/auth/password-reset/send-code",
            """
            {"studentId":"000000000","email":"%s"}
            """.formatted(account.email()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 이미_가입된_이메일로는_회원가입할_수_없다() throws Exception {
        Account account = signup();

        postJson("/api/auth/signup",
            """
            {"studentId":"%s","password":"%s","nickname":"%s","email":"%s"}
            """.formatted("8" + account.studentId().substring(1), PASSWORD,
                "x" + account.nickname().substring(1), account.email().toUpperCase()))
            .andExpect(status().isConflict());
    }

    @Test
    void 이메일_변경_2단계_새이메일로_코드받고_코드와_현재비밀번호로_변경() throws Exception {
        Account account = signup();
        String token = login(account, PASSWORD);
        String newEmail = "changed" + account.studentId() + "@example.com";

        // 로그인 없이는 접근 불가
        postJson("/api/users/me/email/send-code", """
            {"newEmail":"%s"}
            """.formatted(newEmail))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users/me/email/send-code")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"newEmail":"%s"}
                    """.formatted(newEmail)))
            .andExpect(status().isOk());
        String code = sentCode(newEmail);

        // 현재 비밀번호가 틀리면 코드가 맞아도 거부
        mockMvc.perform(patch("/api/users/me/email")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"newEmail":"%s","code":"%s","currentPassword":"wrongPassword1"}
                    """.formatted(newEmail, code)))
            .andExpect(status().isUnauthorized());
        assertThat(userRepository.findByStudentId(account.studentId()).orElseThrow().getEmail())
            .isEqualTo(account.email());

        mockMvc.perform(patch("/api/users/me/email")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"newEmail":"%s","code":"%s","currentPassword":"%s"}
                    """.formatted(newEmail, code, PASSWORD)))
            .andExpect(status().isOk());

        var user = userRepository.findByStudentId(account.studentId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(newEmail);
        assertThat(user.getEmailChangeCode()).isNull();
        assertThat(user.getPendingEmail()).isNull();
    }

    @Test
    void 이메일_변경용_코드로는_비밀번호_재설정이_안_된다() throws Exception {
        Account account = signup();
        String token = login(account, PASSWORD);
        String attackerEmail = "attacker" + account.studentId() + "@example.com";

        mockMvc.perform(post("/api/users/me/email/send-code")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"newEmail":"%s"}
                    """.formatted(attackerEmail)))
            .andExpect(status().isOk());
        String emailChangeCode = sentCode(attackerEmail);

        postJson("/api/auth/password-reset",
            """
            {"studentId":"%s","code":"%s","newPassword":"hacked12345"}
            """.formatted(account.studentId(), emailChangeCode))
            .andExpect(status().isBadRequest());

        login(account, PASSWORD); // 비밀번호는 그대로
    }
}
