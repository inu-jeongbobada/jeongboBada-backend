package com.inu.jeongbobada.domain.course.dto.response;

import com.inu.jeongbobada.domain.professor.entity.Professor;

public record CourseProfessorResDto(
    Long professorId,
    String professorName,
    String professorImageUrl
) {
    public static CourseProfessorResDto from(Professor professor) {
        return new CourseProfessorResDto(
            professor.getProfessorId(),
            professor.getProfessorName(),
            professor.getProfessorImageUrl()
        );
    }
}
