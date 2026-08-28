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
| `SecurityConfig` | `SecurityFilterChain` 빈. 경로별 `permitAll`/인증 필요 설정, 세션 정책 `STATELESS`, 커스텀 필터 등록 |
| `JwtTokenProvider` | 토큰 발급/파싱/검증. 시크릿 키·만료시간 관리 (`jjwt` 사용) |
| `JwtAuthenticationFilter` | `OncePerRequestFilter`. 매 요청의 `Authorization` 헤더에서 토큰 추출·검증 후 `SecurityContext`에 인증 정보 저장 |
| `CustomUserDetailsService` | `student_id`로 DB에서 `User` 조회 후 `UserDetails`로 변환 |
| `JwtAuthenticationEntryPoint` | 인증 실패 시 로그인 페이지 리다이렉트 대신 401 JSON 응답 |
| `AuthController` (user 도메인) | `/api/auth/signup`, `/api/auth/login` — 로그인 성공 시 토큰 발급 |

## 시크릿 키 관리

- DB 비밀번호와 동일한 패턴: `application.yml.example`에 `jwt.secret:` 빈 값으로 커밋, 각자 로컬 `application.yml`(gitignore 대상)에 실제 값 채움
- DB 비밀번호와 달리 팀원끼리 값이 같을 필요는 없음 — 각자 로컬 서버 안에서만 토큰 서명/검증에 쓰이는 값이라 인스턴스 내부 일관성만 있으면 됨

## 다른 도메인(교수/과목) 개발자 영향

- `SecurityConfig`에서 인증 불필요 경로(`GET /api/professors/**` 등)를 `permitAll`로 열어두면, 각 도메인 컨트롤러는 별도 처리 없이 그대로 사용 가능
- 인증이 필요한 경로만 필요 시 `@PreAuthorize` 또는 `SecurityConfig` 경로 매칭으로 제한

## 상태

### 1. 인증 기반 설정
- [ ] `jjwt` 의존성 추가
- [ ] JWT 설정 프로퍼티 추가
- [ ] `SecurityConfig`
- [ ] `PasswordEncoder`
- [ ] Swagger 및 인증 API `permitAll`
- [ ] 세션·폼 로그인·HTTP Basic 비활성화

### 2. 회원가입 및 로그인
- [ ] `User`의 `studentId`, `password`, `role` 확인
- [ ] `UserRepository.findByStudentId`
- [ ] `SignupRequest`
- [ ] `LoginRequest`
- [ ] `TokenResponse`
- [ ] `CustomUserDetailsService`
- [ ] 회원가입 시 BCrypt 암호화
- [ ] `AuthenticationManager` 기반 로그인

### 3. JWT 인증
- [ ] `JwtProperties`
- [ ] `JwtTokenProvider`
- [ ] `JwtAuthenticationFilter`
- [ ] `JwtAuthenticationEntryPoint`
- [ ] `JwtAccessDeniedHandler`
- [ ] `AuthController`
- [ ] 정상·누락·변조·만료 토큰 테스트

### 4. 후속 작업
- [ ] Refresh Token 저장 구조
- [ ] `/api/auth/reissue`
- [ ] `/api/auth/logout`
- [ ] Refresh Token 해시 저장 및 회전

# 예상 브랜치
- feat/security-config
- feat/auth-login
- feat/jwt-authentication
- feat/refresh-token
