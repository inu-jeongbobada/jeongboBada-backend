# jeongboBada Backend

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
새 설정값(`jwt.*` 같은)을 추가하거나 값을 바꿀 때, 아래 4곳을 세트로 확인한다.
로컬 IDE 실행(`application.yml`)과 `docker compose up`으로 백엔드 컨테이너까지 띄우는 경로가
설정을 주입받는 방식이 완전히 달라서(전자는 파일, 후자는 환경변수), 하나만 고치면
Docker 경로에서 컨테이너가 기동 실패한다.

- [ ] `application.yml` (로컬 실행용, 실제 값, gitignore 대상)
- [ ] `application.yml.example` (템플릿, 커밋 대상 — 값은 비워두거나 예시로)
- [ ] `docker-compose.yml`의 `backend.environment` (컨테이너 실행용 env 주입, `${VAR:-기본값}` 형태 권장)
- [ ] `.env` / `.env.example` (`docker-compose.yml`이 참조하는 env 값)

작업을 마치기 전에 이 체크리스트를 훑어보고, 새로 추가한 설정값이 4곳 다 반영됐는지 확인할 것.

## 인증(auth) 관련 작업 시 문서화 규칙
`docs/auth-architecture.md`는 인증 도메인의 설계·진행 상태를 담은 살아있는 문서다.
인증 관련 코드(회원가입/로그인/JWT/refresh token/인증 필터 등)를 구현하거나 수정하는
작업을 마칠 때마다, 그 작업 마무리 단계에서 이 문서를 실제 상태에 맞게 최신화한다.

- 구현 완료된 항목은 체크리스트에서 `[ ]` → `[x]`로 변경
- 계획과 실제 구현이 달라진 부분(클래스명, 저장 방식 등)은 문서에 반영
- 새로 발견된 갭(예: 아직 없는 컴포넌트, 남은 작업)은 "남은 갭" 섹션에 추가
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
