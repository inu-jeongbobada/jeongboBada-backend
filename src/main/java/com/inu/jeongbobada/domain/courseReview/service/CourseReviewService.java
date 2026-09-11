package com.inu.jeongbobada.domain.courseReview.service;

import com.inu.jeongbobada.domain.courseReview.dto.request.ReviewCreateReqDto;
import com.inu.jeongbobada.domain.courseReview.dto.response.ReviewResDto;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.exception.CourseException;
import com.inu.jeongbobada.domain.course.repository.CourseOfferingRepository;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.ReviewSort;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.exception.ProfessorErrorCode;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
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
    private final CourseOfferingRepository courseOfferingRepository;
    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;

    @Transactional
    public String createReview(Long userId, Long courseId, ReviewCreateReqDto request) {
        //  Controller의 인증 정보에서 전달받은 작성자 ID로 사용자를 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        //  후기를 작성할 과목이 존재하는지 확인
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new BusinessException(CourseException.COURSE_NOT_FOUND));

        Professor professor = professorRepository.findById(request.professorId())
            .orElseThrow(() -> new BusinessException(ProfessorErrorCode.PROFESSOR_NOT_FOUND));

        // 이 교수가 실제로 이 과목을 개설한 적 있는지 확인 (엉뚱한 과목-교수 조합으로 후기가 달리는 것 방지)
        courseOfferingRepository.findByCourse_CourseIdAndProfessor_ProfessorId(courseId, professor.getProfessorId())
            .orElseThrow(() -> new BusinessException(CourseException.COURSE_OFFERING_NOT_FOUND));

        CourseReview review = new CourseReview(
            user,
            course,
            professor,
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
