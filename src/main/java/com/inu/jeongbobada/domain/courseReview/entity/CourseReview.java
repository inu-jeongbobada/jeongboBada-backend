package com.inu.jeongbobada.domain.courseReview.entity;

import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.courseReview.enums.*;
import com.inu.jeongbobada.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REVIEW_ID")
    private Long reviewId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COURSE_ID", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "RATING",nullable = false)
    private Rating rating;


    @Column(name = "CONTENT", columnDefinition = "TEXT")
    private String content;


    //서적 필요 여부
    @Enumerated(EnumType.STRING)
    @Column(name = "TEXT_BOOK", nullable = false)
    private TextBook textbook;


    //과제 어려운지
    @Enumerated(EnumType.STRING)
    @Column(name = "ASSIGNMENT_DIFFICULTY", nullable = false)
    private Difficulty assignmentDifficulty;


    //과제 양
    @Enumerated(EnumType.STRING)
    @Column(name = "ASSIGNMENT_AMOUNT", nullable = false)
    private Amount assignmentAmount;



  //많은지 안많은지
    @Enumerated(EnumType.STRING)
    @Column(name = "GROUP_ACTIVITY", nullable = false)
    private GroupActivity groupActivity;


    // 출결 변덕스러운지 안스러운지
    @Enumerated(EnumType.STRING)
    @Column(name = "ATTENDANCE",nullable = false)
    private Attendance attendance;

    // 시험 횟수
    @Enumerated(EnumType.STRING)
    @Column(name = "EXAM_COUNT", nullable = false)
    private Count examCount;

    //퀴즈 난이도
    @Enumerated(EnumType.STRING)
    @Column(name = "QUIZ_DIFFICULTY", nullable = false)
    private Difficulty quizDifficulty;


    //시험 난이도
    @Enumerated(EnumType.STRING)
    @Column(name = "EXAM_DIFFICULTY", nullable = false)
    private Difficulty examDifficulty;

    //쪽지시험 횟수
    @Enumerated(EnumType.STRING)
    @Column(name = "QUIZ_COUNT", nullable = false)
    private Count quizCount;

    // 널널하게 주는지 안주는지
    @Enumerated(EnumType.STRING)
    @Column(name = "GRADING_TYPE", nullable = false)
    private GradingType gradingType;


    public CourseReview(
        User user,
        Course course,
        Rating rating,
        String content,
        TextBook textbook,
        Difficulty assignmentDifficulty,
        Amount assignmentAmount,
        GroupActivity groupActivity,
        Attendance attendance,
        Count examCount,
        Difficulty quizDifficulty,
        Difficulty examDifficulty,
        Count quizCount,
        GradingType gradingType
    ) {
        this.user = user;
        this.course = course;
        this.rating = rating;
        this.content = content;
        this.textbook = textbook;
        this.assignmentDifficulty = assignmentDifficulty;
        this.assignmentAmount = assignmentAmount;
        this.groupActivity = groupActivity;
        this.attendance = attendance;
        this.examCount = examCount;
        this.quizDifficulty = quizDifficulty;
        this.examDifficulty = examDifficulty;
        this.quizCount = quizCount;
        this.gradingType = gradingType;
    }
}



