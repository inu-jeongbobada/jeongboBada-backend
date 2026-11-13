package com.inu.jeongbobada.global.file;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// aws.region, aws.s3.bucket을 바인딩. 값이 비면 기동 단계에서 실패한다 (JwtProperties와 같은 이유, #93).
// 자격 증명(access key 등)은 여기 두지 않는다. SDK가 환경변수·인스턴스 역할에서 직접 찾는다 (S3Config 참고).
@Validated
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
    @NotBlank(message = "aws.region(AWS_REGION)이 비어 있습니다. 예: us-east-1")
    String region,
    @Valid @NotNull(message = "aws.s3.bucket(AWS_S3_BUCKET)이 비어 있습니다")
    S3 s3
) {
    public record S3(
        @NotBlank(message = "aws.s3.bucket(AWS_S3_BUCKET)이 비어 있습니다")
        String bucket
    ) {
    }
}
