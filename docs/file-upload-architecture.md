# 파일 업로드(S3) 아키텍처

공용 파일 업로드 인프라의 설계와 진행 상태. GitHub #98.
재학생 인증(가입 서류)이 첫 적용 대상이고, 그 흐름은 [student-verification-architecture.md](student-verification-architecture.md)에 적는다.

## 결정된 것

| 항목 | 결정 | 근거 |
|---|---|---|
| 업로드 방식 | 서버 경유(multipart). 서버가 크기·형식을 직접 검사한다 | #98, 2026-09-26 |
| 저장소 | AWS S3, 버킷은 비공개 | #98, 2026-09-26 |
| 열람·다운로드 | 서버가 권한을 확인한 뒤 짧게 만료되는 presigned GET URL을 준다 | #98 |
| 위치 | `global`에 공용 컴포넌트 하나. 허용 형식·최대 크기는 호출하는 도메인이 넘긴다 | #98 |
| 신분 확인 서류 | pdf·jpg, 최대 10MB, 1개. 승인 후 바로 삭제 | [wireframe-spec.md](wireframe-spec.md) 1번 |
| 강의 자료 첨부 | 강의평이 아니라 `Material`(학습자료 탭)에 붙인다 | [wireframe-spec.md](wireframe-spec.md) 10번 |

## 진행 환경

해외 결제 카드가 없어 개인 AWS 계정을 바로 만들 수 없다. 그래서 단계를 나눈다.

| 단계 | 환경 | 확인하는 것 |
|---|---|---|
| 1. 코드 작성 | AWS 없음. S3 호출은 테스트에서 `@MockitoBean`으로 대체 | 검증 규칙, 에러 응답, 저장 흐름. CI도 이 상태로 통과해야 한다 |
| 2. 실제 S3 확인 | 연구실에서 개설 요청한 AWS Academy Learner Lab ($50 한도) | 실제 업로드·presigned URL·삭제 동작 |
| 3. 실제 서비스 | 미정 (팀 계정) | — |

Learner Lab에서 지킬 것:
- **실제 학생 서류를 올리지 않는다.** 테스트용 가짜 pdf·jpg만 쓴다. 코스가 끝나면 계정과 데이터가 삭제된다
- 키는 세션마다 새로 발급되는 임시 키(`AWS_SESSION_TOKEN` 포함)다. 고정 키를 만드는 IAM 사용자 생성은 막혀 있을 수 있다
- 리전이 제한될 수 있다(보통 `us-east-1`). 개설되면 실제 조건을 확인하고 이 문서에 적는다

## 설계

### 자격 증명
설정 파일에 access key·secret을 넣지 않는다. AWS SDK 기본 방식(`DefaultCredentialsProvider`)으로 환경에서 찾는다.
- 로컬·Learner Lab: 환경변수 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`
- EC2: 인스턴스 역할
- 단계가 바뀌어도 코드는 그대로이고 환경변수만 바뀐다

키는 S3를 실제로 호출할 때 찾으므로, 키가 없어도 서버는 뜬다(CI·compose 스모크 통과).

### 설정값
| 키 | 예시 | 비고 |
|---|---|---|
| `aws.region` | `us-east-1` | 필수 (`@Validated`) |
| `aws.s3.bucket` | `jeongbobada-dev` | 필수 (`@Validated`) |
| `spring.servlet.multipart.max-file-size` | `10MB` | Spring 기본값이 1MB라 반드시 올려야 한다 |
| `spring.servlet.multipart.max-request-size` | `11MB` | 파일 + 나머지 폼 필드 |

AGENTS.md 설정값 체크리스트 4곳 + `application-test.properties`(가짜 값)에 함께 반영한다.

### 저장 경로
- `{용도}/{UUID}.{확장자}` — 예: `verification/3f2a….pdf`
- 원본 파일명은 경로에 쓰지 않는다(한글·특수문자·개인정보가 섞일 수 있음). 필요하면 DB에 따로 저장한다
- 용도별 prefix: `verification/`(가입 서류), `material/`(학습자료)
- DB에는 URL이 아니라 **S3 key**를 저장한다(버킷이 비공개라 URL로 바로 열 수 없고, 열 때마다 presigned URL을 새로 만든다)

### 검증
서버가 업로드 전에 검사한다. 어긋나면 S3를 호출하지 않는다.
- 빈 파일
- 확장자 + `Content-Type`이 허용 목록에 있는지
- 파일 앞부분 바이트(pdf `%PDF`, jpg `FF D8 FF`) — 확장자만 바꾼 파일을 막는다
- 크기 — `max-file-size`를 넘으면 Spring이 먼저 막는다. 이때 500이 아니라 4xx로 나가는지 통합 테스트로 확인한다

### 에러 코드
`ErrorCode` enum과 `docs/api-spec.md` 표에 함께 추가한다.

| HTTP | code | 상황 |
|---|---|---|
| 400 | `FILE_EMPTY` | 파일이 없거나 비어 있음 |
| 400 | `FILE_TYPE_NOT_ALLOWED` | 허용하지 않는 형식 (화면 문구: pdf, jpg 파일만 올릴 수 있어요) |
| 413 | `FILE_TOO_LARGE` | 최대 크기 초과 (화면 문구: 10MB 이하 파일만 올릴 수 있어요) |
| 503 | `FILE_UPLOAD_FAILED` | S3 호출 실패 |

### 트랜잭션
S3는 DB 트랜잭션에 묶이지 않는다.
- 업로드 성공 후 DB 저장이 실패하면 S3에 파일만 남는다 → 실패 시 방금 올린 파일을 지운다
- DB에서 지운 뒤 S3 삭제가 실패하면 파일만 남는다 → 로그를 남기고, `verification/`은 버킷 수명주기 규칙으로 한 번 더 지운다

## 진행 상태

### PR 1. 공용 업로드 컴포넌트 (AWS 없이)
- [x] AWS SDK v2 S3 의존성 추가 (`software.amazon.awssdk:bom` 2.57.4 + `s3`)
- [x] `AwsProperties`(`@Validated`) + 설정값 5곳 반영
  - `docker-compose.yml`은 region·bucket에 기본값(`us-east-1`, `jeongbobada-dev`)을 둔다. 비밀값이 아니고, 기본값이 없으면 `.env`가 빈 CI 스모크에서 기동이 실패한다
  - 자격 증명 환경변수 3개(`AWS_ACCESS_KEY_ID` 등)는 `.env` → `docker-compose.yml`로 그대로 넘기기만 한다. 비어 있어도 기동된다
- [x] `S3Client`, `S3Presigner` 빈 (`global/file/S3Config`, 자격 증명은 `DefaultCredentialsProvider`)
- [x] 업로드 / 삭제 / presigned GET URL 발급 (`global/file/FileStorage`)
  - `upload(prefix, file, 허용 형식, 최대 크기)` → key 반환. 허용 형식은 `FileType`(지금은 `PDF`, `JPG`) 중에서 고른다
  - prefix 상수는 쓰는 곳(PR 2)에서 만든다. 이번 PR에는 호출하는 곳이 없다
  - `delete`와 `presignedGetUrl`은 S3 예외를 바꾸지 않고 그대로 던진다. 업로드 직후 정리인지, DB 삭제 뒤 정리인지에 따라 처리가 달라서 호출하는 쪽이 정한다 ("트랜잭션" 참고)
  - presigned URL 만료 시간은 호출하는 쪽이 넘긴다 (아래 "결정이 필요한 것")
- [x] 검증(빈 파일·크기·형식·시그니처) + 단위 테스트 `FileStorageTest`
  - 형식은 확장자로 고르고, `Content-Type`과 앞부분 바이트가 **둘 다** 그 형식과 맞아야 한다. `.jpeg`도 받지만 key는 `.jpg`
  - 크기도 여기서 한 번 더 본다. API마다 한도가 다를 수 있어서 `max-file-size`(서버 전체 한도)만으로는 부족하다
- [x] 에러 코드(`FileErrorCode`) + `api-spec.md` 표
- [x] `max-file-size` 초과 시 413 `FILE_TOO_LARGE` 통합 테스트 `MultipartSizeLimitIntegrationTest`
  - 업로드 API가 아직 없어서 공개 API(로그인)에 multipart를 보낸다. multipart 해석은 컨트롤러보다 먼저 일어나므로 같은 경로를 탄다
  - MockMvc는 크기 한도를 적용하지 않아서, 실제 Tomcat을 띄우고(`RANDOM_PORT`) HTTP로 보낸다
  - `GlobalExceptionHandler`가 부모의 `handleMaxUploadSizeExceededException`을 오버라이드한다 (#111). 이때는 한도 값이 예외에 안 담겨 와서(-1) 기본 문구가 나간다
- [x] 실제 S3 확인용 `S3RealBucketTest` — 환경변수 `S3_TEST_BUCKET`이 있을 때만 돈다 (CI는 자동 skip). 업로드 → presigned URL로 내려받기 → 삭제까지 확인. 실행법은 테스트 파일 주석 참고

### PR 2. 가입 서류 연결 → [student-verification-architecture.md](student-verification-architecture.md)
- [ ] `POST /api/auth/signup` multipart 전환, 서류 필수
- [ ] 가입 상태(승인 대기·승인·반려) + 관리자 승인/반려 API(`ROLE_ADMIN`)
- [ ] 승인·반려 시 서류 삭제

### 3. Learner Lab 실제 확인 (개설 후)
- [x] 리전·서비스 제한 확인: `us-east-1`에서 S3 버킷 생성·업로드·presigned URL·삭제 모두 가능 (2026-11-13)
  - Learner Lab 웹 터미널의 CLI는 Lab 계정이 아닌 `runserver` 역할로 실행돼 S3 권한이 없다. 버킷은 콘솔(`AWS ●`)에서 만든다
  - 로컬 키는 `AWS Details` → AWS CLI `Show` 블록을 `~/.aws/credentials`에 넣는다. `Start Lab`마다 바뀐다
- [x] 버킷 생성(퍼블릭 액세스 차단 유지)
- [x] `verification/` 수명주기 규칙 (`verification-cleanup`, 생성 후 30일 만료 — 연습용 값, 실제 일수는 미정)
- [x] 로컬에서 임시 키로 업로드·presigned URL·삭제 확인 (가짜 파일만, `S3_TEST_BUCKET`을 주고 `S3RealBucketTest` 실행해 통과)

## 결정이 필요한 것
- [ ] 학습자료 허용 파일 형식·최대 크기 (와이어프레임 미정)
- [ ] 실제 서비스용 AWS 계정 (명의·결제 수단)
- [ ] presigned GET URL 만료 시간 (관리자 서류 열람 기준)
- [ ] 반려 후 재제출 시 본인 확인 방식 (반려 사용자는 로그인 토큰이 없음)
