package com.inu.jeongbobada.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

// #93: jwt.secret이 비었거나 짧으면 서버가 기동 단계에서 실패해야 한다.
// 검증이 없으면 서버는 정상으로 뜨고 첫 로그인에서야 500이 난다 (docker compose에서 JWT_SECRET을 빼먹은 경우).
class JwtPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
        .withUserConfiguration(Config.class)
        .withPropertyValues("jwt.expiration=3600000", "jwt.refresh-expiration=1209600000");

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class Config {
    }

    @Test
    void secret이_비어_있으면_기동에_실패한다() {
        runner.withPropertyValues("jwt.secret=")
            .run(context -> assertThat(context).hasFailed()
                .getFailure().rootCause().hasMessageContaining("jwt.secret"));
    }

    @Test
    void secret이_32자보다_짧으면_기동에_실패한다() {
        runner.withPropertyValues("jwt.secret=too-short-secret")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void secret이_32자_이상이면_기동된다() {
        runner.withPropertyValues("jwt.secret=integration-test-only-secret-key-that-is-at-least-32-bytes-long")
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtProperties.class));
    }
}
