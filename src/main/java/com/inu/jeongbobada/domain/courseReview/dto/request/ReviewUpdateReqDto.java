package com.inu.jeongbobada.domain.courseReview.dto.request;

import com.inu.jeongbobada.domain.courseReview.enums.Attendance;
import com.inu.jeongbobada.domain.courseReview.enums.ExamCount;
import com.inu.jeongbobada.domain.courseReview.enums.Frequency;
import com.inu.jeongbobada.domain.courseReview.enums.GradingType;
import com.inu.jeongbobada.domain.courseReview.enums.GroupActivity;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewUpdateReqDto(
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
) {
}
