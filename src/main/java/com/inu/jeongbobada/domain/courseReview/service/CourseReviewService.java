package com.inu.jeongbobada.domain.courseReview.service;

import com.inu.jeongbobada.domain.courseReview.dto.request.ReviewCreateReqDto;
import com.inu.jeongbobada.domain.courseReview.dto.response.ReviewResDto;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.exception.CourseException;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.ReviewSort;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor // 생성자 자동생성
public class CourseReviewService {
    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public String createReview(Long userId, Long courseId, ReviewCreateReqDto request) {
        //  Controller의 인증 정보에서 전달받은 작성자 ID로 사용자를 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        //  후기를 작성할 과목이 존재하는지 확인
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException(CourseException.COURSE_NOT_FOUND));


        CourseReview review = new CourseReview(
            user,
            course,
            request.rating(),
            request.content(),
            request.textbook(),
            request.assignmentDifficulty(),
            request.assignmentAmount(),
            request.groupActivity(),
            request.attendance(),
            request.examCount(),
            request.quizDifficulty(),
            request.examDifficulty(),
            request.quizCount(),
            request.gradingType()
        );

        // 후기를 DB에 저장
        courseReviewRepository.save(review);
        return "과목 후기가 등록되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<ReviewResDto> getReviews(Long courseId, ReviewSort sort) {
        // 없는 과목은 404 예외처리  존재하지만 후기가 없는 과목은 빈 목록을 반환
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(CourseException.COURSE_NOT_FOUND);
        }
        // 별점순 최신순 날짜순 으로 조회
        List<CourseReview> reviews = switch (sort) {
            case RECOMMENDED -> courseReviewRepository
                .findAllByCourse_CourseIdOrderByLikeCountDescCreatedAtDescReviewIdDesc(courseId);
            case RATING -> courseReviewRepository.findByCourseIdOrderByRating(courseId);
            case LATEST -> courseReviewRepository
                .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(courseId);
        };
        // map()은 각 데이터를 변환 toList()는 변환 결과를 리스트로 모음

        return reviews.stream()
            .map(review -> new ReviewResDto(
                review.getReviewId(),
                review.getRating(),
                review.getContent(),
                review.getUpdatedAt(),
                review.getCreatedAt()
            ))
            .toList();
    }
}
