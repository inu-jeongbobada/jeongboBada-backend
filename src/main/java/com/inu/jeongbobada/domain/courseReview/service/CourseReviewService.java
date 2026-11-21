package com.inu.jeongbobada.domain.courseReview.service;

import com.inu.jeongbobada.domain.courseReview.dto.request.ReviewCreateReqDto;
import com.inu.jeongbobada.domain.courseReview.dto.request.ReviewUpdateReqDto;
import com.inu.jeongbobada.domain.courseReview.dto.response.ReviewResDto;
import com.inu.jeongbobada.domain.courseReview.dto.response.ReviewSummaryResDto;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.exception.CourseErrorCode;
import com.inu.jeongbobada.domain.course.repository.CourseOfferingRepository;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.Attendance;
import com.inu.jeongbobada.domain.courseReview.enums.ExamCount;
import com.inu.jeongbobada.domain.courseReview.enums.Frequency;
import com.inu.jeongbobada.domain.courseReview.enums.GradingType;
import com.inu.jeongbobada.domain.courseReview.enums.GroupActivity;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;
import com.inu.jeongbobada.domain.courseReview.enums.ReviewSort;
import com.inu.jeongbobada.domain.courseReview.exception.CourseReviewErrorCode;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.exception.ProfessorErrorCode;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.exception.UserErrorCode;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor // 생성자 자동생성
public class CourseReviewService {
    private static final int SUMMARY_MIN_REVIEW_COUNT = 3;
    // 출결 최빈값이 동률이면 화면에 표시되는 한글 이름의 가나다순으로 선택한다.
    private static final List<Attendance> ATTENDANCE_TIE_BREAK_ORDER = List.of(
        Attendance.ASSIGNMENT_OR_QUIZ, // 과제 및 퀴즈로 확인
        Attendance.MULTIPLE,           // 여러 방식 병행
        Attendance.ONLINE_RECORD,      // 온라인 수강 기록
        Attendance.ELECTRONIC,         // 전자출결
        Attendance.ROLL_CALL,          // 직접 호명
        Attendance.NONE                // 출결 확인 없음
    );

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
            .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        Professor professor = professorRepository.findById(request.professorId())
            .orElseThrow(() -> new BusinessException(ProfessorErrorCode.PROFESSOR_NOT_FOUND));

        if (!courseOfferingRepository.existsByCourse_CourseIdAndProfessor_ProfessorId(
            courseId,
            professor.getProfessorId()
        )) {
            throw new BusinessException(CourseErrorCode.COURSE_OFFERING_NOT_FOUND);
        }

        // 수강 시기는 후기 정보로만 저장한다. 같은 사람이 같은 과목·교수에 두 번 쓰는 것은 방지하고,
        // 확인과 저장 사이 동시 요청은
        // DB UNIQUE에 걸려 전역 핸들러가 409 DUPLICATE_RESOURCE로 응답한다 (#118)
        if (courseReviewRepository
            .existsByUser_UserIdAndCourse_CourseIdAndProfessor_ProfessorId(
                userId,
                courseId,
                professor.getProfessorId()
            )) {
            throw new BusinessException(CourseReviewErrorCode.DUPLICATE_COURSE_REVIEW);
        }

        CourseReview review = new CourseReview(
            user,
            course,
            professor,
            request.academicYear(),
            request.semester(),
            request.rating(),
            request.content(),
            request.groupActivity(),
            request.attendance(),
            request.examCount(),
            request.assignmentFrequency(),
            request.quizFrequency(),
            request.gradingType()
        );

        // 후기를 DB에 저장
        courseReviewRepository.save(review);
        return "과목 후기가 등록되었습니다.";
    }

    @Transactional
    public String deleteReview(Long userId, Long courseId, Long reviewId) {
        CourseReview review = courseReviewRepository
            .findByReviewIdAndCourse_CourseId(reviewId, courseId)
            .orElseThrow(() -> new BusinessException(CourseReviewErrorCode.COURSE_REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }

        courseReviewRepository.delete(review);
        return "과목 후기가 삭제되었습니다.";
    }

    @Transactional
    public String updateReview(Long userId, Long courseId, Long reviewId, ReviewUpdateReqDto request) {
        CourseReview review = courseReviewRepository
            .findByReviewIdAndCourse_CourseId(reviewId, courseId)
            .orElseThrow(() -> new BusinessException(CourseReviewErrorCode.COURSE_REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }

        review.update(
            request.rating(),
            request.content(),
            request.groupActivity(),
            request.attendance(),
            request.examCount(),
            request.assignmentFrequency(),
            request.quizFrequency(),
            request.gradingType()
        );

        return "과목 후기가 수정되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<ReviewResDto> getReviews(Long courseId, ReviewSort sort) {
        // 없는 과목은 404 예외처리  존재하지만 후기가 없는 과목은 빈 목록을 반환
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
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
                review.getAcademicYear(),
                review.getSemester(),
                review.getUpdatedAt(),
                review.getCreatedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResDto getReviewSummary(Long courseId, Long professorId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        if (!professorRepository.existsById(professorId)) {
            throw new BusinessException(ProfessorErrorCode.PROFESSOR_NOT_FOUND);
        }

        List<CourseReview> reviews = courseReviewRepository
            .findAllByCourse_CourseIdAndProfessor_ProfessorId(courseId, professorId);

        if (reviews.size() < SUMMARY_MIN_REVIEW_COUNT) {
            return unavailableSummary(reviews.size());
        }

        double ratingAverage = average(reviews, review -> ratingScore(review.getRating()));
        double gradingAverage = average(reviews, review -> gradingTypeScore(review.getGradingType()));
        double groupActivityAverage = average(reviews, review -> groupActivityScore(review.getGroupActivity()));
        double examCountAverage = average(reviews, review -> examCountScore(review.getExamCount()));
        double assignmentAverage = average(reviews, review -> frequencyScore(review.getAssignmentFrequency()));
        double quizAverage = average(reviews, review -> frequencyScore(review.getQuizFrequency()));

        return new ReviewSummaryResDto(
            reviews.size(),
            true,
            SUMMARY_MIN_REVIEW_COUNT,
            ratingAverage,
            gradingAverage,
            gradingTypeFromScore(gradingAverage),
            groupActivityAverage,
            groupActivityFromScore(groupActivityAverage),
            examCountAverage,
            examCountFromScore(examCountAverage),
            assignmentAverage,
            frequencyFromScore(assignmentAverage),
            quizAverage,
            frequencyFromScore(quizAverage),
            attendanceMode(reviews)
        );
    }

    private static ReviewSummaryResDto unavailableSummary(long reviewCount) {
        return new ReviewSummaryResDto(
            reviewCount,
            false,
            SUMMARY_MIN_REVIEW_COUNT,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private static double average(List<CourseReview> reviews, ToIntFunction<CourseReview> score) {
        double value = reviews.stream().mapToInt(score).average().orElse(0.0);
        return Math.round(value * 10.0) / 10.0;
    }

    private static int ratingScore(Rating value) {
        return switch (value) {
            case ONE -> 1;
            case TWO -> 2;
            case THREE -> 3;
            case FOUR -> 4;
            case FIVE -> 5;
        };
    }

    private static int gradingTypeScore(GradingType value) {
        return switch (value) {
            case VERY_STRICT -> 1;
            case STRICT -> 2;
            case NORMAL -> 3;
            case GENEROUS -> 4;
            case VERY_GENEROUS -> 5;
        };
    }

    private static GradingType gradingTypeFromScore(double value) {
        return switch ((int) Math.round(value)) {
            case 1 -> GradingType.VERY_STRICT;
            case 2 -> GradingType.STRICT;
            case 3 -> GradingType.NORMAL;
            case 4 -> GradingType.GENEROUS;
            default -> GradingType.VERY_GENEROUS;
        };
    }

    private static int groupActivityScore(GroupActivity value) {
        return switch (value) {
            case NONE -> 0;
            case SOMETIMES -> 1;
            case ALWAYS -> 2;
        };
    }

    private static GroupActivity groupActivityFromScore(double value) {
        return switch ((int) Math.round(value)) {
            case 0 -> GroupActivity.NONE;
            case 1 -> GroupActivity.SOMETIMES;
            default -> GroupActivity.ALWAYS;
        };
    }

    private static int examCountScore(ExamCount value) {
        return switch (value) {
            case ZERO -> 0;
            case ONE -> 1;
            case TWO -> 2;
            case THREE_OR_MORE -> 3;
        };
    }

    private static ExamCount examCountFromScore(double value) {
        return switch ((int) Math.round(value)) {
            case 0 -> ExamCount.ZERO;
            case 1 -> ExamCount.ONE;
            case 2 -> ExamCount.TWO;
            default -> ExamCount.THREE_OR_MORE;
        };
    }

    private static int frequencyScore(Frequency value) {
        return switch (value) {
            case NONE -> 0;
            case LOW -> 1;
            case NORMAL -> 2;
            case HIGH -> 3;
        };
    }

    private static Frequency frequencyFromScore(double value) {
        return switch ((int) Math.round(value)) {
            case 0 -> Frequency.NONE;
            case 1 -> Frequency.LOW;
            case 2 -> Frequency.NORMAL;
            default -> Frequency.HIGH;
        };
    }

    private static Attendance attendanceMode(List<CourseReview> reviews) {
        Map<Attendance, Long> counts = new EnumMap<>(Attendance.class);
        reviews.forEach(review -> counts.merge(review.getAttendance(), 1L, Long::sum));

        long highestCount = counts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<Attendance> mostSelected = counts.entrySet().stream()
            .filter(entry -> entry.getValue() == highestCount)
            .map(Map.Entry::getKey)
            .toList();

        return ATTENDANCE_TIE_BREAK_ORDER.stream()
            .filter(mostSelected::contains)
            .findFirst()
            .orElseThrow();
    }



}
