package com.inu.jeongbobada.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.global.mail.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

// #124: Swagger의 "로그인 필요(자물쇠)" 표시와 실제 동작이 일치하는지 모든 API에 대해 검사한다.
//   - 자물쇠가 있는 API  → 토큰 없이 호출하면 401 AUTHENTICATION_REQUIRED
//   - 자물쇠가 없는 API  → 토큰 없이 호출해도 AUTHENTICATION_REQUIRED가 나오지 않음
// 새 API를 SecurityConfig에서 authenticated()로 막고 @SecurityRequirement를 빼먹으면(또는 그 반대면) 실패한다.
// 고치는 방법: 로그인이 필요한 API에 @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)을 단다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SwaggerAuthConsistencyIntegrationTest {

    // 자물쇠가 있지만 토큰이 없을 때 401이 아닌 다른 4xx로 끝나는 API와 그 이유
    // (예: "POST /api/some/path", 400 — 이유를 주석으로). 지금은 없다.
    private static final Map<String, Integer> SECURED_BUT_NOT_401 = Map.of();

    @Autowired
    private MockMvc mockMvc;

    // 잘못된 요청만 보내므로 메일이 나갈 일은 없지만, 외부 호출은 테스트에서 막아둔다
    @MockitoBean
    private EmailSender emailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode apiDocs;

    @BeforeEach
    void loadApiDocs() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        apiDocs = objectMapper.readTree(json);
    }

    @Test
    void 문서_전체에_거는_인증_표시는_없다() {
        // 전체에 걸면 공개 API에도 자물쇠가 붙는다 — API별로 @SecurityRequirement를 달 것
        assertThat(apiDocs.path("security").isMissingNode() || apiDocs.path("security").isEmpty()).isTrue();
    }

    @Test
    void 자물쇠_표시와_토큰_없이_호출한_실제_결과가_일치한다() throws Exception {
        List<String> mismatches = new ArrayList<>();
        int secured = 0;

        for (Iterator<Map.Entry<String, JsonNode>> paths = apiDocs.path("paths").fields(); paths.hasNext(); ) {
            Map.Entry<String, JsonNode> path = paths.next();
            if (!path.getKey().startsWith("/api/")) {
                continue;
            }
            for (Iterator<Map.Entry<String, JsonNode>> ops = path.getValue().fields(); ops.hasNext(); ) {
                Map.Entry<String, JsonNode> op = ops.next();
                String method = op.getKey().toUpperCase();
                String key = method + " " + path.getKey();
                boolean lock = op.getValue().path("security").size() > 0;
                if (lock) {
                    secured++;
                }

                MockHttpServletResponse response = callWithoutToken(method, path.getKey(), op.getValue().has("requestBody"));
                String code = codeOf(response);
                boolean authRequired = response.getStatus() == 401 && "AUTHENTICATION_REQUIRED".equals(code);

                if (lock && SECURED_BUT_NOT_401.containsKey(key)) {
                    if (response.getStatus() != SECURED_BUT_NOT_401.get(key)) {
                        mismatches.add(key + ": 예외 목록에는 " + SECURED_BUT_NOT_401.get(key) + "인데 실제 " + response.getStatus());
                    }
                } else if (lock && !authRequired) {
                    mismatches.add(key + ": Swagger엔 자물쇠가 있는데 토큰 없이 " + response.getStatus() + " " + code
                        + " — 실제로 로그인이 필요 없으면 @SecurityRequirement를 빼고, 필요하면 SecurityConfig/컨트롤러를 확인");
                } else if (!lock && authRequired) {
                    mismatches.add(key + ": 토큰 없이 401 AUTHENTICATION_REQUIRED인데 Swagger엔 자물쇠가 없음"
                        + " — @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME) 추가");
                }
            }
        }

        assertThat(secured).as("자물쇠가 있는 API 수 — 0이면 표시가 통째로 빠진 것").isGreaterThanOrEqualTo(4);
        assertThat(mismatches).as("Swagger 인증 표시와 실제 동작이 다름").isEmpty();
    }

    @Test
    void 모든_API에_공통_에러_응답이_ApiResponse_형식으로_문서화된다() {
        List<String> missing = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> paths = apiDocs.path("paths").fields(); paths.hasNext(); ) {
            Map.Entry<String, JsonNode> path = paths.next();
            if (!path.getKey().startsWith("/api/")) {
                continue;
            }
            for (Iterator<Map.Entry<String, JsonNode>> ops = path.getValue().fields(); ops.hasNext(); ) {
                Map.Entry<String, JsonNode> op = ops.next();
                JsonNode responses = op.getValue().path("responses");
                String key = op.getKey().toUpperCase() + " " + path.getKey();
                if (!responses.has("500")) {
                    missing.add(key + ": 500");
                }
                if (op.getValue().path("security").size() > 0 && !responses.has("401")) {
                    missing.add(key + ": 401");
                }
            }
        }
        assertThat(missing).isEmpty();
        assertThat(apiDocs.at("/components/schemas/ErrorResponse/properties").has("errors")).isTrue();
    }

    private MockHttpServletResponse callWithoutToken(String method, String pathPattern, boolean hasBody) throws Exception {
        String path = pathPattern.replaceAll("\\{[^}]+}", "1");
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), URI.create(path));
        if (hasBody) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    private String codeOf(MockHttpServletResponse response) {
        try {
            return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8)).path("code").asText("");
        } catch (Exception e) {
            return "";
        }
    }
}
