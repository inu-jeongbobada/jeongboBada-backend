package com.inu.jeongbobada.domain.professor.dto;

import com.inu.jeongbobada.domain.lab.dto.LabListResponseDto;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProfessorListResponseDto {
    private Long professorId;
    private String professorName;
    private String professorImageUrl;
    private LabListResponseDto labList;

    public static ProfessorListResponseDto from(Professor professor) {
        return ProfessorListResponseDto.builder()
            .professorId(professor.getProfessorId())
            .professorName(professor.getProfessorName())
            .professorImageUrl(professor.getProfessorImageUrl())
            .labList(professor.getLab() != null ? LabListResponseDto.from(professor.getLab()) : null)
            .build();
    }
}
