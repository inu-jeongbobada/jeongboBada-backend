package com.inu.jeongbobada.domain.courseReview.entity;

import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.enums.Semester;
import com.inu.jeongbobada.domain.courseReview.enums.*;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "COURSE_REVIEW",
    uniqueConstraints = {
        @UniqueConstraint(
            // 같은 과목이라도 담당 교수가 다르면 별개 강의로 보고 후기를 따로 남길 수 있게 한다.
            name = "UK_USER_COURSE_PROFESSOR_REVIEW",
            columnNames = {"USER_ID", "COURSE_ID", "PROFESSOR_ID"}
        )
    }
)



public class CourseReview extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REVIEW_ID")
    private Long reviewId;

    @Column(name = "LIKE_COUNT", nullable = false)
    private long likeCount = 0L;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COURSE_ID", nullable = false)
    private Course course;

    // 같은 과목이어도 담당 교수마다 강의 스타일이 달라서 후기를 교수 단위로 연결한다.
    // CourseOffering이 아니라 Professor를 직접 참조하는 이유: 학기별 CourseOffering 행이
    // 갈아끼워져도(=매 학기 재import) 후기가 courseId+professorId로 계속 살아있게 하기 위함.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESSOR_ID", nullable = false)
    private Professor professor;

    // 작성자가 이 과목을 수강한 연도와 학기를 후기 정보로 보존한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "ACADEMIC_YEAR", nullable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEMESTER", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "RATING",nullable = false)
    private Rating rating;

    @NotBlank
    @Size(min = 20, max = 1000)
    @Column(name = "CONTENT", length = 1000, nullable = false)
    private String content;

    // 조모임 빈도
    @Enumerated(EnumType.STRING)
    @Column(name = "GROUP_ACTIVITY", nullable = false)
    private GroupActivity groupActivity;

    // 출결 방식
    @Enumerated(EnumType.STRING)
    @Column(name = "ATTENDANCE",nullable = false)
    private Attendance attendance;

    // 시험 횟수
    @Enumerated(EnumType.STRING)
    @Column(name = "EXAM_COUNT", nullable = false)
    private ExamCount examCount;

    // 과제 빈도
    @Enumerated(EnumType.STRING)
    @Column(name = "ASSIGNMENT_FREQUENCY", nullable = false)
    private Frequency assignmentFrequency;

    // 퀴즈 빈도
    @Enumerated(EnumType.STRING)
    @Column(name = "QUIZ_FREQUENCY", nullable = false)
    private Frequency quizFrequency;

    // 널널하게 주는지 안주는지
    @Enumerated(EnumType.STRING)
    @Column(name = "GRADING_TYPE", nullable = false)
    private GradingType gradingType;


    public CourseReview(
        User user,
        Course course,
        Professor professor,
        AcademicYear academicYear,
        Semester semester,
        Rating rating,
        String content,
        GroupActivity groupActivity,
        Attendance attendance,
        ExamCount examCount,
        Frequency assignmentFrequency,
        Frequency quizFrequency,
        GradingType gradingType
    ) {
        this.user = user;
        this.course = course;
        this.professor = professor;
        this.academicYear = academicYear;
        this.semester = semester;
        this.rating = rating;
        this.content = content;
        this.groupActivity = groupActivity;
        this.attendance = attendance;
        this.examCount = examCount;
        this.assignmentFrequency = assignmentFrequency;
        this.quizFrequency = quizFrequency;
        this.gradingType = gradingType;
    }

    public void update(
        Rating rating,
        String content,
        GroupActivity groupActivity,
        Attendance attendance,
        ExamCount examCount,
        Frequency assignmentFrequency,
        Frequency quizFrequency,
        GradingType gradingType
    ) {
        this.rating = rating;
        this.content = content;
        this.groupActivity = groupActivity;
        this.attendance = attendance;
        this.examCount = examCount;
        this.assignmentFrequency = assignmentFrequency;
        this.quizFrequency = quizFrequency;
        this.gradingType = gradingType;
    }
}
