package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.LoginRequest;
import com.inu.jeongbobada.domain.user.dto.NicknameCheckResponse;
import com.inu.jeongbobada.domain.user.dto.ReissueRequest;
import com.inu.jeongbobada.domain.user.dto.SignupRequest;
import com.inu.jeongbobada.domain.user.dto.TokenResponse;
import com.inu.jeongbobada.domain.user.service.AuthService;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 가입 폼에서 제출 전 실시간으로 사용 가능 여부를 확인하는 용도. 학번/이메일은 user enumeration 방지를 위해 제공하지 않음 (docs/auth-architecture.md 9번 참고)
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname(@RequestParam String nickname) {
        NicknameCheckResponse checkResponse = authService.checkNicknameAvailability(nickname);

        ApiResponse<NicknameCheckResponse> response = ApiResponse.ok(checkResponse);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);

        ApiResponse<TokenResponse> response = ApiResponse.ok(tokenResponse);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // Access Token 만료 시 클라이언트가 body에 refresh token을 담아 호출 -> 검증 통과하면 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse tokenResponse = authService.reissue(request);

        ApiResponse<TokenResponse> response = ApiResponse.ok(tokenResponse);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    // 아직 인증 필터(SecurityContext에 유저 채워주는 필터)가 없어서,
    // Authorization 헤더의 access token을 여기서 직접 파싱해 studentId를 얻는다.
    // 나중에 JWT 필터가 생기면 @AuthenticationPrincipal 등으로 대체할 것.
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        String accessToken = authorizationHeader.replaceFirst("^Bearer ", "");
        authService.logout(accessToken);

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

}
