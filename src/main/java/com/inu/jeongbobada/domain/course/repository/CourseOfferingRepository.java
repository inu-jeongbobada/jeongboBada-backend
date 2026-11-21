package com.inu.jeongbobada.domain.course.repository;

import com.inu.jeongbobada.domain.course.entity.CourseOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    @Query(
        value = """
            select offering
            from CourseOffering offering
            join fetch offering.course course
            join fetch offering.professor professor
            where offering.active = true
            order by collate(course.courseName as ucs_basic) asc,
                     collate(professor.professorName as ucs_basic) asc,
                     offering.courseOfferingId asc
            """,
        countQuery = """
            select count(offering)
            from CourseOffering offering
            where offering.active = true
            """
    )
    Page<CourseOffering> findAllActive(Pageable pageable);

    @Query(
        value = """
            select offering
            from CourseOffering offering
            join fetch offering.course course
            join fetch offering.professor professor
            where offering.active = true
              and (
                replace(course.courseName, ' ', '') like concat('%', :keyword, '%') escape '\\'
                or replace(professor.professorName, ' ', '') like concat('%', :keyword, '%') escape '\\'
              )
            order by collate(course.courseName as ucs_basic) asc,
                     collate(professor.professorName as ucs_basic) asc,
                     offering.courseOfferingId asc
            """,
        countQuery = """
            select count(offering)
            from CourseOffering offering
            join offering.course course
            join offering.professor professor
            where offering.active = true
              and (
                replace(course.courseName, ' ', '') like concat('%', :keyword, '%') escape '\\'
                or replace(professor.professorName, ' ', '') like concat('%', :keyword, '%') escape '\\'
              )
            """
    )
    Page<CourseOffering> searchActiveByKeyword(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        value = """
            select offering
            from CourseOffering offering
            join fetch offering.course course
            join fetch offering.professor professor
            where offering.active = true
              and (
                regexp_like(replace(course.courseName, ' ', ''), :initialPattern)
                or regexp_like(replace(professor.professorName, ' ', ''), :initialPattern)
              )
            order by collate(course.courseName as ucs_basic) asc,
                     collate(professor.professorName as ucs_basic) asc,
                     offering.courseOfferingId asc
            """,
        countQuery = """
            select count(offering)
            from CourseOffering offering
            join offering.course course
            join offering.professor professor
            where offering.active = true
              and (
                regexp_like(replace(course.courseName, ' ', ''), :initialPattern)
                or regexp_like(replace(professor.professorName, ' ', ''), :initialPattern)
              )
            """
    )
    Page<CourseOffering> searchActiveByInitialConsonant(
        @Param("initialPattern") String initialPattern,
        Pageable pageable
    );

    List<CourseOffering> findAllByCourse_CourseIdAndActiveTrue(Long courseId);

    boolean existsByCourse_CourseIdAndProfessor_ProfessorId(Long courseId, Long professorId);

}
