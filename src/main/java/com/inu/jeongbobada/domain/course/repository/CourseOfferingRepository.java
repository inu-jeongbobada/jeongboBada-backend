package com.inu.jeongbobada.domain.course.repository;

import com.inu.jeongbobada.domain.course.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    List<CourseOffering> findAllByActiveTrue();

    List<CourseOffering> findAllByCourse_CourseIdAndActiveTrue(Long courseId);

    Optional<CourseOffering> findByCourse_CourseIdAndProfessor_ProfessorId(Long courseId, Long professorId);
}
