package com.inu.jeongbobada.domain.lab.dto;

import lombok.Getter;

@Getter
public class LabDetailResponseDto {
    private Long labId;
    private String labName;
    private String labUrl;
    private String labDetail;

    public LabDetailResponseDto(Long labId, String labName, String labUrl, String labDetail) {
        this.labId = labId;
        this.labName = labName;
        this.labUrl = labUrl;
        this.labDetail = labDetail;
    }
}
