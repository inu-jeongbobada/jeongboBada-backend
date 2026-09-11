package com.inu.jeongbobada.domain.courseReview.service;

import com.inu.jeongbobada.domain.course.exception.CourseException;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;
import com.inu.jeongbobada.domain.courseReview.enums.ReviewSort;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseReviewSortTest {
    private final CourseReviewRepository reviews = mock(CourseReviewRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final CourseReviewService service = new CourseReviewService(
        reviews, courses, mock(UserRepository.class));

    // 각 선택이 해당 조회만 실행하고, Repository 결과의 순서를 그대로 보존하는지 확인합니다.
    @ParameterizedTest
    @EnumSource(ReviewSort.class)
    void selectsOnlyRequestedSortAndPreservesOrder(ReviewSort sort) {
        when(courses.existsById(1L)).thenReturn(true);
        CourseReview first = mock(CourseReview.class);
        CourseReview second = mock(CourseReview.class);
        when(first.getReviewId()).thenReturn(8L);
        when(first.getRating()).thenReturn(Rating.FIVE);
        when(second.getReviewId()).thenReturn(3L);
        List<CourseReview> result = List.of(first, second);
        switch (sort) {
            case LATEST -> when(reviews.findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(1L)).thenReturn(result);
            case RECOMMENDED -> when(reviews.findAllByCourse_CourseIdOrderByLikeCountDescCreatedAtDescReviewIdDesc(1L)).thenReturn(result);
            case RATING -> when(reviews.findByCourseIdOrderByRating(1L)).thenReturn(result);
        }

        var response = service.getReviews(1L, sort);
        assertThat(response).extracting(dto -> dto.reviewId()).containsExactly(8L, 3L);
        assertThat(response.getFirst().rating()).isEqualTo(Rating.FIVE);
        switch (sort) {
            case LATEST -> verify(reviews).findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(1L);
            case RECOMMENDED -> verify(reviews).findAllByCourse_CourseIdOrderByLikeCountDescCreatedAtDescReviewIdDesc(1L);
            case RATING -> verify(reviews).findByCourseIdOrderByRating(1L);
        }
        verifyNoMoreInteractions(reviews);
    }

    @Test
    void missingCourseReturnsNotFound() {
        assertThatThrownBy(() -> service.getReviews(1L, ReviewSort.LATEST))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(CourseException.COURSE_NOT_FOUND));
        verifyNoInteractions(reviews);
    }

    @Test
    void courseWithoutReviewsReturnsEmptyList() {
        when(courses.existsById(1L)).thenReturn(true);
        when(reviews.findByCourseIdOrderByRating(1L)).thenReturn(List.of());
        assertThat(service.getReviews(1L, ReviewSort.RATING)).isEmpty();
    }
}
