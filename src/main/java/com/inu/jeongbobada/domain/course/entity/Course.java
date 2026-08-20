package com.inu.jeongbobada.domain.course.entity;



import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.material.entity.Material;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "COURSE")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "COURSE_ID")
    private Long courseId;

    @Column(name = "COURSE_NAME", nullable = false, length = 100)
    private String courseName;

    @Column(name = "COURSE_CODE" , nullable = false, unique = true, length = 20 )
    private String courseCode;

    @Column(name = "PROFESSOR_NAME", nullable = false, length = 20)
    private String professorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE",nullable = false)
    private Grade grade;

    @Column(name = "COURSE_TIME")
    private String courseTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEMESTER",nullable = false)
    private Semester semester;

    @Column(name = "COURSE_DETAIL",columnDefinition = "TEXT", nullable = false)
    private String courseDetail ;

    @Enumerated(EnumType.STRING)
    @Column(name = "CREDIT",nullable = false)
    private Credits credits;

    @Enumerated(EnumType.STRING)
    @Column(name = "COURSE_TYPE",nullable = false)
    private CourseType courseType;

    @Enumerated(EnumType.STRING)
    @Column(name ="IS_ONLINE",nullable = false)
    private IsOnline isOnline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESSOR_ID")
    private Professor professor;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<CourseReview> review;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Material> material;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVALUATION_TYPE",nullable = false)
    private EvaluationType evaluationType;




    public Course(
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
        this.courseName = courseName;
        this.professorName = professorName;
        this.courseDetail = courseDetail;
        this.grade = grade;
        this.semester = semester;
        this.credits = credits;
        this.courseCode = courseCode;
        this.courseTime = courseTime;
        this.courseType = courseType;
        this.professor = professor;
        this.evaluationType = evaluationType;
        this.isOnline = isOnline;

    }



}
