package com.inu.jeongbobada.domain.course.dto.response;

import com.inu.jeongbobada.domain.course.entity.CourseOffering;
import com.inu.jeongbobada.domain.course.enums.CourseType;
import com.inu.jeongbobada.domain.course.enums.Credits;
import com.inu.jeongbobada.domain.course.enums.Grade;
import com.inu.jeongbobada.domain.course.enums.Semester;

// 목록은 "이번 학기 개설된 강의" 기준으로 한 줄씩 보여준다 (CourseOffering 기준).
// courseId는 Course(과목 자체)의 id라서, 학기가 바뀌어도 상세/후기 조회에 계속 같은 값을 쓸 수 있다.
public record CourseListResDto(

    Long courseId,
    String courseName,
    String professorName,
    Grade grade,
    Semester semester,
    Credits credits,
    String courseCode,
    CourseType courseType

) {
    public static CourseListResDto from(CourseOffering offering) {
        return new CourseListResDto(
            offering.getCourse().getCourseId(),
            offering.getCourse().getCourseName(),
            offering.getProfessor().getProfessorName(),
            offering.getGrade(),
            offering.getSemester(),
            offering.getCredits(),
            offering.getCourse().getCourseCode(),
            offering.getCourseType()
        );
    }
}
