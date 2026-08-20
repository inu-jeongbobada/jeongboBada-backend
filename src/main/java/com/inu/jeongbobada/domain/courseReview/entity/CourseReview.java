package com.inu.jeongbobada.domain.courseReview.entity;

import com.inu.jeongbobada.domain.course.entity.Course;
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


    @Column(nullable = false)
    private Double rating;


    @Column(columnDefinition = "TEXT")
    private String content;

   //나중에 수정
    /*

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
    @Column(name = "GROUP_ACTIVITY", nullable = false)
    private Boolean groupActivity;

    // 출결 변덕스러운지 안스러운지
    @Column(nullable = false)
    private Attedance attendance;

    // 시험 횟수
    @Column(name = "EXAM_COUNT", nullable = false)
    private Examcount examCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXAM_DIFFICULTY", nullable = false)
    private Difficulty quizDifficulty;


    @Enumerated(EnumType.STRING)
    @Column(name = "EXAM_DIFFICULTY", nullable = false)
    private Difficulty examDifficulty;

    //쪽지시험
    @Column(name = "quiz", nullable = false)
    private Examcount quiz;

    // 널널하게 주는지 안주는지
    @Enumerated(EnumType.STRING)
    @Column(name = "GRADING_TYPE", nullable = false)
    private GradingType gradingType;
*/

}
