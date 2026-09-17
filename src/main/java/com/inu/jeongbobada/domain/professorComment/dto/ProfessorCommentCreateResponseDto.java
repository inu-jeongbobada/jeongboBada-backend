package com.inu.jeongbobada.domain.professorComment.dto;

import lombok.Getter;

@Getter
public class ProfessorCommentCreateResponseDto {
    private final String message;

    public ProfessorCommentCreateResponseDto(String message) {
        this.message = message;
    }
}
