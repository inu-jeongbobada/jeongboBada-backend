package com.inu.jeongbobada.domain.professor.dto;

import com.inu.jeongbobada.domain.lab.dto.LabDetailResponseDto;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentDetailResponseDto;
import lombok.Getter;

import java.util.List;

@Getter
public class ProfessorDetailResponseDto {
    private String professorName;
    private String professorImageUrl;
    private String professorDetail;

    private LabDetailResponseDto labDetailResponseDto;
    private List<ProfessorCommentDetailResponseDto> professorCommentDetailResponseDtos;

    public ProfessorDetailResponseDto(String professorName, String professorImageUrl, String professorDetail, LabDetailResponseDto labDetailResponseDto, List<ProfessorCommentDetailResponseDto> professorCommentDetailResponseDtos) {
        this.professorName = professorName;
        this.professorImageUrl = professorImageUrl;
        this.professorDetail = professorDetail;
        this.labDetailResponseDto = labDetailResponseDto;
        this.professorCommentDetailResponseDtos = professorCommentDetailResponseDtos;
    }
}
