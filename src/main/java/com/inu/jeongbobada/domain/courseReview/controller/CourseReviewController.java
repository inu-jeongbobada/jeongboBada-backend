package com.inu.jeongbobada.domain.courseReview.controller;

import com.inu.jeongbobada.domain.courseReview.dto.request.ReviewCreateReqDto;
import com.inu.jeongbobada.domain.courseReview.dto.response.ReviewResDto;
import com.inu.jeongbobada.domain.courseReview.enums.ReviewSort;
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
    // ApiResponse로 감싼 응답을 반환
    public ResponseEntity<ApiResponse<String>> createReview(
        @PathVariable Long courseId, //URL COURSEID 가져옴
        @RequestBody ReviewCreateReqDto request, //JSON을 req dto로 받음
        @AuthenticationPrincipal CustomUserDetails user
    ) {

        if (user == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }// 사용자 x -> 예외처리


        String message = courseReviewService.createReview(user.getUserId(), courseId, request);

        // 메서드 반환 타입에 맞춰 등록 결과를 공통 응답으로 감쌈
        return ResponseEntity.ok(ApiResponse.ok(message));


    }

    //과목 후기 조회

    @GetMapping("/{courseId}/reviews")

    public ResponseEntity<ApiResponse<List<ReviewResDto>>> getReviews(
        @AuthenticationPrincipal CustomUserDetails user,
        @PathVariable Long courseId,
        @RequestParam(defaultValue = "LATEST") ReviewSort sort
    ) {

        if (user == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }


        List<ReviewResDto> response = courseReviewService.getReviews(courseId, sort);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
