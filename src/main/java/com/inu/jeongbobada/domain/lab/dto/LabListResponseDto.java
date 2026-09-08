package com.inu.jeongbobada.domain.lab.dto;

import com.inu.jeongbobada.domain.lab.entity.Lab;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LabListResponseDto {
    private Long labId;
    private String labName;
    private String labUrl;

    public static LabListResponseDto from(Lab lab) {
        return LabListResponseDto.builder()
            .labId(lab.getLabId())
            .labName(lab.getLabName())
            .labUrl(lab.getLabUrl())
            .build();
    }
}
