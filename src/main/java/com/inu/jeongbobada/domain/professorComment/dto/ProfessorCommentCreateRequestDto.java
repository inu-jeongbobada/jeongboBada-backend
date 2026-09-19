package com.inu.jeongbobada.domain.professorComment.dto;

import com.inu.jeongbobada.domain.professorComment.entity.ProfessorCommentAnonymity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProfessorCommentCreateRequestDto {
    @NotNull
    private int professorCommentRate;
    @NotBlank
    private String professorCommentDetail;
    @NotNull
    private ProfessorCommentAnonymity professorCommentAnonymity;

    public ProfessorCommentCreateRequestDto(int professorCommentRate, String professorCommentDetail, ProfessorCommentAnonymity professorCommentAnonymity) {
        this.professorCommentRate = professorCommentRate;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
