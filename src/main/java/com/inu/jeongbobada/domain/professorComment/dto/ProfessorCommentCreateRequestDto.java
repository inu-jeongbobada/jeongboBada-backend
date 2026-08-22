package com.inu.jeongbobada.domain.professorComment.dto;

import lombok.Getter;

@Getter
public class ProfessorCommentCreateRequestDto {
    private Long userId;

    private String professorCommentDetail;
    private String professorCommentAnonymity;

    public ProfessorCommentCreateRequestDto(Long userId, String professorCommentDetail, String professorCommentAnonymity) {
        this.userId = userId;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
