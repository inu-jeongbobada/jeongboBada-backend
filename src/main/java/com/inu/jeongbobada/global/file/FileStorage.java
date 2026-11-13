package com.inu.jeongbobada.global.file;

import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 공용 파일 저장소 (S3, 비공개 버킷). 허용 형식·최대 크기·저장 위치(prefix)는 호출하는 도메인이 정한다.
// DB에는 URL이 아니라 upload()가 돌려준 key를 저장하고, 열 때마다 presignedGetUrl()로 짧게 만료되는 URL을 만든다.
// S3는 DB 트랜잭션에 묶이지 않으니, 업로드 뒤 DB 저장이 실패하면 호출하는 쪽이 delete()로 지워야 한다.
@Slf4j
@Component
public class FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public FileStorage(S3Client s3Client, S3Presigner s3Presigner, AwsProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.s3().bucket();
    }

    // 검증에 걸리면 S3를 호출하지 않는다. key는 {prefix}/{UUID}.{확장자}이고,
    // 원본 파일명은 한글·특수문자·개인정보가 섞일 수 있어 key에 쓰지 않는다.
    public String upload(String prefix, MultipartFile file, Set<FileType> allowedTypes, DataSize maxSize) {
        FileType type = validate(file, allowedTypes, maxSize);
        String key = prefix + "/" + UUID.randomUUID() + "." + type.extension();

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(type.contentType())
            .build();
        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        } catch (SdkException | IOException e) {
            log.error("파일 업로드 실패: key={}", key, e);
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
        return key;
    }

    // 실패는 호출하는 쪽이 판단한다 (업로드 직후 정리라면 원래 에러를 살려야 하고, DB 삭제 뒤라면 로그만 남기면 된다)
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    // 만료 시간은 쓰는 곳마다 다르다 (관리자 서류 열람, 학습자료 다운로드 등)
    public String presignedGetUrl(String key, Duration expiresIn) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
            .signatureDuration(expiresIn)
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }

    private FileType validate(MultipartFile file, Set<FileType> allowedTypes, DataSize maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > maxSize.toBytes()) {
            throw new BusinessException(FileErrorCode.FILE_TOO_LARGE, maxSize.toMegabytes() + "MB 이하 파일만 올릴 수 있어요");
        }

        String extension = extensionOf(file.getOriginalFilename());
        FileType type = allowedTypes.stream()
            .filter(candidate -> candidate.hasExtension(extension))
            .findFirst()
            .orElseThrow(() -> typeNotAllowed(allowedTypes));
        if (!type.contentType().equals(file.getContentType()) || !type.matchesSignature(head(file, type.signatureLength()))) {
            throw typeNotAllowed(allowedTypes);
        }
        return type;
    }

    private static String extensionOf(String filename) {
        if (filename == null || filename.lastIndexOf('.') < 0) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] head(MultipartFile file, int length) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BusinessException typeNotAllowed(Set<FileType> allowedTypes) {
        String allowed = allowedTypes.stream()
            .sorted()
            .map(FileType::extension)
            .collect(Collectors.joining(", "));
        return new BusinessException(FileErrorCode.FILE_TYPE_NOT_ALLOWED, allowed + " 파일만 올릴 수 있어요");
    }
}
