package com.inu.jeongbobada.domain.courseReview.service;

import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.course.repository.CourseOfferingRepository;
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.Attendance;
import com.inu.jeongbobada.domain.courseReview.enums.ExamCount;
import com.inu.jeongbobada.domain.courseReview.enums.Frequency;
import com.inu.jeongbobada.domain.courseReview.enums.GradingType;
import com.inu.jeongbobada.domain.courseReview.enums.GroupActivity;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseReviewSummaryTest {

    private final CourseReviewRepository reviews = mock(CourseReviewRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final ProfessorRepository professors = mock(ProfessorRepository.class);
    private final CourseReviewService service = new CourseReviewService(
        reviews, courses, mock(CourseOfferingRepository.class), professors, mock(UserRepository.class));

    @Test
    void 후기_3개_미만이면_요약을_노출하지_않는다() {
        stubExistingCourseAndProfessor();
        when(reviews.findAllByCourse_CourseIdAndProfessor_ProfessorId(1L, 2L))
            .thenReturn(List.of(mock(CourseReview.class), mock(CourseReview.class)));

        var result = service.getReviewSummary(1L, 2L);

        assertThat(result.reviewCount()).isEqualTo(2);
        assertThat(result.summaryAvailable()).isFalse();
        assertThat(result.minimumReviewCount()).isEqualTo(3);
        assertThat(result.averageRating()).isNull();
        assertThat(result.attendance()).isNull();
    }

    @Test
    void 평균과_출결_최빈값을_계산한다() {
        stubExistingCourseAndProfessor();
        List<CourseReview> reviewList = List.of(
            review(Rating.FIVE, GradingType.VERY_GENEROUS, GroupActivity.NONE,
                ExamCount.ZERO, Frequency.NONE, Frequency.LOW, Attendance.ELECTRONIC),
            review(Rating.FOUR, GradingType.GENEROUS, GroupActivity.SOMETIMES,
                ExamCount.TWO, Frequency.NORMAL, Frequency.NORMAL, Attendance.ELECTRONIC),
            review(Rating.THREE, GradingType.NORMAL, GroupActivity.ALWAYS,
                ExamCount.THREE_OR_MORE, Frequency.HIGH, Frequency.HIGH, Attendance.ROLL_CALL)
        );
        when(reviews.findAllByCourse_CourseIdAndProfessor_ProfessorId(1L, 2L)).thenReturn(reviewList);

        var result = service.getReviewSummary(1L, 2L);

        assertThat(result.reviewCount()).isEqualTo(3);
        assertThat(result.summaryAvailable()).isTrue();
        assertThat(result.averageRating()).isEqualTo(4.0);
        assertThat(result.gradingTypeAverage()).isEqualTo(4.0);
        assertThat(result.gradingType()).isEqualTo(GradingType.GENEROUS);
        assertThat(result.groupActivityAverage()).isEqualTo(1.0);
        assertThat(result.groupActivity()).isEqualTo(GroupActivity.SOMETIMES);
        assertThat(result.examCountAverage()).isEqualTo(1.7);
        assertThat(result.examCount()).isEqualTo(ExamCount.TWO);
        assertThat(result.assignmentFrequencyAverage()).isEqualTo(1.7);
        assertThat(result.assignmentFrequency()).isEqualTo(Frequency.NORMAL);
        assertThat(result.quizFrequencyAverage()).isEqualTo(2.0);
        assertThat(result.quizFrequency()).isEqualTo(Frequency.NORMAL);
        assertThat(result.attendance()).isEqualTo(Attendance.ELECTRONIC);
    }

    @Test
    void 출결_최빈값이_동률이면_한글_이름의_가나다순으로_반환한다() {
        stubExistingCourseAndProfessor();
        List<CourseReview> reviewList = List.of(
            defaultReview(Attendance.ELECTRONIC),
            defaultReview(Attendance.ELECTRONIC),
            defaultReview(Attendance.ROLL_CALL),
            defaultReview(Attendance.ROLL_CALL)
        );
        when(reviews.findAllByCourse_CourseIdAndProfessor_ProfessorId(1L, 2L)).thenReturn(reviewList);

        assertThat(service.getReviewSummary(1L, 2L).attendance())
            .isEqualTo(Attendance.ELECTRONIC);
    }

    private void stubExistingCourseAndProfessor() {
        when(courses.existsById(1L)).thenReturn(true);
        when(professors.existsById(2L)).thenReturn(true);
    }

    private static CourseReview defaultReview(Attendance attendance) {
        return review(Rating.FOUR, GradingType.NORMAL, GroupActivity.SOMETIMES,
            ExamCount.TWO, Frequency.NORMAL, Frequency.NORMAL, attendance);
    }

    private static CourseReview review(
        Rating rating,
        GradingType gradingType,
        GroupActivity groupActivity,
        ExamCount examCount,
        Frequency assignmentFrequency,
        Frequency quizFrequency,
        Attendance attendance
    ) {
        CourseReview review = mock(CourseReview.class);
        when(review.getRating()).thenReturn(rating);
        when(review.getGradingType()).thenReturn(gradingType);
        when(review.getGroupActivity()).thenReturn(groupActivity);
        when(review.getExamCount()).thenReturn(examCount);
        when(review.getAssignmentFrequency()).thenReturn(assignmentFrequency);
        when(review.getQuizFrequency()).thenReturn(quizFrequency);
        when(review.getAttendance()).thenReturn(attendance);
        return review;
    }
}
