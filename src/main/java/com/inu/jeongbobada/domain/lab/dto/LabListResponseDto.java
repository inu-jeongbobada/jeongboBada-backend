package com.inu.jeongbobada.domain.lab.dto;

import lombok.Getter;

@Getter
public class LabListResponseDto {
    private Long labId;
    private String labName;
    private String labUrl;

    public LabListResponseDto(Long labId, String labName, String labUrl) {
        this.labId = labId;
        this.labName = labName;
        this.labUrl = labUrl;
    }
}
