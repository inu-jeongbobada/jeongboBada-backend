package com.inu.jeongbobada.global.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
