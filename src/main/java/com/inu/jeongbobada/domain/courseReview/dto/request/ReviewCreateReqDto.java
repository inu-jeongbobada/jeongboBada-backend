package com.inu.jeongbobada.domain.courseReview.dto.request;
import com.inu.jeongbobada.domain.courseReview.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// 필수 여부·길이는 CourseReview 엔티티(nullable = false, content 20~500자)와 같게 둔다 (#105).
// 어떤 항목을 필수로 둘지는 #106(시안↔API 정렬)에서 바뀔 수 있다 — 바꿀 때는 엔티티와 함께 바꾼다.
public record ReviewCreateReqDto(

    // 같은 과목이라도 담당 교수가 여러 명일 수 있어서, 어느 교수님 강의에 대한 후기인지 명시한다.
    @NotNull(message = "교수를 선택해주세요.")
    Long professorId,

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

){
}
