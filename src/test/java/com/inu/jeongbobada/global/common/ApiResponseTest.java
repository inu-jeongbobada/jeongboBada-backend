package com.inu.jeongbobada.global.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ok_응답은_httpStatus는_숨기고_data만_내려간다() throws Exception {
        // given
        String data = "hello";

        // when
        ApiResponse<String> response = ApiResponse.ok(data);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        // then
        assertThat(response.httpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.success()).isTrue();
        assertThat(json.has("httpStatus")).isFalse();
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").asText()).isEqualTo("hello");
        assertThat(json.has("message")).isFalse();
    }

    @Test
    void created_응답은_httpStatus가_CREATED다() {
        // given
        String data = "hello";

        // when
        ApiResponse<String> response = ApiResponse.created(data);

        // then
        assertThat(response.httpStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.success()).isTrue();
    }

    @Test
    void error_응답은_data는_숨기고_message만_내려간다() throws Exception {
        // given
        String message = "잘못된 요청입니다";

        // when
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST, message);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        // then
        assertThat(response.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.success()).isFalse();
        assertThat(json.has("data")).isFalse();
        assertThat(json.get("message").asText()).isEqualTo(message);
    }

    @Test
    void error_ErrorCode로_만들면_code와_httpStatus가_ErrorCode를_따라간다() throws Exception {
        // given
        GlobalErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;

        // when
        ApiResponse<Void> response = ApiResponse.error(errorCode);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        // then
        assertThat(response.httpStatus()).isEqualTo(errorCode.getHttpStatus());
        assertThat(json.get("code").asText()).isEqualTo(errorCode.getCode());
        assertThat(json.get("message").asText()).isEqualTo(errorCode.getMessage());
    }

    @Test
    void 필드별_오류가_있으면_errors로_내려가고_없으면_빠진다() throws Exception {
        // given
        ApiResponse<Void> withErrors = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE, "학번은 필수입니다.",
            java.util.List.of(new ApiResponse.FieldError("studentId", "학번은 필수입니다.")));
        ApiResponse<Void> withoutErrors = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE);

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(withErrors));
        JsonNode jsonWithout = objectMapper.readTree(objectMapper.writeValueAsString(withoutErrors));

        // then
        assertThat(json.get("errors").get(0).get("field").asText()).isEqualTo("studentId");
        assertThat(json.get("errors").get(0).get("message").asText()).isEqualTo("학번은 필수입니다.");
        assertThat(jsonWithout.has("errors")).isFalse();
    }
}
