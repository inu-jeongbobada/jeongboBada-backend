package com.inu.jeongbobada.domain.professor.dto;

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
    private String professorRoom;
    private String professorCall;

//    private LabListResponseDto labList;

    public static ProfessorListResponseDto from(Professor professor) {
        return ProfessorListResponseDto.builder()
            .professorId(professor.getProfessorId())
            .professorName(professor.getProfessorName())
            .professorImageUrl(professor.getProfessorImageUrl())
            .professorRoom(professor.getProfessorRoom())
            .professorCall(professor.getProfessorCall())
//            .labList(professor.getLab() != null ? LabListResponseDto.from(professor.getLab()) : null)
            .build();
    }
}
