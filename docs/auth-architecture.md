# 인증(JWT) 아키텍처

CLAUDE.md 개발 우선순위 1번(인증) 착수 전, 구조를 먼저 문서로 정리한다. 실제 구현은 이 문서를 기준으로 진행한다.

## 배경

- 로그인 식별자는 학번(`student_id`)
- 세션 대신 JWT 기반 무상태(stateless) 인증
- 현재는 `spring-boot-starter-security`만 의존성으로 추가돼 있고 커스텀 설정이 없어, 모든 요청이 Spring Security 기본 폼 로그인으로 막혀 있음 (Swagger UI 포함)

## 구성 요소

Spring Security 필터 체인 구조상 거의 정형화된 패턴을 따른다. 전부 `global/security` 패키지에 위치.

| 클래스 | 역할 |
|---|---|
| `SecurityConfig` | `SecurityFilterChain` 빈. 현재 `PERMIT_ALL_PATHS` 외 `anyRequest().permitAll()`로 전체 열어둔 상태 (커스텀 필터 미등록) |
| `JwtTokenProvider` | 토큰 발급/파싱/검증 (`jjwt` 사용). `createAccessToken`/`createRefreshToken`/`validateToken`/`getStudentId` 구현 완료 |
| `JwtAuthenticationFilter` | **미구현.** 매 요청의 `Authorization` 헤더에서 토큰 추출·검증 후 `SecurityContext`에 인증 정보 저장하는 역할 — 아직 없어서 access token이 실제 요청 인증엔 안 쓰이고 있음. `/reissue`, `/logout`에서 서비스가 토큰을 직접 파싱하는 임시 방식으로 우회 중 |
| `StudentUserDetailsService` | (당초 `CustomUserDetailsService`로 계획했으나 실제 클래스명은 이것) `student_id`로 DB에서 `User` 조회 후 `UserDetails`로 변환. 구현 완료 |
| `JwtAuthenticationEntryPoint` | **미구현.** 인증 실패 시 로그인 페이지 리다이렉트 대신 401 JSON 응답 |
| `AuthController` (user 도메인) | `/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout` 구현 완료 |

## 시크릿 키 관리

- DB 비밀번호와 동일한 패턴: `application.yml.example`에 `jwt.secret:` 빈 값으로 커밋, 각자 로컬 `application.yml`(gitignore 대상)에 실제 값 채움
- DB 비밀번호와 달리 팀원끼리 값이 같을 필요는 없음 — 각자 로컬 서버 안에서만 토큰 서명/검증에 쓰이는 값이라 인스턴스 내부 일관성만 있으면 됨

## 다른 도메인(교수/과목) 개발자 영향

- `SecurityConfig`에서 인증 불필요 경로(`GET /api/professors/**` 등)를 `permitAll`로 열어두면, 각 도메인 컨트롤러는 별도 처리 없이 그대로 사용 가능
- 인증이 필요한 경로만 필요 시 `@PreAuthorize` 또는 `SecurityConfig` 경로 매칭으로 제한

## 상태

### 1. 인증 기반 설정
- [x] `jjwt` 의존성 추가
- [x] JWT 설정 프로퍼티 추가
- [x] `SecurityConfig`
- [x] `PasswordEncoder`
- [x] Swagger 및 인증 API `permitAll`
- [x] 세션·폼 로그인·HTTP Basic 비활성화

### 2. 회원가입 및 로그인
- [x] `User`의 `studentId`, `password`, `userRole` 확인
- [x] `UserRepository.findByStudentId`
- [x] `SignupRequest`
- [x] `LoginRequest`
- [x] `TokenResponse`
- [x] `StudentUserDetailsService` (당초 계획명은 `CustomUserDetailsService`)
- [x] 회원가입 시 BCrypt 암호화
- [x] `AuthenticationManager` 기반 로그인

### 3. JWT 인증
- [x] `JwtProperties`
- [x] `JwtTokenProvider`
- [x] `AuthController`
- [ ] `JwtAuthenticationFilter` / `JwtAuthenticationEntryPoint` — 5번 섹션으로 이동
- [ ] `JwtAccessDeniedHandler` — 6번 섹션으로 이동
- [ ] 정상·누락·변조·만료 토큰 테스트 — 8번 섹션으로 이동

### 4. 후속 작업 (Refresh Token) — [이슈 #55](https://github.com/inu-jeongbobada/jeongboBada-backend/issues/55)
- [x] Refresh Token 저장 구조 — `User` 엔티티에 `refreshToken`/`refreshTokenExpiresAt` 컬럼 (별도 테이블/Redis 없음, 멀티 디바이스 요구사항 없어 오버엔지니어링으로 판단)
- [x] `/api/auth/reissue`
- [x] `/api/auth/logout`
- [ ] Refresh Token 해시 저장 및 회전 — **회전(로그인/재발급마다 신규 발급)은 완료**, **해시 저장은 미완료**(현재 DB에 평문 저장, `studentId`처럼 응답 DTO 노출 금지 대상으로만 취급 중)

### 5. 인증 필터 (1순위 — 과목 후기 도메인이 대기 중)
과목(course) 후기 컨트롤러에서 "로그인한 사용자만 접근 가능"이 필요해져서, 더는 미룰 수 없는 상태.
- [ ] 커스텀 `UserDetails`(예: `CustomUserDetails`) — `StudentUserDetailsService.loadUserByUsername()`이
      Spring 기본 `User`(username/password/authorities만 있음) 대신, `userId` 등 우리 도메인 정보를
      담은 객체를 반환하도록 교체
- [ ] `JwtAuthenticationFilter` — `OncePerRequestFilter`. 매 요청 `Authorization` 헤더에서 토큰
      추출·검증 후 `SecurityContext`에 위 커스텀 `UserDetails` 채움
- [ ] `JwtAuthenticationEntryPoint` — 인증 안 된 요청에 401 JSON 응답
- [ ] `SecurityConfig`에 필터 등록, 인증 필요한 경로를 `permitAll` → `authenticated()`로 전환
- [ ] 과목 후기 담당자에게 엔티티에 작성자 `user_id` 컬럼(FK) 먼저 넣어두라고 전달함 (필터 완성 전
      선작업, 나중에 스키마 변경 없이 `@AuthenticationPrincipal`만 끼워넣을 수 있도록)

### 6. 권한 관리 / 개인정보 수정 (기능 명세 반영, 2순위)
- [ ] `JwtAccessDeniedHandler` — 권한 부족 요청에 403 JSON 응답
- [ ] STUDENT/ADMIN 권한 구분 — `@PreAuthorize` 등으로 관리자 전용 API 제한 (5번 필터 완료 후 가능)
- [ ] 개인정보 수정 API — 닉네임/비밀번호 변경

### 7. 비밀번호 찾기 (학교 이메일 인증, 3순위)
PASS 본인인증은 소모임 프로젝트 규모에 비해 비용·행정 부담이 커서 채택하지 않음.
학번 기반 서비스 특성상 학교 이메일(`@inu.ac.kr`) 인증으로 대체하기로 결정.
- [ ] `User` 엔티티에 학교 이메일 컬럼 추가 (회원가입 시 같이 받을지, 나중에 등록할지 결정 필요)
- [ ] 이메일 발송 연동 (Spring Mail + SMTP)
- [ ] 인증코드 생성/저장(TTL 있는 임시 저장) + 검증 API
- [ ] 인증 성공 시 비밀번호 재설정 API

### 8. 남은 갭 (우선순위 낮음)
- [ ] Refresh Token DB 평문 저장 → 해시(예: SHA-256) 저장으로 전환 검토
- [ ] 정상·누락·변조·만료 토큰 자동 테스트 코드 (지금은 Postman 수동 확인만 함)

# 예상 브랜치
- feat/security-config
- feat/auth-login
- feat/jwt-authentication
- feat/refresh-token
- feat/jwt-authentication-filter
- feat/auth-authorization (권한 관리)
- feat/user-profile-update (개인정보 수정)
- feat/password-reset-email (비밀번호 찾기)
