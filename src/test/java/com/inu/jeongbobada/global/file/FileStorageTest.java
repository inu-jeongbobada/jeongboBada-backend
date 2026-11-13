package com.inu.jeongbobada.global.file;

import com.inu.jeongbobada.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// S3 없이 검증 규칙과 S3 호출 내용을 확인한다. 실제 버킷 확인은 S3RealBucketTest.
// presign은 네트워크 없이 서명만 계산하므로, 가짜 키를 넣은 진짜 S3Presigner로 확인한다.
class FileStorageTest {

    private static final Set<FileType> PDF_JPG = Set.of(FileType.PDF, FileType.JPG);
    private static final DataSize TEN_MB = DataSize.ofMegabytes(10);
    private static final byte[] PDF = "%PDF-1.7 test".getBytes();
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = S3Presigner.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
            .build();
        fileStorage = new FileStorage(s3Client, s3Presigner,
            new AwsProperties("us-east-1", new AwsProperties.S3("test-bucket")));
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @Test
    void pdf를_올리면_prefix와_UUID로_된_key를_돌려준다() {
        String key = fileStorage.upload("verification", file("재학증명서.pdf", "application/pdf", PDF), PDF_JPG, TEN_MB);

        assertThat(key).matches("verification/[0-9a-f-]{36}\\.pdf");
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().key()).isEqualTo(key);
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void jpeg_확장자도_jpg로_저장한다() {
        String key = fileStorage.upload("verification", file("학생증.JPEG", "image/jpeg", JPG), PDF_JPG, TEN_MB);

        assertThat(key).endsWith(".jpg");
    }

    @Test
    void 빈_파일은_FILE_EMPTY() {
        assertError(() -> fileStorage.upload("verification", file("a.pdf", "application/pdf", new byte[0]), PDF_JPG, TEN_MB),
            FileErrorCode.FILE_EMPTY);
        assertError(() -> fileStorage.upload("verification", null, PDF_JPG, TEN_MB), FileErrorCode.FILE_EMPTY);
    }

    @Test
    void 호출하는_쪽의_최대_크기를_넘으면_FILE_TOO_LARGE() {
        assertThatThrownBy(() -> fileStorage.upload("verification",
                file("a.pdf", "application/pdf", PDF), PDF_JPG, DataSize.ofBytes(PDF.length - 1)))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_TOO_LARGE));
        verifyNoInteractions(s3Client);
    }

    @Test
    void 허용하지_않은_형식이면_허용_형식을_알려준다() {
        assertThatThrownBy(() -> fileStorage.upload("verification", file("학생증.heic", "image/heic", JPG), PDF_JPG, TEN_MB))
            .isInstanceOfSatisfying(BusinessException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_TYPE_NOT_ALLOWED);
                assertThat(e.getMessage()).isEqualTo("pdf, jpg 파일만 올릴 수 있어요");
            });
        verifyNoInteractions(s3Client);
    }

    @Test
    void 허용_목록에_없는_형식은_확장자가_맞아도_거절한다() {
        assertError(() -> fileStorage.upload("verification", file("a.jpg", "image/jpeg", JPG), Set.of(FileType.PDF), TEN_MB),
            FileErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void 확장자만_바꾼_파일은_거절한다() {
        // 내용은 jpg인데 이름과 Content-Type만 pdf
        assertError(() -> fileStorage.upload("verification", file("위조.pdf", "application/pdf", JPG), PDF_JPG, TEN_MB),
            FileErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void Content_Type이_확장자와_다르면_거절한다() {
        assertError(() -> fileStorage.upload("verification", file("a.pdf", "text/plain", PDF), PDF_JPG, TEN_MB),
            FileErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void 확장자가_없으면_거절한다() {
        assertError(() -> fileStorage.upload("verification", file("document", "application/pdf", PDF), PDF_JPG, TEN_MB),
            FileErrorCode.FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    void S3_호출이_실패하면_FILE_UPLOAD_FAILED() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("Access Denied").statusCode(403).build());

        assertThatThrownBy(() -> fileStorage.upload("verification", file("a.pdf", "application/pdf", PDF), PDF_JPG, TEN_MB))
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void 삭제는_같은_버킷의_key로_요청한다() {
        fileStorage.delete("verification/abc.pdf");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(request.getValue().key()).isEqualTo("verification/abc.pdf");
    }

    @Test
    void presigned_URL은_key와_만료_시간을_담는다() {
        String url = fileStorage.presignedGetUrl("verification/abc.pdf", Duration.ofMinutes(10));

        assertThat(url)
            .contains("test-bucket")
            .contains("verification/abc.pdf")
            .contains("X-Amz-Expires=600");
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private void assertError(Runnable upload, FileErrorCode expected) {
        assertThatThrownBy(upload::run)
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
        verifyNoInteractions(s3Client);
    }
}
