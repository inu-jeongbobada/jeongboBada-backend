package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.EmailChangeSendCodeRequest;
import com.inu.jeongbobada.domain.user.dto.EmailUpdateRequest;
import com.inu.jeongbobada.domain.user.dto.NicknameUpdateRequest;
import com.inu.jeongbobada.domain.user.dto.PasswordUpdateRequest;
import com.inu.jeongbobada.domain.user.security.CustomUserDetails;
import com.inu.jeongbobada.domain.user.service.EmailChangeService;
import com.inu.jeongbobada.domain.user.service.UserService;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/users/me/** 는 SecurityConfig에서 authenticated() — Swagger에도 로그인 필요로 표시
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailChangeService emailChangeService;

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody NicknameUpdateRequest request
    ) {
        userService.updateNickname(userDetails.getUserId(), request.nickname());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody PasswordUpdateRequest request
    ) {
        userService.updatePassword(userDetails.getUserId(), request.currentPassword(), request.newPassword());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 이메일 변경 1단계: 새 이메일로 인증코드 발송
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailChangeCode(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody EmailChangeSendCodeRequest request
    ) {
        emailChangeService.sendCode(userDetails.getUserId(), request.newEmail());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 이메일 변경 2단계: 인증코드 + 현재 비밀번호를 확인하고 변경
    @PatchMapping("/email")
    public ResponseEntity<ApiResponse<Void>> updateEmail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody EmailUpdateRequest request
    ) {
        emailChangeService.updateEmail(
            userDetails.getUserId(), request.newEmail(), request.code(), request.currentPassword());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }
}
