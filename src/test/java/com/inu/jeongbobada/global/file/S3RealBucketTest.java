package com.inu.jeongbobada.global.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 S3 버킷으로 업로드 → presigned URL로 내려받기 → 삭제까지 확인한다.
// S3_TEST_BUCKET이 있을 때만 돈다 (CI에는 없어서 자동 skip). 자격 증명은 S3Config와 같은 방식(환경변수)으로 찾는다.
//   S3_TEST_BUCKET=버킷 AWS_REGION=us-east-1 AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_SESSION_TOKEN=... \
//     ./gradlew test --tests '*S3RealBucketTest'
// 가짜 pdf만 올린다. 실제 학생 서류를 쓰지 말 것 (docs/file-upload-architecture.md).
@EnabledIfEnvironmentVariable(named = "S3_TEST_BUCKET", matches = ".+")
class S3RealBucketTest {

    private static final byte[] FAKE_PDF = "%PDF-1.7 jeongbobada S3RealBucketTest".getBytes();

    @Test
    void 업로드한_파일을_presigned_URL로_받고_삭제한다() throws Exception {
        AwsProperties properties = new AwsProperties(
            System.getenv().getOrDefault("AWS_REGION", "us-east-1"),
            new AwsProperties.S3(System.getenv("S3_TEST_BUCKET")));
        S3Config config = new S3Config();

        try (var credentials = config.awsCredentialsProvider();
             S3Client s3Client = config.s3Client(properties, credentials);
             S3Presigner s3Presigner = config.s3Presigner(properties, credentials);
             HttpClient http = HttpClient.newHttpClient()) {
            FileStorage fileStorage = new FileStorage(s3Client, s3Presigner, properties);

            String key = fileStorage.upload("test",
                new MockMultipartFile("file", "fake.pdf", "application/pdf", FAKE_PDF),
                Set.of(FileType.PDF), DataSize.ofMegabytes(1));
            try {
                String url = fileStorage.presignedGetUrl(key, Duration.ofMinutes(1));
                HttpResponse<byte[]> download = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
                assertThat(download.statusCode()).isEqualTo(200);
                assertThat(download.body()).isEqualTo(FAKE_PDF);
            } finally {
                fileStorage.delete(key);
            }

            assertThatThrownBy(() -> s3Client.headObject(
                    HeadObjectRequest.builder().bucket(properties.s3().bucket()).key(key).build()))
                .isInstanceOf(NoSuchKeyException.class);
        }
    }
}
