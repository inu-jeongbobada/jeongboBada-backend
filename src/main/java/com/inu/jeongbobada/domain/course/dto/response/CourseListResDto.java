package com.inu.jeongbobada.domain.course.dto.response;

import com.inu.jeongbobada.domain.course.enums.CourseType;
import com.inu.jeongbobada.domain.course.enums.Credits;
import com.inu.jeongbobada.domain.course.enums.Grade;
import com.inu.jeongbobada.domain.course.enums.Semester;

public record CourseListResDto(
    String courseName,
    String professorName,
    Grade grade,
    Semester semester,
    Credits credits ,
    String courseCode,
    CourseType courseType

) {
}
