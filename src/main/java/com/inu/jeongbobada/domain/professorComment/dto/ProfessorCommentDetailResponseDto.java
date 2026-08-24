package com.inu.jeongbobada.domain.professorComment.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProfessorCommentDetailResponseDto {
    private Long professorCommentId;
    private String professorCommentDetail;
    private int professorCommentRate;
    private LocalDateTime professorCommentDate;
    private String professorCommentAnonymity;

    public ProfessorCommentDetailResponseDto(Long professorCommentId, String professorCommentDetail, int professorCommentRate, LocalDateTime professorCommentDate, String professorCommentAnonymity) {
        this.professorCommentId = professorCommentId;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentRate = professorCommentRate;
        this.professorCommentDate = professorCommentDate;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
