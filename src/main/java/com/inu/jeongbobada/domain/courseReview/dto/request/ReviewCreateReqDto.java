package com.inu.jeongbobada.domain.courseReview.dto.request;

import com.inu.jeongbobada.domain.course.enums.Semester;
import com.inu.jeongbobada.domain.courseReview.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// 필수 여부·길이는 CourseReview 엔티티와 같게 둔다 (#105).
public record ReviewCreateReqDto(

    // 같은 과목이라도 담당 교수가 여러 명일 수 있어서, 어느 교수님 강의에 대한 후기인지 명시한다.
    @NotNull(message = "교수를 선택해주세요.")
    Long professorId,

    @NotNull(message = "수강 연도를 선택해주세요.")
    AcademicYear academicYear,

    @NotNull(message = "수강 학기를 선택해주세요.")
    Semester semester,

    @NotNull(message = "별점을 선택해주세요.")
    Rating rating,

    @NotBlank(message = "후기 내용을 입력해주세요.")
    @Size(min = 20, max = 1000, message = "후기 내용은 20자 이상 1000자 이하로 입력해주세요.")
    String content,

    @NotNull(message = "조모임 빈도를 선택해주세요.")
    GroupActivity groupActivity,

    @NotNull(message = "출결 방식을 선택해주세요.")
    Attendance attendance,

    @NotNull(message = "시험 횟수를 선택해주세요.")
    ExamCount examCount,

    @NotNull(message = "과제 빈도를 선택해주세요.")
    Frequency assignmentFrequency,

    @NotNull(message = "퀴즈 빈도를 선택해주세요.")
    Frequency quizFrequency,

    @NotNull(message = "학점 기준을 선택해주세요.")
    GradingType gradingType

){
}
