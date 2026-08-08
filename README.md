# jeongboBada

인천대학교 학과 정보 플랫폼(정보바다)의 백엔드. 소모임 팀 프로젝트.

프론트엔드는 React로 별도 레포에서 개발되며, 이 저장소는 REST API를 제공하는 백엔드입니다.

## 기술 스택

| 분류 | 내용 |
|---|---|
| 언어/프레임워크 | Java 21, Spring Boot 4.1.0 |
| 빌드 도구 | Gradle (Groovy) |
| DB | MySQL 8.4 (Docker Compose로 실행) |
| 인증 | Spring Security + JWT (jjwt 0.12.7), 로그인 식별자는 학번(student_id) |
| API 문서 | springdoc-openapi (Swagger UI) |

## 시작하기

### 1. 요구 사항
- JDK 21
- Docker Desktop

> MySQL은 로컬에 직접 설치하지 않습니다. 반드시 Docker Compose로 띄운 컨테이너를 사용합니다.

### 2. MySQL 실행

```bash
docker compose up -d
```

`docker-compose.yml`에 MySQL 8.4 컨테이너 설정이 고정되어 있어, 별도 설정 없이 팀원 누구나 동일한 DB 환경을 띄울 수 있습니다.

### 3. 애플리케이션 설정

`application.yml`은 DB 비밀번호가 포함되어 있어 git에 커밋하지 않습니다. 예제 파일을 복사해서 사용하세요.

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

기본값 그대로면 위 Docker Compose 설정과 바로 맞습니다.

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 5. API 문서 확인

애플리케이션 실행 후 아래 주소에서 Swagger UI로 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

## 프로젝트 구조

패키지는 계층(controller/service/...)이 아니라 **도메인(기능) 단위**로 나눕니다.

```
com.inu.jeongbobada
├── global/      # 공통 설정, 보안(JWT), 예외 처리
├── user/        # 인증·사용자
├── professor/   # 교수 정보
├── course/      # 전공 수업
├── community/   # 게시판·댓글
├── career/      # 진로 수기
└── mypage/      # 즐겨찾기·알림
```

## 데이터 모델

`user`, `professor`, `course`, `course_review`, `professor_review`, `post`, `comment`, `career_story`, `favorite`, `notification` — 총 10개 테이블. 자세한 내용은 [docs/erd.md](docs/erd.md) 참고.

PK는 전부 `BIGINT AUTO_INCREMENT`를 사용합니다 (UUID 아님).

## 개발 우선순위 (MVP)

1. 인증 (회원가입/로그인, JWT)
2. 교수 정보
3. 전공 수업 (수업 후기, 학점별 필터)
4. 커뮤니티 / 진로 / 개인화 (즐겨찾기·알림)

## 기여 방법

- PR을 올릴 때는 [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md) 형식을 따릅니다.
- 커밋/PR 제목은 `<타입>: <한 줄 요약>` 형식입니다. (`feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`)
- PR을 올리면 GitHub Actions([ci.yml](.github/workflows/ci.yml))가 자동으로 빌드/테스트를 검증합니다.
- API 스펙 초안은 [docs/api-spec.md](docs/api-spec.md)에서 관리합니다.
