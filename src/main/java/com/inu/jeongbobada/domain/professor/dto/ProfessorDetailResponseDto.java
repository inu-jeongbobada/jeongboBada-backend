package com.inu.jeongbobada.domain.professor.dto;

import com.inu.jeongbobada.domain.lab.dto.LabDetailResponseDto;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentDetailResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProfessorDetailResponseDto {
    private String professorName;
    private String professorImageUrl;
    private String professorDetail;

    private LabDetailResponseDto labDetail;
    private List<ProfessorCommentDetailResponseDto> professorCommentDetails;

    public static ProfessorDetailResponseDto from(Professor professor) {
        return ProfessorDetailResponseDto.builder()
            .professorName(professor.getProfessorName())
            .professorImageUrl(professor.getProfessorImageUrl())
            .professorDetail(professor.getProfessorDetail())
            .labDetail(professor.getLab() != null ? LabDetailResponseDto.from(professor.getLab()) : null)
            .professorCommentDetails(
                professor.getProfessorComments().stream().map(ProfessorCommentDetailResponseDto::from).toList()
            )
            .build();
    }
}
