package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.PasswordResetRequest;
import com.inu.jeongbobada.domain.user.dto.PasswordResetSendCodeRequest;
import com.inu.jeongbobada.domain.user.dto.PasswordResetSendCodeResponse;
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

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<PasswordResetSendCodeResponse>> sendCode(@Valid @RequestBody PasswordResetSendCodeRequest request) {
        ApiResponse<PasswordResetSendCodeResponse> response =
            ApiResponse.ok(passwordResetService.sendCode(request.studentId(), request.email()));
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
