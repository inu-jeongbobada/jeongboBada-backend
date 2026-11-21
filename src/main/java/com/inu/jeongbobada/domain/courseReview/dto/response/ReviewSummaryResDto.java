package com.inu.jeongbobada.domain.courseReview.dto.response;

import com.inu.jeongbobada.domain.courseReview.enums.Attendance;
import com.inu.jeongbobada.domain.courseReview.enums.ExamCount;
import com.inu.jeongbobada.domain.courseReview.enums.Frequency;
import com.inu.jeongbobada.domain.courseReview.enums.GradingType;
import com.inu.jeongbobada.domain.courseReview.enums.GroupActivity;

public record ReviewSummaryResDto(
    long reviewCount,
    boolean summaryAvailable,
    int minimumReviewCount,
    Double averageRating,
    Double gradingTypeAverage,
    GradingType gradingType,
    Double groupActivityAverage,
    GroupActivity groupActivity,
    Double examCountAverage,
    ExamCount examCount,
    Double assignmentFrequencyAverage,
    Frequency assignmentFrequency,
    Double quizFrequencyAverage,
    Frequency quizFrequency,
    Attendance attendance
) {
}
