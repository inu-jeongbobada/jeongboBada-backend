package com.inu.jeongbobada.domain.course.dto.response;

import com.inu.jeongbobada.domain.course.enums.*;

public record CourseDetailResDto(

    Long courseId,
    String courseName,
    String professorName,
    String courseDetail,
    Grade grade,
    Semester semester,
    Credits credits ,
    String courseCode,
    String courseTime,
    CourseType courseType,
    CourseProfessorResDto professor,
    EvaluationType evaluationType,
    IsOnline isOnline
    // , ReviewSummaryResDto reviewSummarys

) {
}
