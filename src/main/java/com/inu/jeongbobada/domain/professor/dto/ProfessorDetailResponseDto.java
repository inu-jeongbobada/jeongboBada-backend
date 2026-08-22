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

    private LabDetailResponseDto labDetail;
    private List<ProfessorCommentDetailResponseDto> professorCommentDetails;

    public ProfessorDetailResponseDto(String professorName, String professorImageUrl, String professorDetail, LabDetailResponseDto labDetail, List<ProfessorCommentDetailResponseDto> professorCommentDetails) {
        this.professorName = professorName;
        this.professorImageUrl = professorImageUrl;
        this.professorDetail = professorDetail;
        this.labDetail = labDetail;
        this.professorCommentDetails = professorCommentDetails;
    }
}
