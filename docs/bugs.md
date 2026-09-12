# 발견된 버그 모음

테스트하다가 발견한 버그를 도메인/파일별로 정리해두는 문서. 새로 발견하면 아래 형식으로 추가한다.

## course-review: 후기 목록 조회(GET)에 불필요한 로그인 체크

- **발견자**: 유기현
- **발견일**: 2026-09-11
- **상태**: 미수정 (다른 개발자 담당)
- **관련 파일**: [CourseReviewController.java](../src/main/java/com/inu/jeongbobada/domain/courseReview/controller/CourseReviewController.java)
- **증상**: `GET /api/courses/{courseId}/reviews`를 로그인 없이 호출하면 401(`COMMON_401`, "인증이 필요합니다")이 반환됨.
- **원인**: `getReviews()`에 `POST .../reviews`(후기 등록)용 `if (user == null) throw ...` 체크가 그대로 복사되어 들어감. 정작 `courseReviewService.getReviews(courseId, sort)` 호출에는 `user`를 넘기지도 않아, 로그인 여부를 실제로 쓰지 않으면서 막기만 하는 상태.
- **수정 방향**: 후기 등록(쓰기)은 작성자 식별을 위해 로그인이 필요하지만, 후기 목록 조회(읽기)는 비로그인 사용자도 볼 수 있어야 함. `getReviews()`의 `user == null` 체크를 제거.

## 전역: 윈도우에서 data.sql의 한글이 깨진 채로 DB에 들어감

- **발견자**: 팀원(윈도우 사용)
- **발견일**: 2026-09-12
- **수정일**: 2026-09-12
- **상태**: 수정 완료
- **관련 파일**:
  - [application.yml.example](../src/main/resources/application.yml.example)
  - [docker-compose.yml](../docker-compose.yml)
- **증상**: 맥에서는 정상인데 윈도우에서 `docker compose up`으로 백엔드를 띄우면 교수/과목 이름 등
  `data.sql`로 들어간 한글 데이터가 깨져서 저장됨. Swagger 표시 문제가 아니라 DB에 실제로 깨진
  값이 들어간 것으로 확인됨.
- **원인**: `spring.sql.init.encoding` 설정이 없어서, Spring이 `data.sql`을 읽을 때 OS 기본
  인코딩에 의존했음. 맥은 기본이 UTF-8이라 문제없지만, 윈도우 로케일 기본 인코딩(CP949/MS949 등)
  에서는 UTF-8로 저장된 `data.sql`의 한글이 깨진 채로 읽힘. `data.sql` 파일 자체는 UTF-8이 맞고,
  git 줄바꿈(EOL) 설정 문제도 아님(확인 완료).
- **수정 내용**: `application.yml`/`application.yml.example`/`docker-compose.yml` 세 곳에
  `spring.sql.init.encoding: UTF-8`(env: `SPRING_SQL_INIT_ENCODING`) 명시적으로 추가.
  이미 깨진 상태로 들어간 데이터는 설정만으로 복구되지 않으므로, 재기동해서 `data.sql`이
  다시 실행되며 정상 값으로 재시딩되게 함 (아직 실 서비스 전이라 지워도 되는 시드 데이터라 안전).

## course: 강의 상세 조회에서 Course ↔ Professor 순환참조

- **발견자**: 유기현
- **발견일**: 2026-09-11
- **수정일**: 2026-09-11
- **상태**: 수정 완료
- **관련 파일**:
  - [CourseDetailResDto.java](../src/main/java/com/inu/jeongbobada/domain/course/dto/response/CourseDetailResDto.java)
  - [CourseService.java](../src/main/java/com/inu/jeongbobada/domain/course/service/CourseService.java)
  - [CourseProfessorResDto.java](../src/main/java/com/inu/jeongbobada/domain/course/dto/response/CourseProfessorResDto.java) (신규)
- **증상**: `GET /api/courses/{courseId}` 호출 시 같은 강의/교수 정보가 무한히 반복되다가 잘린, 파싱 불가능한 JSON이 응답됨.
- **원인**: `CourseDetailResDto.professor` 필드가 DTO가 아니라 JPA 엔티티 `Professor`를 그대로 들고 있었음. `Course.professor`(다대일)와 `Professor.courses`(일대다, `mappedBy`)가 같은 FK 하나를 양방향으로 참조하는 구조인데, Jackson이 순환 감지 없이 `course → professor → courses[0](=같은 course) → professor → ...` 경로를 계속 따라가며 직렬화하다 StackOverflowError로 응답 스트림이 중간에 끊김.
- **수정 내용**: `courses` 백레퍼런스가 없는 `CourseProfessorResDto`(professorId/professorName/professorImageUrl)를 만들어 `CourseDetailResDto.professor`가 엔티티 대신 이 DTO를 갖도록 변경. `docs/api-spec.md`의 강의 상세 조회 응답 예시도 함께 수정.
