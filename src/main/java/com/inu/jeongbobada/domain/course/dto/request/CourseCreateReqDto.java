package com.inu.jeongbobada.domain.course.dto.request;

import com.inu.jeongbobada.domain.course.enums.*;
import com.inu.jeongbobada.domain.professor.entity.Professor;

public record CourseCreateReqDto(
    // 어드민 생성할시 사용
    String courseName,
    String professorName,
    String courseDetail,
    Grade grade,
    Semester semester,
    Credits credits ,
    String courseCode,
    String courseTime,
    CourseType courseType,
    Professor professor,
    EvaluationType evaluationType,
    IsOnline isOnline


) {


}
