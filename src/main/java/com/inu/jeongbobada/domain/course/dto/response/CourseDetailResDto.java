package com.inu.jeongbobada.domain.course.dto.response;

import java.util.List;

public record CourseDetailResDto(
    Long courseId,
    String courseCode,
    String courseName,
    String courseDetail,
    List<CourseOfferingResDto> offerings
) {
}
