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
