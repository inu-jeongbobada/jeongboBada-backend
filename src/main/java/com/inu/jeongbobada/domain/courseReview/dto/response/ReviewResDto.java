package com.inu.jeongbobada.domain.courseReview.dto.response;

import com.inu.jeongbobada.domain.courseReview.enums.Rating;

import java.time.LocalDateTime;

public record ReviewResDto(

    Long reviewId,

    Rating rating,

    String content ,

    LocalDateTime updatedAt,

    LocalDateTime createdAt
){
}
