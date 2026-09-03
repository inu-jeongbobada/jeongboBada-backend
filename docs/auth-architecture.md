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
- [ ] `JwtAuthenticationFilter` — 미구현. `SecurityConfig`가 전부 `permitAll`이라 access token이 실제 요청 인증에는 아직 안 쓰임
- [ ] `JwtAuthenticationEntryPoint` — 미구현
- [ ] `JwtAccessDeniedHandler` — 미구현
- [x] `AuthController`
- [ ] 정상·누락·변조·만료 토큰 테스트 — refresh token 쪽은 Postman으로 수동 확인 완료, access token 자동 테스트는 아직 없음

### 4. 후속 작업 (Refresh Token) — [이슈 #55](https://github.com/inu-jeongbobada/jeongboBada-backend/issues/55)
- [x] Refresh Token 저장 구조 — `User` 엔티티에 `refreshToken`/`refreshTokenExpiresAt` 컬럼 (별도 테이블/Redis 없음, 멀티 디바이스 요구사항 없어 오버엔지니어링으로 판단)
- [x] `/api/auth/reissue`
- [x] `/api/auth/logout`
- [ ] Refresh Token 해시 저장 및 회전 — **회전(로그인/재발급마다 신규 발급)은 완료**, **해시 저장은 미완료**(현재 DB에 평문 저장, `studentId`처럼 응답 DTO 노출 금지 대상으로만 취급 중)

### 5. 남은 인증 관련 갭
- [ ] `JwtAuthenticationFilter` 부재로 보호가 필요한 엔드포인트(마이페이지 즐겨찾기 등)가 아직 없음 — 다음 엔드포인트가 필요해지는 시점에 필터부터 구현해야 함
- [ ] Refresh Token DB 평문 저장 → 해시(예: SHA-256) 저장으로 전환 검토

# 예상 브랜치
- feat/security-config
- feat/auth-login
- feat/jwt-authentication
- feat/refresh-token
