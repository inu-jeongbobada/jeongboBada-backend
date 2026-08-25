package com.inu.jeongbobada.domain.courseReview.dto.response;

import com.inu.jeongbobada.domain.courseReview.enums.Rating;

public record ReviewResDto(

    Long reviewId,

    Rating rating,

    String content

){
}
