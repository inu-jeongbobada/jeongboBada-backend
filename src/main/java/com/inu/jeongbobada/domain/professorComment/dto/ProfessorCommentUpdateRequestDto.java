package com.inu.jeongbobada.domain.professorComment.dto;

import com.inu.jeongbobada.domain.professorComment.entity.ProfessorCommentAnonymity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProfessorCommentUpdateRequestDto {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer professorCommentRate;
    @NotBlank
    private String professorCommentDetail;
    @NotNull
    private ProfessorCommentAnonymity professorCommentAnonymity;

    public ProfessorCommentUpdateRequestDto(Integer professorCommentRate, String professorCommentDetail, ProfessorCommentAnonymity professorCommentAnonymity) {
        this.professorCommentRate = professorCommentRate;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
