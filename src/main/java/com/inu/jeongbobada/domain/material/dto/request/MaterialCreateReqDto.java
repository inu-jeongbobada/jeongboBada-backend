package com.inu.jeongbobada.domain.material.dto.request;

import com.inu.jeongbobada.domain.material.enums.MaterialType;

public record MaterialCreateReqDto(


    String title,
    String content,
    MaterialType materialType





) {
}
