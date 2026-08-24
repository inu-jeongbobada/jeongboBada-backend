package com.inu.jeongbobada.domain.material.dto.response;

import com.inu.jeongbobada.domain.material.enums.MaterialType;

import java.time.LocalDateTime;

public record MaterialListResDto(
    Long materialId,
    String title,
    MaterialType materialType,
    String originalFileName,
    Long fileSize,
    LocalDateTime createdAt
) {

}
