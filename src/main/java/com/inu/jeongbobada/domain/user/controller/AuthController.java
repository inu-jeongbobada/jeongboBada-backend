package com.inu.jeongbobada.domain.user.controller;

import com.inu.jeongbobada.domain.user.dto.LoginRequest;
import com.inu.jeongbobada.domain.user.dto.LogoutRequest;
import com.inu.jeongbobada.domain.user.dto.NicknameCheckResponse;
import com.inu.jeongbobada.domain.user.dto.ReissueRequest;
import com.inu.jeongbobada.domain.user.dto.SignupRequest;
import com.inu.jeongbobada.domain.user.dto.TokenResponse;
import com.inu.jeongbobada.domain.user.service.AuthService;
import com.inu.jeongbobada.global.common.ApiResponse;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
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

    // 로그아웃: 서버에 저장된 refresh token을 지운다. 둘 중 하나로 사용자를 찾는다.
    //   1) body의 refreshToken (권장) — access token이 만료돼도 로그아웃된다 (#129)
    //   2) Authorization 헤더의 access token (예전 방식, 하위 호환) — access가 만료됐으면 아무것도 지우지 못한다
    // 둘 다 없으면 400. 찾지 못해도(이미 로그아웃·만료 등) 로그아웃은 항상 성공(200)으로 응답한다.
    // access token이 필수가 아니므로 Swagger 자물쇠는 달지 않는다.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody(required = false) LogoutRequest request
    ) {
        String refreshToken = request != null ? request.refreshToken() : null;
        String accessToken = authorizationHeader != null ? authorizationHeader.replaceFirst("^Bearer ", "") : null;
        if (isBlank(refreshToken) && isBlank(accessToken)) {
            ApiResponse<Void> error = ApiResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE,
                "로그아웃할 refreshToken(body) 또는 Authorization 헤더가 필요합니다");
            return ResponseEntity.status(error.httpStatus()).body(error);
        }
        authService.logout(refreshToken, accessToken);

        ApiResponse<Void> response = ApiResponse.ok(null);
        return ResponseEntity.status(response.httpStatus()).body(response);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
