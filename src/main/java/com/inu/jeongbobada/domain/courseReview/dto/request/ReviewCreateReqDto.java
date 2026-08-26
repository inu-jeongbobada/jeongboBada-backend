package com.inu.jeongbobada.domain.courseReview.dto.request;
import com.inu.jeongbobada.domain.courseReview.enums.*;


public record ReviewCreateReqDto(

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
