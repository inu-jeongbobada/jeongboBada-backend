package com.inu.jeongbobada.domain.material.dto.response;

import com.inu.jeongbobada.domain.material.enums.MaterialType;

import java.time.LocalDateTime;

public record MaterialDetailResDto(

    Long materialId,
    String title,
    String content,
    MaterialType materialType,
    String fileUrl,
    String originalFileName,
    Long fileSize,
    LocalDateTime createdAt,
    LocalDateTime updatedAt



) {
}
