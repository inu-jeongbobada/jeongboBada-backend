package com.inu.jeongbobada.domain.professor.dto;

import com.inu.jeongbobada.domain.lab.dto.LabListResponseDto;
import lombok.Getter;

@Getter
public class ProfessorListResponseDto {
    private Long professorId;
    private String professorName;
    private String professorImageUrl;

    private LabListResponseDto labListResponseDto;

    public ProfessorListResponseDto(Long professorId, String professorName, String professorImageUrl, LabListResponseDto labListResponseDto) {
        this.professorId = professorId;
        this.professorName = professorName;
        this.professorImageUrl = professorImageUrl;
        this.labListResponseDto = labListResponseDto;
    }
}
