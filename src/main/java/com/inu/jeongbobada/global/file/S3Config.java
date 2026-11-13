package com.inu.jeongbobada.global.file;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// 자격 증명은 설정 파일이 아니라 SDK 기본 방식으로 찾는다.
// 로컬·Learner Lab은 환경변수(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN), EC2는 인스턴스 역할.
// 키는 S3를 실제로 호출할 때 찾으므로, 키가 없는 CI·compose 스모크에서도 서버는 뜬다.
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {

    @Bean(destroyMethod = "close")
    public DefaultCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client(AwsProperties properties, DefaultCredentialsProvider credentialsProvider) {
        return S3Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(AwsProperties properties, DefaultCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider)
            .build();
    }
}
