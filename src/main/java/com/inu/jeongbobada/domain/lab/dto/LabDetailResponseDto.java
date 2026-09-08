package com.inu.jeongbobada.domain.lab.dto;

import com.inu.jeongbobada.domain.lab.entity.Lab;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LabDetailResponseDto {
    private Long labId;
    private String labName;
    private String labUrl;
    private String labDetail;

    public static LabDetailResponseDto from(Lab lab) {
        return LabDetailResponseDto.builder()
            .labId(lab.getLabId())
            .labName(lab.getLabName())
            .labUrl(lab.getLabUrl())
            .labDetail(lab.getLabDetail())
            .build();
    }
}
