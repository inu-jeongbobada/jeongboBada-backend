package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.NicknameUpdateRequest;
import com.inu.jeongbobada.domain.user.dto.PasswordUpdateRequest;
import com.inu.jeongbobada.domain.user.security.CustomUserDetails;
import com.inu.jeongbobada.domain.user.service.UserService;
import com.inu.jeongbobada.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}
