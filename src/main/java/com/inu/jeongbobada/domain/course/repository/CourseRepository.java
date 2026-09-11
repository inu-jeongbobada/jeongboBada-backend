package com.inu.jeongbobada.domain.course.repository;

import com.inu.jeongbobada.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);
}
