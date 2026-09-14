package com.inu.jeongbobada.domain.courseReview.dto.request;
import com.inu.jeongbobada.domain.courseReview.enums.*;


public record ReviewCreateReqDto(

    // 같은 과목이라도 담당 교수가 여러 명일 수 있어서, 어느 교수님 강의에 대한 후기인지 명시한다.
    Long professorId,

    Rating rating,

    String content,

    TextBook textbook,

    Difficulty assignmentDifficulty,

    Amount assignmentAmount,

    GroupActivity groupActivity,

    Attendance attendance,

    Count examCount,

    Difficulty quizDifficulty,

    Difficulty examDifficulty,

    Count quizCount,

    GradingType gradingType

){
}
