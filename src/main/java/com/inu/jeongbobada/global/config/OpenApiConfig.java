package com.inu.jeongbobada.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    // Swagger UI에 Authorize 버튼을 추가한다. 로그인으로 받은 JWT를 여기 넣으면
    // 인증이 필요한 API 요청에 Authorization 헤더가 자동으로 붙는다.
    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

        return new OpenAPI()
            .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, bearerScheme))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}
