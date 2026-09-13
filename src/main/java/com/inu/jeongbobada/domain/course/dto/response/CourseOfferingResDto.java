package com.inu.jeongbobada.domain.course.dto.response;

import com.inu.jeongbobada.domain.course.entity.CourseOffering;
import com.inu.jeongbobada.domain.course.enums.*;

public record CourseOfferingResDto(
    Long courseOfferingId,
    CourseProfessorResDto professor,
    Integer academicYear,
    Semester semester,
    Grade grade,
    Credits credits,
    String courseTime,
    CourseType courseType,
    EvaluationType evaluationType,
    IsOnline isOnline
) {
    public static CourseOfferingResDto from(CourseOffering offering) {
        return new CourseOfferingResDto(
            offering.getCourseOfferingId(),
            CourseProfessorResDto.from(offering.getProfessor()),
            offering.getAcademicYear(),
            offering.getSemester(),
            offering.getGrade(),
            offering.getCredits(),
            offering.getCourseTime(),
            offering.getCourseType(),
            offering.getEvaluationType(),
            offering.getIsOnline()
        );
    }
}
