package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.LoginRequest;
import com.inu.jeongbobada.domain.user.dto.SignupRequest;
import com.inu.jeongbobada.domain.user.dto.TokenResponse;
import com.inu.jeongbobada.domain.user.service.AuthService;
import com.inu.jeongbobada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);

        ApiResponse<Void> response = ApiResponse.created(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);

        ApiResponse<TokenResponse> response = ApiResponse.ok(tokenResponse);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }
}
