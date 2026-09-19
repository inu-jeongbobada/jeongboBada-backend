package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.PasswordResetRequest;
import com.inu.jeongbobada.domain.user.dto.PasswordResetSendCodeRequest;
import com.inu.jeongbobada.domain.user.dto.PasswordResetVerifyCodeRequest;
import com.inu.jeongbobada.domain.user.service.PasswordResetService;
import com.inu.jeongbobada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그인 전에 쓰는 API라 /api/auth/** (SecurityConfig의 permitAll)에 둔다.
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // 학번/이메일이 맞든 틀리든 항상 200 — 가입 여부를 알아낼 수 없게 한다.
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody PasswordResetSendCodeRequest request) {
        passwordResetService.sendCode(request.studentId(), request.email());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 코드가 맞는지만 확인 (코드는 소모되지 않음)
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody PasswordResetVerifyCodeRequest request) {
        passwordResetService.verifyCode(request.studentId(), request.code());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.studentId(), request.code(), request.newPassword());

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }
}
