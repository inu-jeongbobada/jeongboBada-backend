package com.inu.jeongbobada.domain.professorComment.dto;

import com.inu.jeongbobada.domain.professorComment.entity.ProfessorCommentAnonymity;
import lombok.Getter;

@Getter
public class ProfessorCommentCreateRequestDto {
    private int professorCommentRate;
    private String professorCommentDetail;
    private ProfessorCommentAnonymity professorCommentAnonymity;

    public ProfessorCommentCreateRequestDto(int professorCommentRate, String professorCommentDetail, ProfessorCommentAnonymity professorCommentAnonymity) {
        this.professorCommentRate = professorCommentRate;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
