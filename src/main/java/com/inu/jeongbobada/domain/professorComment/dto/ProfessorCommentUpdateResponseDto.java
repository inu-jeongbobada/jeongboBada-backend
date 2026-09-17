package com.inu.jeongbobada.domain.professorComment.dto;

import lombok.Getter;

@Getter
public class ProfessorCommentUpdateResponseDto {
    private final String message;

    public ProfessorCommentUpdateResponseDto(String message) {
        this.message = message;
    }
}
