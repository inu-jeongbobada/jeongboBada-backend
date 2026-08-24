package com.inu.jeongbobada.domain.professorComment.dto;

import lombok.Getter;

@Getter
public class ProfessorCommentUpdateRequestDto {
    private Long userId;

    private String professorCommentDetail;
    private String professorCommentAnonymity;

    public ProfessorCommentUpdateRequestDto(Long userId, String professorCommentDetail, String professorCommentAnonymity) {
        this.userId = userId;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
