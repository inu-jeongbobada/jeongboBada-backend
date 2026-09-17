package com.inu.jeongbobada.domain.professorComment.dto;

import com.inu.jeongbobada.domain.professorComment.entity.ProfessorComment;
import com.inu.jeongbobada.domain.professorComment.entity.ProfessorCommentAnonymity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ProfessorCommentDetailResponseDto {
    private Long professorCommentId;
    private String professorCommentDetail;
    private int professorCommentRate;
    private LocalDateTime professorCommentDate;
    private ProfessorCommentAnonymity professorCommentAnonymity;

    public static ProfessorCommentDetailResponseDto from(ProfessorComment professorComment) {
        return ProfessorCommentDetailResponseDto.builder()
            .professorCommentId(professorComment.getProfessorCommentId())
            .professorCommentDetail(professorComment.getProfessorCommentDetail())
            .professorCommentRate(professorComment.getProfessorCommentRate())
            .professorCommentDate(professorComment.getProfessorCommentDate())
            .professorCommentAnonymity(professorComment.getProfessorCommentAnonymity())
            .build();
    }
}
