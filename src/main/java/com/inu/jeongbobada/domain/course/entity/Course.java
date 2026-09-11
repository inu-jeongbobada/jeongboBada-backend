package com.inu.jeongbobada.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 학기가 바뀌어도 안 변하는 "과목 자체" 정보만 갖는다.
// 담당 교수/시간표/학점 등 학기마다 바뀌는 정보는 CourseOffering에서 관리한다.
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

    @Column(name = "COURSE_CODE", nullable = false, unique = true, length = 20)
    private String courseCode;

    @Column(name = "COURSE_DETAIL", columnDefinition = "TEXT", nullable = false)
    private String courseDetail;

    // 이번 학기 시간표에 없으면 false로 내려놓고, DELETE는 하지 않는다 (후기가 courseId를 참조하고 있어서).
    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    public Course(
            String courseName,
            String courseCode,
            String courseDetail
    ) {
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseDetail = courseDetail;
        this.active = true;
    }

    public void updateDetail(String courseName, String courseDetail) {
        this.courseName = courseName;
        this.courseDetail = courseDetail;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
