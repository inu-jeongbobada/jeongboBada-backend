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
| `SecurityConfig` | `SecurityFilterChain` 빈. `JwtAuthenticationFilter`/`JwtAuthenticationEntryPoint` 등록 완료. 단, `PERMIT_ALL_PATHS` 외엔 아직 `anyRequest().permitAll()`이라 실제로 `authenticated()`로 막힌 경로는 없음 (아래 "실제 경로 제한은 누가 하나" 참고) |
| `JwtTokenProvider` | 토큰 발급/파싱/검증 (`jjwt` 사용). `createAccessToken`/`createRefreshToken`/`validateToken`/`getStudentId` 구현 완료 |
| `JwtAuthenticationFilter` | 구현 완료. 매 요청의 `Authorization` 헤더에서 토큰 추출·검증 후 `SecurityContext`에 `CustomUserDetails` 저장 |
| `CustomUserDetails` | 구현 완료. `UserDetails` 구현체, `userId`/`nickname` 등 도메인 정보 보유 |
| `StudentUserDetailsService` | (당초 `CustomUserDetailsService`로 계획했으나 실제 클래스명은 이것) `student_id`로 DB에서 `User` 조회 후 `CustomUserDetails`로 변환. 구현 완료 |
| `JwtAuthenticationEntryPoint` | 구현 완료. 인증 실패 시 로그인 페이지 리다이렉트 대신 401 JSON 응답 (`GlobalErrorCode.UNAUTHORIZED`) |
| `AuthController` (user 도메인) | `/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout` 구현 완료 |

## API 동작 규칙 (헷갈리기 쉬운 것 정리)

나중에 "이거 어떻게 동작하기로 했더라?" 싶을 때 코드 안 뒤지고 여기부터 볼 것.

| API | 로그인 필요? | 규칙 |
|---|---|---|
| `POST /api/auth/signup` | X | 학번/닉네임/이메일 중복 검사 후 BCrypt로 암호화해 저장. **이메일 필수**(비밀번호 찾기용, 대소문자·공백 무시하고 정규화해 저장) |
| `POST /api/auth/login` | X | 학번+비밀번호를 `AuthenticationManager`에 위임해 검증. **학번이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않고 둘 다 동일하게 401(`USER_401`)** — user enumeration(가입 여부 유추) 방지 목적, `GlobalExceptionHandler.handleBadCredentialsException` 참고 |
| `POST /api/auth/reissue` | X (refresh token 자체가 인증 수단) | refresh token 서명·만료 검증 + **DB에 저장된 값과 문자열 일치**해야 통과 (탈취된 구 토큰 재사용 방지). 통과 시 access/refresh 둘 다 새로 발급(회전) |
| `POST /api/auth/logout` | O (access token) | DB에 저장된 refresh token을 삭제만 함 — access token 자체를 서버가 강제로 만료시키는 건 아니라서, 이미 발급된 access token은 만료 시각까지는 계속 유효 |
| `PATCH /api/users/me/nickname` | O | 닉네임 `unique` 제약 때문에 중복 검사하되, **본인 소유 닉네임이면 중복 에러 안 냄**(자기 자신으로의 "변경"은 통과) |
| `PATCH /api/users/me/password` | O | **현재 비밀번호(`currentPassword`) 확인 필수** — 세션(access token) 탈취 상태에서 공격자가 비밀번호만 바꿔버리는 것 방지. 성공 시 **기존 refresh token 무효화**(`clearRefreshToken()`) → 다른 기기/세션은 재로그인 필요 |
| `POST /api/auth/password-reset/send-code` | X | 학번+이메일이 **가입 때 등록한 것과 일치할 때만** 6자리 코드를 메일로 발송. 학번 없음/이메일 불일치/쿨다운 중/**메일 발송 실패**도 전부 동일하게 200 — user enumeration 방지(로그인 API와 같은 원칙), 원인은 서버 로그로만 남김 |
| `POST /api/auth/password-reset/verify-code` | X | 코드가 맞는지만 확인하고 **코드는 소모하지 않음**(화면 단계 이동 판단용). 없음/만료/불일치/없는 학번 모두 동일하게 400(`USER_400`) |
| `POST /api/auth/password-reset` | X | 학번+코드+새 비밀번호. **서버는 verify-code 통과를 기억하지 않으므로 코드를 다시 검증**하고, 성공하면 코드를 폐기(1회용). 성공 시 `clearRefreshToken()`으로 기존 로그인 무효화 |
| `POST /api/users/me/email/send-code` | O | **새 이메일**로 인증코드 발송. 현재 이메일과 동일 400 / 이미 사용 중 409 / 쿨다운 중 429 / 발송 실패 503 (로그인한 본인 대상이라 사유를 그대로 알려줌) |
| `PATCH /api/users/me/email` | O | `newEmail`+`code`+`currentPassword`. **현재 비밀번호 확인 필수**, 코드를 받은 이메일과 다른 주소로는 변경 불가. 이메일이 없는 기존 가입자도 같은 API로 처음 등록 |
| 관리자 전용 API (아직 없음) | O + `ROLE_ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` 사용. 권한 부족이면 403(`JwtAccessDeniedHandler`), 미인증이면 401(`JwtAuthenticationEntryPoint`) — 이 둘은 이미 배선 완료 |

**민감정보 취급 원칙**
- `studentId`(학번)는 로그인 식별자 전용 — 공개 응답 DTO에는 절대 노출하지 않고 `nickname`만 노출
- `refreshToken`은 DB에 평문 저장 중(해시 전환은 [8번 섹션](#8-남은-갭-우선순위-낮음) 참고)이라 `studentId`와 동일하게 응답 DTO 노출 금지 대상으로 취급
- `email`도 `studentId`와 동일하게 응답 DTO 노출 금지 대상. 인증코드는 원문을 저장하지 않고 BCrypt 해시만 저장하며 로그에도 남기지 않음

## 시크릿 키 관리

- DB 비밀번호와 동일한 패턴: `application.yml.example`에 `jwt.secret:` 빈 값으로 커밋, 각자 로컬 `application.yml`(gitignore 대상)에 실제 값 채움
- DB 비밀번호와 달리 팀원끼리 값이 같을 필요는 없음 — 각자 로컬 서버 안에서만 토큰 서명/검증에 쓰이는 값이라 인스턴스 내부 일관성만 있으면 됨

## 다른 도메인(교수/과목) 개발자 영향

- `SecurityConfig`에서 인증 불필요 경로(`GET /api/professors/**` 등)를 `permitAll`로 열어두면, 각 도메인 컨트롤러는 별도 처리 없이 그대로 사용 가능
- 인증이 필요한 경로만 필요 시 `@PreAuthorize` 또는 `SecurityConfig` 경로 매칭으로 제한

### 실제로 어느 경로를 막을지는 누가 정하나
인증 필터/엔트리포인트(5번 섹션)는 "토큰이 유효한지 판단하는 기계"를 만든 것뿐이고,
**어느 화면(엔드포인트)에 로그인을 요구할지는 각 도메인 담당자가 자기 API를 만들 때 정한다.**
Figma 화면 기준으로 "로그인 없이 보이는 화면(첫 페이지 등)"은 그대로 두고,
"로그인해야 접근 가능한 화면(마이페이지, 후기 작성 등)"만 아래처럼 추가하면 됨.

- `SecurityConfig.java`의 `TODO` 주석 자리(`authorizeHttpRequests` 블록, `anyRequest().permitAll()` 위)에
  `.requestMatchers(HttpMethod.POST, "/api/reviews/**").authenticated()`처럼 경로를 추가
- 그 API 컨트롤러에서는 `@AuthenticationPrincipal CustomUserDetails user`로 로그인한 사용자 정보(`userId` 등) 사용
- 토큰 없이/무효한 토큰으로 접근하면 `JwtAuthenticationEntryPoint`가 자동으로 401 응답 (직접 처리 불필요)
- 인증 도메인 쪽에서 추가로 해줄 작업은 없음 — 각 도메인 PR에서 위 두 줄만 추가하면 됨

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
- [x] `JwtAuthenticationFilter` / `JwtAuthenticationEntryPoint` — 5번 섹션 참고 (구현 완료)
- [ ] `JwtAccessDeniedHandler` — 6번 섹션으로 이동
- [ ] 정상·누락·변조·만료 토큰 테스트 — 8번 섹션으로 이동

### 4. 후속 작업 (Refresh Token) — [이슈 #55](https://github.com/inu-jeongbobada/jeongboBada-backend/issues/55)
- [x] Refresh Token 저장 구조 — `User` 엔티티에 `refreshToken`/`refreshTokenExpiresAt` 컬럼 (별도 테이블/Redis 없음, 멀티 디바이스 요구사항 없어 오버엔지니어링으로 판단)
- [x] `/api/auth/reissue`
- [x] `/api/auth/logout`
- [ ] Refresh Token 해시 저장 및 회전 — **회전(로그인/재발급마다 신규 발급)은 완료**, **해시 저장은 미완료**(현재 DB에 평문 저장, `studentId`처럼 응답 DTO 노출 금지 대상으로만 취급 중)

### 5. 인증 필터 (완료)
과목(course) 후기 컨트롤러에서 "로그인한 사용자만 접근 가능"이 필요해져서 우선 구현함.
- [x] 커스텀 `UserDetails`(`CustomUserDetails`) — `StudentUserDetailsService.loadUserByUsername()`이
      Spring 기본 `User`(username/password/authorities만 있음) 대신, `userId` 등 우리 도메인 정보를
      담은 객체를 반환하도록 교체
- [x] `JwtAuthenticationFilter` — `OncePerRequestFilter`. 매 요청 `Authorization` 헤더에서 토큰
      추출·검증 후 `SecurityContext`에 위 커스텀 `UserDetails` 채움
- [x] `JwtAuthenticationEntryPoint` — 인증 안 된 요청에 401 JSON 응답
- [x] `SecurityConfig`에 필터 등록 (`addFilterBefore` + `exceptionHandling`)
- [x] 과목 후기 담당자에게 엔티티에 작성자 `user_id` 컬럼(FK) 먼저 넣어두라고 전달함
- [ ] **후속(다른 도메인 담당)**: 실제로 로그인 필요한 경로를 `permitAll` → `authenticated()`로
      전환하는 건 각 도메인 PR에서 진행 — 위 "실제로 어느 경로를 막을지는 누가 정하나" 참고.
      지금은 배선만 끝났고 `anyRequest().permitAll()`이라 아직 아무 경로도 안 막혀있음

### 6. 권한 관리 / 개인정보 수정 (기능 명세 반영, 2순위) — [이슈 #64](https://github.com/inu-jeongbobada/jeongboBada-backend/issues/64)
- [x] `JwtAccessDeniedHandler` — 권한 부족 요청에 403 JSON 응답 (`GlobalErrorCode.FORBIDDEN` 추가)
- [x] `@EnableMethodSecurity` 활성화 + `SecurityConfig`에 `accessDeniedHandler` 배선 —
      `@PreAuthorize("hasRole('ADMIN')")`를 쓸 준비는 끝났고, `CustomUserDetails`가 이미
      `ROLE_ADMIN`/`ROLE_USER` 권한을 부여하고 있어 추가 변경 없이 바로 동작함
- [ ] **실제 관리자 전용 API 없음** — professor/course 컨트롤러가 전부 조회(GET)만 있어서
      `@PreAuthorize`를 붙일 대상이 아직 없음. 관리자 전용 API(교수/과목 등록·수정 등)가
      생기면 그 메서드에 애노테이션만 추가하면 됨
- [x] 개인정보 수정 API — 닉네임/비밀번호 변경 ([이슈 #86](https://github.com/inu-jeongbobada/jeongboBada-backend/issues/86))
      `UserController`/`UserService` 신설(`PATCH /api/users/me/nickname`, `PATCH /api/users/me/password`).
      `SecurityConfig`에 `/api/users/me/**` `authenticated()` 추가. 비밀번호 변경 성공 시
      `clearRefreshToken()`으로 기존 refresh token 무효화(재로그인 필요)하도록 결정

### 7. 비밀번호 찾기 (이메일 인증, 3순위) — [이슈 #87](https://github.com/inu-jeongbobada/jeongboBada-backend/issues/87)
PASS 본인인증은 소모임 프로젝트 규모에 비해 비용·행정 부담이 커서 채택하지 않고, 이메일 인증코드 방식으로 대체.

**이메일: 학교 이메일(`@inu.ac.kr`)이 아니라 개인 이메일(도메인 제한 없음)로 결정.**
당초 "학번 기반 서비스니까 학교 이메일"로 정했었으나, 재검토 결과 폐기함 — 회원가입 자체가 학번을
검증하지 않아(학교 인증 시스템 연동 없이 그냥 숫자 9자리만 확인) `@inu.ac.kr`을 강제해도 실질적인
"진짜 인천대생" 보장이 없고, 오히려 학교 이메일을 잘 안 써서 비밀번호 찾기가 필요한 순간 학교
이메일 자체에도 못 들어가는 역설이 더 큰 리스크로 판단됨. 목적은 "계정 소유자만 열어볼 수 있는
채널" 확보일 뿐이라 개인 이메일로 충분.

**구현 완료 (feat/password-reset-email)**
- [x] `User`에 `email` 컬럼 추가 (unique). **DB 컬럼은 null 허용, 회원가입 요청(`SignupRequest.email`)에서만 필수**로 결정 —
      이 기능 이전에 만들어진 행이 있는 DB(팀원 로컬 등)에서 `ddl-auto: update`가 not null unique 컬럼을 추가하다
      기동에 실패하는 걸 막기 위함 (unique는 null이 여러 개여도 통과). 저장·비교 전 항상 trim+소문자로 정규화(`EmailNormalizer`).
      재설정 시점에 즉석으로 받으면 타인 계정을 탈취할 수 있어서, 가입 때 등록해둔 이메일과 일치할 때만 코드를 발송
- [x] 이메일 발송 연동 — `spring-boot-starter-mail` + `EmailSender`(`global/mail`). `JavaMailSender`를 `ObjectProvider`로 받아서
      `spring.mail.host`가 없는 환경(CI 등)에서도 앱은 기동되고, 메일을 실제로 보내는 시점에만 실패
- [x] 인증코드 생성/저장 — `VerificationCode`(`@Embeddable` record: 해시/발급/만료/실패횟수)를 `User`의 컬럼으로 저장
      (별도 테이블/Redis 없음, refreshToken과 동일 패턴). 정책은 `VerificationCodeManager` 한 곳에서 관리:
      6자리(`SecureRandom`) · 5분 만료 · 재발송 쿨다운 60초 · 5회 틀리면 폐기 · DB에는 BCrypt 해시만 저장
- [x] 비밀번호 찾기 API — `PasswordResetController`/`PasswordResetService` (`send-code` → `verify-code` → 재설정, 코드는 1회 소모)
- [x] 이메일 등록/변경 API (2단계) — `UserController` + `EmailChangeService`
- [x] 테스트 — 서비스 단위 테스트(`VerificationCodeManagerTest`, `PasswordResetServiceTest`, `EmailChangeServiceTest`)와
      실제 MySQL로 API 전체 흐름을 통과시키는 통합 테스트(`PasswordResetIntegrationTest`)
- [x] 설정값 4곳 반영 — `application.yml`(로컬), `application.yml.example`, `docker-compose.yml`(`SPRING_MAIL_*`), `.env`/`.env.example`(`MAIL_*`)

**이메일 등록/변경 (로그인 필요, 2단계)**
사용자가 나중에 이메일을 바꾸고 싶을 수 있고, 이메일이 비어 있는 기존 가입자가 처음 등록할 경로도 필요해서 추가.
- `POST /api/users/me/email/send-code` — **새 이메일**로 인증코드 발송
- `PATCH /api/users/me/email` — `newEmail` + `code` + `currentPassword`를 받아 변경
- 2단계로 한 이유: 이메일은 비밀번호 찾기의 열쇠라서 값만 바꾸게 두면 계정 탈취 경로가 됨
  (access token 탈취 → 공격자 이메일로 변경 → 비밀번호 찾기로 재설정).
  새 이메일로 코드를 확인해 오타·타인 이메일 등록을 막고, `currentPassword`로 세션 탈취 상태의 변경을 막음
  (비밀번호 변경 API가 `currentPassword`를 요구하는 것과 같은 이유).

**설계 결정 (헷갈리기 쉬운 것)**
- **인증코드는 용도별 컬럼을 분리** (`PASSWORD_RESET_CODE_*` / `EMAIL_CHANGE_CODE_*` + `PENDING_EMAIL`). 하나를 공유하면
  로그인한 공격자가 자기 이메일로 "이메일 변경 코드"를 받아 그 코드로 남의 비밀번호 재설정을 통과시키는 우회가 생김 (테스트로 막혀 있음)
- **`verify-code`는 코드를 소모하지 않음.** 서버가 "검증 통과"를 기억하지 않으므로 재설정 API가 코드를 다시 검증·소모함
  → 프론트는 3단계 요청에도 2단계에서 입력한 `code`를 함께 보내야 함. 별도 재설정 토큰 발급 방식은 저장 컬럼이 늘어 채택하지 않음.
  틀린 횟수는 `verify-code`와 재설정 API가 합쳐서 셈
- **틀린 횟수는 예외가 나도 DB에 커밋돼야 함** → 검증 메서드에 `@Transactional(noRollbackFor = BusinessException.class)`.
  이걸 빼면 예외로 롤백돼서 횟수가 쌓이지 않고 무제한 시도가 가능해짐 (통합 테스트가 이 회귀를 잡는 것을 확인함)
- 없음/만료/불일치/없는 학번은 모두 `INVALID_VERIFICATION_CODE` 하나로 응답 — 추측 공격에 힌트를 주지 않기 위함
- 비밀번호 찾기 재설정 성공 시 `clearRefreshToken()` (비밀번호 변경 API와 동일하게 다른 기기는 재로그인)

**남은 갭 / 후속**
- [x] 실제 SMTP(Gmail 앱 비밀번호) 발송 확인 — 로컬 IDE 실행(`application.yml`)에서 비밀번호 찾기 `send-code` 메일 수신을 확인함 (2026-09-19)
- [ ] `docker compose`로 backend까지 띄우는 경로(`SPRING_MAIL_*` 환경변수 주입)에서는 아직 발송 확인 안 함. 이 경로는 파일이 아니라
      환경변수로 주입하므로 로컬 실행과 별개로 확인 필요
- [ ] 메일 발송이 요청 스레드에서 동기 처리됨 — SMTP 지연이 응답 시간에 그대로 반영되고, 응답 시간 차이로 가입 여부를 추정할 여지가
      있음. 비동기(`@Async`) 전환 검토
- [ ] 계정 단위 쿨다운/횟수 제한만 있고 IP 단위 rate limit은 없음 — 타인의 학번+이메일을 알면 60초마다 그 이메일로 코드 메일을 보낼 수 있음
- [ ] 인증코드 정책 값(5분/60초/5회)이 코드 상수 — 필요해지면 설정으로 분리
- [ ] 이메일 변경 성공 시 refresh token을 무효화할지 미정 (현재는 유지)
- [ ] **회원가입 API 계약 변경**(`email` 필수) — 프론트에 공유 필요
- [ ] 가입 시 이미 등록된 이메일이면 409라 "이 이메일이 가입돼 있다"가 노출됨 (학번/닉네임 중복과 같은 수준으로 감수)

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
