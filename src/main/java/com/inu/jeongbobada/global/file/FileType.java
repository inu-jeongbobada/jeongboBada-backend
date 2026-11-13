package com.inu.jeongbobada.global.file;

import java.util.Arrays;
import java.util.Set;

// 업로드를 허용할 수 있는 형식. 호출하는 도메인이 이 중에서 허용 목록을 골라 넘긴다.
// signature는 파일 맨 앞 바이트로, 확장자만 바꾼 파일을 막는 데 쓴다.
public enum FileType {

    PDF("pdf", Set.of("pdf"), "application/pdf", new byte[]{'%', 'P', 'D', 'F'}),
    JPG("jpg", Set.of("jpg", "jpeg"), "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    // S3 key와 안내 문구에 쓰는 대표 확장자 (jpeg로 올려도 key는 .jpg)
    private final String extension;
    private final Set<String> extensions;
    private final String contentType;
    private final byte[] signature;

    FileType(String extension, Set<String> extensions, String contentType, byte[] signature) {
        this.extension = extension;
        this.extensions = extensions;
        this.contentType = contentType;
        this.signature = signature;
    }

    String extension() {
        return extension;
    }

    String contentType() {
        return contentType;
    }

    boolean hasExtension(String ext) {
        return extensions.contains(ext);
    }

    int signatureLength() {
        return signature.length;
    }

    boolean matchesSignature(byte[] head) {
        return Arrays.equals(head, signature);
    }
}
