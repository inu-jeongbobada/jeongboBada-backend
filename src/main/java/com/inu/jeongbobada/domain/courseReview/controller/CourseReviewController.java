package com.inu.jeongbobada.domain.courseReview.controller;

import com.inu.jeongbobada.domain.courseReview.dto.request.ReviewCreateReqDto;
import com.inu.jeongbobada.domain.courseReview.dto.response.ReviewResDto;
import com.inu.jeongbobada.domain.courseReview.service.CourseReviewService;
import com.inu.jeongbobada.domain.user.security.CustomUserDetails;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    //과목 후기 등록

    @PostMapping("/{courseId}/reviews")
    // 수정: 조회와 동일하게 ApiResponse로 감싼 응답을 반환합니다.
    public ResponseEntity<ApiResponse<String>> createReview(
        @PathVariable Long courseId, //URL COURSEID 가져옴
        @RequestBody ReviewCreateReqDto request, //JSON을 req dto로 받음
        @AuthenticationPrincipal CustomUserDetails user
    ) {

        if (user == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        // 수정: CustomUserDetails에 정의된 접근자는 userId()가 아닌 getUserId()입니다.
        // TODO: Service에 createReview(Long userId, Long courseId, ReviewCreateReqDto request) 구현이 필요합니다.
        String message = courseReviewService.createReview(user.getUserId(), courseId, request);

        // 수정: 메서드 반환 타입에 맞춰 등록 결과를 공통 응답으로 감쌉니다.
        return ResponseEntity.ok(ApiResponse.ok(message));


    }

    //과목 후기 조회

    @GetMapping("/{courseId}/reviews")
    // 수정: 실제 반환값인 ApiResponse<List<ReviewResDto>>와 선언 타입을 맞춥니다.
    public ResponseEntity<ApiResponse<List<ReviewResDto>>> getReviews(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long courseId
    ) {

        if (user == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        // TODO: Service에 getReviews(Long courseId) 구현이 필요합니다.
        List<ReviewResDto> response = courseReviewService.getReviews(courseId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
