package com.inu.jeongbobada.domain.professor.dto;

import com.inu.jeongbobada.domain.course.dto.response.CourseListResDto;
import com.inu.jeongbobada.domain.lab.dto.LabDetailResponseDto;
import com.inu.jeongbobada.domain.professor.entity.Professor;
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
    private String professorRoom;
    private String professorCall;
    private String professorEmail;
    private String professorDetail;

    private LabDetailResponseDto labDetail;
    private List<CourseListResDto> courseList;
//    private List<ProfessorCommentDetailResponseDto> professorCommentDetails;

    public static ProfessorDetailResponseDto from(Professor professor) {
        return ProfessorDetailResponseDto.builder()
            .professorName(professor.getProfessorName())
            .professorImageUrl(professor.getProfessorImageUrl())
            .professorDetail(professor.getProfessorDetail())
            .professorRoom(professor.getProfessorRoom())
            .professorCall(professor.getProfessorCall())
            .professorEmail(professor.getProfessorEmail())
            .labDetail(professor.getLab() != null ? LabDetailResponseDto.from(professor.getLab()) : null)
            .courseList(
                professor.getCourseOfferings().stream().map(CourseListResDto::from).toList()
            )
//            .professorCommentDetails(
//                professor.getProfessorComments().stream().map(ProfessorCommentDetailResponseDto::from).toList()
//            )
            .build();
    }
}
