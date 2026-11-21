package com.inu.jeongbobada.domain.courseReview.dto.response;

import com.inu.jeongbobada.domain.course.enums.Semester;
import com.inu.jeongbobada.domain.courseReview.enums.AcademicYear;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;

import java.time.LocalDateTime;

public record ReviewResDto(

    Long reviewId,

    Rating rating,

    String content,

    AcademicYear academicYear,

    Semester semester,

    LocalDateTime updatedAt,

    LocalDateTime createdAt
){
}
