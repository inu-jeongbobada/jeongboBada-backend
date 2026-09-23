# jeongboBada Backend

> 이 파일은 팀이 쓰는 AI 에이전트(Codex, Claude Code 등)가 **함께 읽는 프로젝트 규칙**이다.
> Codex는 이 파일을 직접 읽고, Claude Code는 `CLAUDE.md`의 `@AGENTS.md`로 불러온다.
> **규칙은 이 파일에서만 고친다.** `CLAUDE.md`에 같은 내용을 복사하지 말 것 (한쪽만 고쳐지면 도구마다 다른 규칙으로 작업하게 된다).

인천대학교 학과 정보 플랫폼(정보바다)의 백엔드. 소모임 팀 프로젝트.

## 기술 스택 (확정)
- Java 21, Spring Boot 4.1.0, Gradle(Groovy)
- DB: **MySQL 8.x — 반드시 Docker로 실행. 로컬에 MySQL 직접 설치하지 말 것.**
- 인증: Spring Security + JWT (jjwt 0.12.7). 로그인 식별자는 학번(student_id)
- API 문서: springdoc-openapi (Swagger UI)
- 프론트엔드: React (별도 레포, REST API로 통신)

## 중요 규칙
- **MySQL은 로컬 설치 금지. docker-compose로 컨테이너를 띄워서 사용한다.**
- PK는 `BIGINT AUTO_INCREMENT` 사용 (UUID 아님).
- `application.yml`, `application-*.yml`은 .gitignore로 제외 (DB 비밀번호 포함).
  대신 값을 비운 `application.yml.example`을 커밋한다.
- 패키지는 계층별이 아니라 **도메인(기능)별**로 나눈다.

## 설정값(application.yml) 추가/변경 시 체크리스트
새 설정값(`jwt.*` 같은)을 추가하거나 값을 바꿀 때, 아래 4곳(+ 통합 테스트가 그 값을 쓰면 1곳)을 세트로 확인한다.
로컬 IDE 실행(`application.yml`)과 `docker compose up`으로 백엔드 컨테이너까지 띄우는 경로가
설정을 주입받는 방식이 완전히 달라서(전자는 파일, 후자는 환경변수), 하나만 고치면
Docker 경로에서 컨테이너가 기동 실패한다.

- [ ] `application.yml` (로컬 실행용, 실제 값, gitignore 대상)
- [ ] `application.yml.example` (템플릿, 커밋 대상 — 값은 비워두거나 예시로)
- [ ] `docker-compose.yml`의 `backend.environment` (컨테이너 실행용 env 주입, `${VAR:-기본값}` 형태 권장)
- [ ] `.env` / `.env.example` (`docker-compose.yml`이 참조하는 env 값)
- [ ] `src/test/resources/application-test.properties` (**통합 테스트가 그 값을 필요로 할 때만**, 테스트 전용 가짜 값.
      CI에는 `application.yml`이 없어서 여기 없으면 "로컬은 통과, CI만 실패"한다.
      확장자는 `.properties`로 고정 — `.yml`로 만들면 `.gitignore`의 `application-*.yml`에 걸려 커밋에서 조용히 빠진다)

작업을 마치기 전에 이 체크리스트를 훑어보고, 새로 추가한 설정값이 위 곳들에 다 반영됐는지 확인할 것.
(Claude Code에서는 `.claude/hooks/check-config-sync.sh` 훅이 파일 수정 때마다 이 체크리스트를 자동으로 검사한다.
Codex 등 다른 도구에는 이 훅이 없으니 반드시 직접 확인한다.)

## 테스트 작성 기준
**"DB가 실제로 있어야 답이 나오는 것"이면 통합 테스트, 순수 로직이면 단위 테스트**로 나눈다.

| 확인하려는 것 | 테스트 |
|---|---|
| 계산·정렬·조건 분기 같은 순수 로직 | 단위 테스트 (DB 불필요, 예: `CourseReviewSortTest`) |
| 쿼리 결과, UNIQUE/FK 제약, 트랜잭션 저장/롤백 | 통합 테스트 |
| 에러가 올바른 응답 코드(400/401/403 등)로 나가는지, 로그인 필요/권한 경로 | 통합 테스트 |
| 화면·문구 | 수동 확인 |

PR에 아래 중 하나라도 있으면 **통합 테스트를 1개 이상** 붙인다. 새 API는 "성공 1개 + 대표 실패 1개"면 충분하다.
1. 새 DB 쿼리(Repository 메서드)를 추가했다
2. UNIQUE·FK·삭제 관련 동작을 다룬다
3. 트랜잭션 저장/롤백이 결과에 영향을 준다
4. 로그인이 필요한 새 API를 추가했다

통합 테스트 작성 방법 (예: `PasswordResetIntegrationTest`):
- `@SpringBootTest` + `@ActiveProfiles("test")` — 필요한 설정은 `application-test.properties`에서 받는다
- 실제 MySQL(docker)을 쓰고, 테스트가 만든 데이터는 끝나고 직접 지운다
- 메일 발송 같은 외부 호출은 `@MockitoBean`으로 대체한다
- **로컬에서 통과했다고 끝내지 말 것.** 로컬은 각자의 `application.yml`이 있어서 CI와 조건이 다르다. 설정 파일이 없는 깨끗한
  복제본에서 CI와 같이 환경변수 3개(`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`)만 주고 `./gradlew build`가 통과하는지 확인한다.
- **DB도 CI처럼 비어 있어야 한다.** 로컬 DB에는 `bootRun`이 넣은 `data.sql` 시드 데이터가 있어서, 데이터에 따라 결과가 달라지는
  테스트는 로컬만 통과할 수 있다. 빈 MySQL을 임시로 띄워 확인하고 끝나면 지운다:
  `docker run -d --name jb-ci-mysql -e MYSQL_DATABASE=jeongbobada -e MYSQL_USER=jeongbobada -e MYSQL_PASSWORD=jeongbobada -e MYSQL_ROOT_PASSWORD=root -p 3307:3306 mysql:8.4`
  → `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/jeongbobada` 로 빌드 → `docker rm -f jb-ci-mysql`

## 에러 처리 규칙
에러 응답은 `{ success:false, code, message, errors? }`(`ApiResponse`) 하나로 통일한다. 아래 규칙은 테스트 2개가 CI에서 강제한다.

- **새 에러는 도메인 `ErrorCode` enum과 `docs/api-spec.md` "에러 코드" 표에 함께 추가한다.**
  - code는 상황 하나에 하나, 의미 있는 `UPPER_SNAKE_CASE` 문자열 (예: `DUPLICATE_NICKNAME`). HTTP 상태나 번호를 넣지 않는다 (#115)
  - `ErrorCodeRulesTest`가 code 중복·형식, 문서 표와의 일치(HTTP 상태 포함)를 검사한다
- **클라이언트가 잘못 보낸 요청은 500이 아니라 4xx여야 한다.**
  - 요청 DTO에 검증 애노테이션(`@NotNull`/`@NotBlank`/`@Size` 등)을 붙이고 컨트롤러 `@RequestBody`에 `@Valid`. 필수값 null이 서비스까지 내려가 500이 되는 게 가장 흔한 원인이다
  - `BadRequestSafetyNetIntegrationTest`가 **모든 API**에 잘못된 요청(body 없음·깨진 JSON·`{}`·`[]`·text/plain·경로 변수 문자열/범위 초과 × 토큰 유무)을 보내 500이 없는지 검사한다. 새 API도 자동으로 포함된다
  - 당장 못 고치는 500은 버그 이슈를 만들고 그 테스트의 `KNOWN_500`에 이슈 번호와 함께 등록한다. **고치면 반드시 제거한다** — 그 이슈 체크리스트에 "KNOWN_500에서 제거"를 적어둘 것 (테스트가 강제하지 못한다: 500 도달 여부가 DB 데이터에 따라 달라서)
- **필터(`OncePerRequestFilter` 등)에서는 예외를 던지지 않는다.** 필터는 `GlobalExceptionHandler`보다 앞이라 예외가 500으로 샌다 (#110)
- **Spring MVC 표준 예외에 `@ExceptionHandler`를 새로 달지 않는다.** `GlobalExceptionHandler`가 `ResponseEntityExceptionHandler`를 상속해 이미 처리하므로, 같은 타입에 또 달면 기동 시 ambiguous 오류가 난다. 메시지를 바꾸려면 부모의 `handleXxx`를 오버라이드한다 (#111)
- **UNIQUE 컬럼은 서비스에서 사전 확인(`existsBy...`)하고 도메인 code(예: `DUPLICATE_NICKNAME`)로 응답한다.** 확인과 저장 사이 동시 요청 충돌은 전역 핸들러가 409 `DUPLICATE_RESOURCE`로 처리한다 (#109)

## 도메인 아키텍처 문서화 규칙
어떤 도메인에 `docs/{도메인}-architecture.md`(예: `docs/auth-architecture.md`)가 있다면,
그 문서는 해당 도메인의 설계·진행 상태를 담은 살아있는 문서다. 그 도메인 코드를
구현하거나 수정하는 작업을 마칠 때마다, 마무리 단계에서 문서를 실제 상태에 맞게 최신화한다.

- 구현 완료된 항목은 체크리스트에서 `[ ]` → `[x]`로 변경
- 계획과 실제 구현이 달라진 부분(클래스명, 저장 방식 등)은 문서에 반영
- 새로 발견된 갭(예: 아직 없는 컴포넌트, 남은 작업)은 문서에 추가
- 문서 갱신은 코드 변경과 같은 작업 단위로 취급 — 별도로 미루지 않는다

## 패키지 구조
com.inu.jeongbobada
- global/ : config, security(JWT), exception, common
- user/ : 인증·사용자 (controller/service/repository/entity/dto)
- professor/ : 교수 정보
- course/ : 전공 수업
- community/ : 게시판·댓글
- career/ : 진로 수기
- mypage/ : 즐겨찾기·알림

## 데이터 모델 (10개 테이블)
user, professor, course, course_review, professor_review,
post, comment, career_story, favorite, notification

## 개발 우선순위 (MVP)
1. 인증(회원가입/로그인 JWT) → 2. 교수 정보 → 3. 전공 수업(수업 후기, 학점별 필터)
   그 다음에 커뮤니티 / 진로 / 개인화 순으로.
