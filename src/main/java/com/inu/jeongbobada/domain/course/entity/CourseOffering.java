package com.inu.jeongbobada.domain.course.entity;

import com.inu.jeongbobada.domain.course.enums.*;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 특정 학년도/학기에 특정 교수가 개설한 과목 한 건.
// 학수번호 뒷자리(분반)는 학기마다 재배정돼서 안정적인 키가 될 수 없으므로 저장하지 않는다.
// 같은 (course, professor, academicYear, semester) 조합이면 매 학기 새로 넣지 않고 이 행을 갱신한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "COURSE_OFFERING",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_COURSE_OFFERING",
            columnNames = {"COURSE_ID", "PROFESSOR_ID", "ACADEMIC_YEAR", "SEMESTER"}
        )
    }
)
public class CourseOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COURSE_OFFERING_ID")
    private Long courseOfferingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COURSE_ID", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESSOR_ID", nullable = false)
    private Professor professor;

    @Column(name = "ACADEMIC_YEAR", nullable = false)
    private Integer academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEMESTER", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE", nullable = false)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "CREDITS", nullable = false)
    private Credits credits;

    @Column(name = "COURSE_TIME", nullable = false)
    private String courseTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "COURSE_TYPE", nullable = false)
    private CourseType courseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVALUATION_TYPE", nullable = false)
    private EvaluationType evaluationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "IS_ONLINE", nullable = false)
    private IsOnline isOnline;

    // 이번 학기 시간표에 다시 안 나오면 false로만 바꾸고, DELETE는 하지 않는다.
    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    public CourseOffering(
            Course course,
            Professor professor,
            Integer academicYear,
            Semester semester,
            Grade grade,
            Credits credits,
            String courseTime,
            CourseType courseType,
            EvaluationType evaluationType,
            IsOnline isOnline
    ) {
        this.course = course;
        this.professor = professor;
        this.academicYear = academicYear;
        this.semester = semester;
        this.grade = grade;
        this.credits = credits;
        this.courseTime = courseTime;
        this.courseType = courseType;
        this.evaluationType = evaluationType;
        this.isOnline = isOnline;
        this.active = true;
    }

    public void updateFromImport(
            Grade grade,
            Credits credits,
            String courseTime,
            CourseType courseType,
            EvaluationType evaluationType,
            IsOnline isOnline
    ) {
        this.grade = grade;
        this.credits = credits;
        this.courseTime = courseTime;
        this.courseType = courseType;
        this.evaluationType = evaluationType;
        this.isOnline = isOnline;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
