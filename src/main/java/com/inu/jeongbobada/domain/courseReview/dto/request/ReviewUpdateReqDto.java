package com.inu.jeongbobada.domain.courseReview.dto.request;

import com.inu.jeongbobada.domain.courseReview.enums.Amount;
import com.inu.jeongbobada.domain.courseReview.enums.Attendance;
import com.inu.jeongbobada.domain.courseReview.enums.Count;
import com.inu.jeongbobada.domain.courseReview.enums.Difficulty;
import com.inu.jeongbobada.domain.courseReview.enums.GradingType;
import com.inu.jeongbobada.domain.courseReview.enums.GroupActivity;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;
import com.inu.jeongbobada.domain.courseReview.enums.TextBook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewUpdateReqDto(
    @NotNull(message = "별점을 선택해주세요.")
    Rating rating,

    @NotBlank(message = "후기 내용을 입력해주세요.")
    @Size(min = 20, max = 500, message = "후기 내용은 20자 이상 500자 이하로 입력해주세요.")
    String content,

    @NotNull(message = "교재 필요 여부를 선택해주세요.")
    TextBook textbook,

    @NotNull(message = "과제 난이도를 선택해주세요.")
    Difficulty assignmentDifficulty,

    @NotNull(message = "과제 양을 선택해주세요.")
    Amount assignmentAmount,

    @NotNull(message = "조모임 빈도를 선택해주세요.")
    GroupActivity groupActivity,

    @NotNull(message = "출결 방식을 선택해주세요.")
    Attendance attendance,

    @NotNull(message = "시험 횟수를 선택해주세요.")
    Count examCount,

    @NotNull(message = "퀴즈 난이도를 선택해주세요.")
    Difficulty quizDifficulty,

    @NotNull(message = "시험 난이도를 선택해주세요.")
    Difficulty examDifficulty,

    @NotNull(message = "퀴즈 횟수를 선택해주세요.")
    Count quizCount,

    @NotNull(message = "학점 기준을 선택해주세요.")
    GradingType gradingType
) {
}
