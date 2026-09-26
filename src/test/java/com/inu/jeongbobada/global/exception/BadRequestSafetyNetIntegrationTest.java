package com.inu.jeongbobada.global.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.mail.EmailSender;
import com.inu.jeongbobada.global.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #119: "클라이언트가 잘못된 요청을 보내도 500은 나지 않는다"를 모든 API에 대해 검사하는 안전망.
// API 목록을 RequestMappingHandlerMapping에서 자동으로 모으므로, 새 API를 추가하면 테스트를 따로 쓰지 않아도 검사 대상이 된다.
// 이 테스트가 실패하면 실패 목록(메서드·경로·요청 유형·상태 코드)이 메시지로 나온다. 고치는 방법:
//   - 요청 DTO에 검증 애노테이션 + 컨트롤러에 @Valid (필수값 누락이 서비스까지 내려가 500이 되는 경우가 가장 흔하다)
//   - 필터에서 예외를 던지지 않기 (#110), 새 예외는 GlobalExceptionHandler가 ApiResponse로 감싸는지 확인
//   - 당장 못 고치면 버그 이슈를 만들고 KNOWN_500에 이슈 번호와 함께 등록 (고치면 반드시 제거 — 이슈 체크리스트에 적어둘 것)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BadRequestSafetyNetIntegrationTest {

    // 아직 고치지 못한 알려진 500. "METHOD 경로패턴 요청유형" → 이슈 번호. 이 조합에서 난 500만 건너뛴다.
    // 이슈가 해결되면 반드시 여기서 지운다 (그 이슈의 체크리스트에 적어둘 것).
    // "이제 500이 아니면 실패"로 강제하지 않는 이유: 500까지 도달하는지가 DB 데이터에 따라 달라진다.
    // 예) 강의평 {}는 과목 1번이 있어야 서비스까지 가서 500이 나는데, CI의 빈 DB에서는 404로 먼저 끝난다.
    private static final Map<String, String> KNOWN_500 = Map.of();

    // 잘못된 요청 유형. body를 받는 메서드(POST/PUT/PATCH)에만 body 유형을 보낸다.
    private enum BodyCase {
        NO_BODY(null, null),
        BROKEN_JSON(MediaType.APPLICATION_JSON, "{"),
        EMPTY_OBJECT(MediaType.APPLICATION_JSON, "{}"),
        JSON_ARRAY(MediaType.APPLICATION_JSON, "[]"),
        TEXT_PLAIN(MediaType.TEXT_PLAIN, "hello");

        private final MediaType contentType;
        private final String body;

        BodyCase(MediaType contentType, String body) {
            this.contentType = contentType;
            this.body = body;
        }
    }

    // 경로 변수 자리에 넣을 값: 정상 숫자 / 문자열 / Long 범위 초과
    private static final List<String> PATH_VALUES = List.of("1", "abc", "99999999999999999999");

    private static final Set<RequestMethod> BODY_METHODS = Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 잘못된 요청만 보내므로 메일이 나갈 일은 없지만, 외부 호출은 테스트에서 막아둔다
    @MockitoBean
    private EmailSender emailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String studentId;
    private String accessToken;

    // 로그인이 필요한 API는 토큰이 없으면 필터에서 401로 끝나서 컨트롤러까지 가지 않는다.
    // 컨트롤러·서비스까지 도달하는 경로도 검사하려고, 토큰 없는 요청과 토큰 있는 요청을 둘 다 보낸다.
    @BeforeEach
    void signUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1","nickname":"sn%d","email":"sn%d@example.com"}
                    """.formatted(studentId, n % 1_000_000, n)))
            .andExpect(status().isCreated());
        accessToken = jwtTokenProvider.createAccessToken(studentId);
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void 모든_API는_잘못된_요청에_500이_아니라_ApiResponse_형식의_4xx로_응답한다() throws Exception {
        List<String> failures = new ArrayList<>();
        int requests = 0;

        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith("/api/")) {
                    continue; // swagger 등 우리 API가 아닌 것
                }
                for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
                    List<BodyCase> bodyCases = BODY_METHODS.contains(method) ? List.of(BodyCase.values()) : List.of(BodyCase.NO_BODY);
                    for (String path : expandPath(pattern)) {
                        for (BodyCase bodyCase : bodyCases) {
                            for (boolean withToken : List.of(false, true)) {
                                requests++;
                                String key = method + " " + pattern + " " + bodyCase;
                                String label = method + " " + path + " [" + bodyCase + (withToken ? ", 토큰" : ", 토큰 없음") + "]";
                                MockHttpServletResponse response = perform(method, path, bodyCase, withToken);
                                String problem = check(response);

                                if (KNOWN_500.containsKey(key) && response.getStatus() >= 500) {
                                    continue;
                                }
                                if (problem != null) {
                                    failures.add(label + " → " + problem);
                                }
                            }
                        }
                    }
                }
            }
        }

        assertThat(requests).as("검사한 요청 수 — 0이면 API 목록을 못 모은 것").isGreaterThan(100);
        assertThat(failures).as("잘못된 요청에 500이 나거나 ApiResponse 형식이 아닌 응답 (%d건 중)", requests).isEmpty();
    }

    private MockHttpServletResponse perform(RequestMethod method, String path, BodyCase bodyCase, boolean withToken) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method.name()), URI.create(path));
        if (bodyCase.contentType != null) {
            builder.contentType(bodyCase.contentType).content(bodyCase.body);
        }
        if (withToken) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    // 문제가 없으면 null
    private String check(MockHttpServletResponse response) throws Exception {
        int status = response.getStatus();
        if (status >= 500) {
            return status + " " + response.getContentAsString(StandardCharsets.UTF_8);
        }
        if (status < 400) {
            return null; // 정상 응답 (예: GET /api/courses/1)
        }
        JsonNode json;
        try {
            json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return status + " 응답이 JSON이 아님: " + response.getContentAsString(StandardCharsets.UTF_8);
        }
        if (json == null || !json.path("success").isBoolean() || json.path("success").asBoolean()
                || json.path("code").asText("").isBlank()) {
            return status + " ApiResponse 형식이 아님: " + json;
        }
        return null;
    }

    // "/api/courses/{courseId}/reviews" → 경로 변수마다 PATH_VALUES를 넣은 경로들
    private static List<String> expandPath(String pattern) {
        if (!pattern.contains("{")) {
            return List.of(pattern);
        }
        return PATH_VALUES.stream()
            .map(value -> pattern.replaceAll("\\{[^}]+}", value))
            .toList();
    }
}
