package com.inu.jeongbobada.domain.course.controller;

import com.inu.jeongbobada.domain.course.dto.response.CourseDetailResDto;
import com.inu.jeongbobada.domain.course.dto.response.CourseListResDto;

import com.inu.jeongbobada.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {


    private final CourseService courseService;


    // 과목 목록 조회
    @GetMapping
    public List<CourseListResDto> searchCourseList() {
        return courseService.getCourses();
    }


    // 과목 상세 조회
    @GetMapping("/{courseId}")
    public CourseDetailResDto searchCourse(@PathVariable Long courseId) {
        return courseService.getCourse(courseId);
    }
}
