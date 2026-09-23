package com.inu.jeongbobada.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    // 로그인이 필요한 API에 @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)으로 붙인다.
    public static final String BEARER_SCHEME_NAME = "bearerAuth";

    private static final String ERROR_SCHEMA = "ErrorResponse";
    private static final String ERROR_CODE_TABLE_URL =
        "https://github.com/inu-jeongbobada/jeongboBada-backend/blob/develop/docs/api-spec.md#에러-코드";

    // Swagger UI에 Authorize 버튼을 추가한다. 로그인으로 받은 JWT를 여기 넣으면
    // 로그인이 필요한 API(자물쇠 표시) 요청에 Authorization 헤더가 자동으로 붙는다.
    // 문서 전체에 security를 걸면 공개 API에도 자물쇠가 붙어서, API별로 @SecurityRequirement를 단다 (#124).
    // 표시와 실제 401 동작이 어긋나지 않는지는 SwaggerAuthConsistencyIntegrationTest가 검사한다.
    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

        return new OpenAPI()
            .info(new Info()
                .title("정보바다 API")
                .description("""
                    자물쇠가 있는 API는 로그인이 필요하다. `POST /api/auth/login`으로 받은 accessToken을 Authorize에 넣고 호출한다.

                    에러 응답은 모두 `{ success: false, code, message, errors? }` 형식이다. 프론트는 `code`로 분기한다.
                    각 API에는 공통 에러(400/401/500)만 표시돼 있고, 도메인별 code(예: 409 `DUPLICATE_NICKNAME`)는 \
                    [에러 코드 표](%s)를 참고한다.""".formatted(ERROR_CODE_TABLE_URL)))
            .components(new Components()
                .addSecuritySchemes(BEARER_SCHEME_NAME, bearerScheme));
    }

    // 모든 API에 공통 에러 응답을 붙인다. 컨트롤러에서 같은 상태 코드를 직접 문서화했으면 그쪽을 유지한다.
    // ErrorResponse 스키마를 여기서 등록하는 이유: openAPI()에서 등록하면 그 시점엔 아무 API도 참조하지 않아
    // springdoc이 "안 쓰는 스키마"로 지운다. 참조를 붙이는 이 단계에서 함께 넣어야 문서에 남는다.
    @Bean
    public OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(ERROR_SCHEMA, errorResponseSchema());
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses() != null ? operation.getResponses() : new ApiResponses();
                if (acceptsInput(operation)) {
                    responses.putIfAbsent("400", errorResponse(
                        "입력값 오류 — 필드별 사유는 errors",
                        Map.of("success", false, "code", "INVALID_INPUT_VALUE", "message", "학번은 필수입니다.",
                            "errors", List.of(Map.of("field", "studentId", "message", "학번은 필수입니다.")))));
                }
                if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                    responses.putIfAbsent("401", errorResponse(
                        "로그인 필요 — access token 없음/만료/위조. reissue 후 다시 호출",
                        Map.of("success", false, "code", "AUTHENTICATION_REQUIRED", "message", "인증이 필요합니다")));
                }
                responses.putIfAbsent("500", errorResponse(
                    "서버 오류",
                    Map.of("success", false, "code", "INTERNAL_SERVER_ERROR", "message", "서버 내부 오류가 발생했습니다")));
                operation.setResponses(responses);
            }));
        };
    }

    private static boolean acceptsInput(Operation operation) {
        return operation.getRequestBody() != null
            || (operation.getParameters() != null && !operation.getParameters().isEmpty());
    }

    private static ApiResponse errorResponse(String description, Map<String, Object> example) {
        return new ApiResponse()
            .description(description)
            .content(new Content().addMediaType("application/json", new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA))
                .example(example)));
    }

    // global/common/ApiResponse의 에러 형태
    @SuppressWarnings("rawtypes")
    private static Schema errorResponseSchema() {
        Schema<?> fieldError = new ObjectSchema()
            .addProperty("field", new StringSchema().description("문제가 된 필드·파라미터 이름").example("studentId"))
            .addProperty("message", new StringSchema().description("사유").example("학번은 필수입니다."));

        return new ObjectSchema()
            .description("에러 응답. code 목록은 docs/api-spec.md \"에러 코드\" 표")
            .addProperty("success", new BooleanSchema().example(false))
            .addProperty("code", new StringSchema().description("프론트가 분기에 쓰는 에러 code").example("INVALID_INPUT_VALUE"))
            .addProperty("message", new StringSchema().description("사용자에게 보여줄 대표 문구"))
            .addProperty("errors", new ArraySchema().items(fieldError)
                .description("필드별 오류. 입력값 오류(400)일 때만 있고, 나머지 에러에는 이 키가 없다"));
    }
}
