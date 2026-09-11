package com.inu.jeongbobada.domain.courseReview.repository;

import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    //  과목별 후기를 작성일 최신순으로 조회 작성일이 같으면 후기 ID 내림차순

    List<CourseReview> findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(Long courseId);

    //  추천 수가 같으면 작성일 후기 ID 내림차순으로 순서를 결정
    List<CourseReview> findAllByCourse_CourseIdOrderByLikeCountDescCreatedAtDescReviewIdDesc(Long courseId);

    // 추가: 문자열로 저장된 별점을 숫자로 대응시켜 5점부터 1점 순으로 조회합니다.
    @Query("""
        select r from CourseReview r
        where r.course.courseId = :courseId
        order by case r.rating
            when com.inu.jeongbobada.domain.courseReview.enums.Rating.FIVE then 5
            when com.inu.jeongbobada.domain.courseReview.enums.Rating.FOUR then 4
            when com.inu.jeongbobada.domain.courseReview.enums.Rating.THREE then 3
            when com.inu.jeongbobada.domain.courseReview.enums.Rating.TWO then 2
            when com.inu.jeongbobada.domain.courseReview.enums.Rating.ONE then 1
            else 0
        end desc, r.createdAt desc, r.reviewId desc
        """)
    List<CourseReview> findByCourseIdOrderByRating(@Param("courseId") Long courseId);
}
