# course 도메인 아키텍처

전공 수업(course) 도메인의 설계 배경과 현재 구현 상태를 담은 문서. 이 도메인 코드를 만지는 작업을 마칠 때마다 최신화한다.

## 배경 — 왜 Course/CourseOffering을 분리했는가

기존에는 `Course` 테이블 하나에 과목 정보(이름, 코드, 설명)와 학기별 정보(교수, 학년, 시간표, 학점, 평가방식 등)가 전부 섞여 있었다. 이 구조로 "매 학기 과목 정보를 새로 넣는다"를 하려면 두 가지 방법뿐이었다:

1. 매 학기 기존 행을 DELETE하고 새로 INSERT — **`course_review`가 courseId를 FK로 참조하므로, 과목을 지우는 순간 그 과목에 달린 수업 후기가 전부 고아가 되거나 CASCADE로 삭제된다.** 서비스 핵심 가치인 "누적된 강의 후기"가 학기마다 초기화되는 셈.
2. 매 학기 새 courseId로 계속 INSERT만 함 — 같은 과목("데이터베이스" 등)이 학기마다 다른 courseId를 갖게 되어, 후기가 학기별로 쪼개지고 필터(학점별 후기 보기)가 무의미해짐.

### 실제 데이터로 검증한 사실

인천대 종합강의시간표(2025-2학기, 2026-2학기) 엑셀을 비교한 결과:

- **학수번호 앞자리(과목코드)는 연도가 바뀌어도 유지된다.** 예: "C언어"는 두 학기 모두 `0010086`.
- **학수번호 뒷자리(분반 번호)는 학기마다 재배정된다.** 예: `0010086004`가 2025-2엔 한재현 교수님, 2026-2엔 허혜선 교수님으로 바뀜.

즉 **학수번호 전체(분반 포함)를 과목/리뷰 식별자로 쓰면, 분반 번호가 바뀌는 순간 엉뚱한 교수님한테 과거 후기가 연결되는 사고**가 난다. 앞자리 과목코드만 안정적인 식별자로 쓸 수 있다.

### 참고 — inu-appcenter/inu-portal-server (INTIP)

동일한 문제를 실제로 겪고 있는 오픈소스 프로젝트를 확인했다. `Course`(과목, `courseCode`/`(title,department)` unique, `active` boolean으로 soft-delete)와 `CourseOffering`(학기별 개설, `course_id`+`professor_id`+`semester_id` 조합, unique 제약이 `(semester_id, subject_number)`)으로 테이블을 분리해두었다 — 학수번호가 학기 안에서만 유일하다는 걸 스키마로 명시한 것. 저희도 같은 방향으로 가되, 학교 API 연동용 원본 필드(dept_code 등 20여 개)는 들고 오지 않고 실제 쓰는 필드만 남겼다.

## 설계

```
Course (과목 자체, 학기 안 타는 정보)
 ├─ courseId       PK
 ├─ courseCode     UNIQUE  ─ 학수번호 앞자리(과목코드)
 ├─ courseName
 ├─ courseDetail
 └─ active         이번 학기 개설 안 되면 false. DELETE는 하지 않는다 (후기가 courseId를 참조).

CourseOffering (학기별 개설, 학기마다 값이 바뀌는 정보)
 ├─ courseOfferingId  PK
 ├─ course            FK -> Course
 ├─ professor         FK -> Professor
 ├─ academicYear      예: 2026
 ├─ semester          FIRST | SECOND
 ├─ grade / credits / courseTime / courseType / evaluationType / isOnline
 ├─ active            이번 학기 시간표에 안 나오면 false로만 바꾸고 DELETE 안 함
 └─ UNIQUE(course, professor, academicYear, semester)
     -> 같은 과목을 같은 교수가 같은 학기에 또 넣으면 새 행이 아니라 기존 행을 갱신

CourseReview (기존 + professor 추가)
 ├─ course     FK -> Course      (학기 안 타는 리뷰 대상 축 1)
 ├─ professor  FK -> Professor   (리뷰 대상 축 2 — NEW)
 └─ UNIQUE(user, course, professor)
```

**왜 리뷰가 `CourseOffering`이 아니라 `Course` + `Professor`를 직접 참조하는가**: `CourseOffering`은 매 학기 import 로직이 갱신/재생성할 수 있는 행이다. 만약 리뷰가 `CourseOffering`을 참조하면, 그 학기 개설이 사라지는 순간(=행이 비활성화되거나 다음 학기 데이터로 갱신되는 순간) 리뷰 연결이 애매해진다. `Course`+`Professor` 조합은 두 엔티티 모두 안정적인 PK를 가진, 학기 churn의 영향을 받지 않는 축이라 리뷰가 영구적으로 붙을 수 있다.

## API 영향

- `GET /api/courses` — **이번 학기 개설 강의(활성 CourseOffering) 기준으로 한 줄씩** 응답. `courseId`는 Course의 안정적인 id라서, 이 값으로 상세/후기 조회를 계속 이어갈 수 있다. (기존엔 목록 응답에 courseId가 아예 없던 버그가 있었음 — 이번에 같이 해결됨)
- `GET /api/courses/{courseId}` — Course 기본 정보 + 그 과목의 활성 `CourseOffering` 목록(교수/시간표/학점 등)을 함께 반환.
- `POST /api/courses/{courseId}/reviews` — 요청 바디에 `professorId`가 추가됨 (같은 과목도 교수마다 리뷰를 따로 남길 수 있어서). 서버는 그 `(course, professor)` 조합으로 실제 개설된 적이 있는지(`CourseOffering` 존재 여부) 검증 후 저장.

## 구현 상태

- [x] `Course` / `CourseOffering` 엔티티 분리
- [x] `CourseRepository`, `CourseOfferingRepository`
- [x] `GET /api/courses`, `GET /api/courses/{courseId}` — 새 구조로 마이그레이션, 로컬에서 응답 확인 완료
- [x] `CourseReview`에 `professor` FK 추가, unique 제약 `(user, course, professor)`로 변경
- [x] `POST .../reviews`에 `professorId` 검증 로직 추가 (컴파일/단위테스트만 확인, **실제 로그인 붙여서 수동 테스트는 아직 안 함**)
- [x] `data.sql`을 새 스키마(course + course_offering)로 재작성
- [ ] 매 학기 엑셀(종합강의시간표) import 파이프라인 — courseCode로 Course 매칭, (course,professor,year,semester)로 CourseOffering upsert. **아직 미구현, 이번 작업 범위 밖.**
- [ ] `CourseCreateReqDto`/`CourseUpdateReqDto` — 기존 flat 구조 그대로 남아있음(컨트롤러에 안 붙어있어서 컴파일은 됨). 관리자 등록 API를 실제로 만들 때 Course/CourseOffering 분리 구조에 맞게 다시 설계해야 함.
- [ ] "이번 학기가 몇 학기인지" 판단 로직 — 지금은 `CourseOffering.active` 플래그를 수동/import 시점에 맞춰 관리한다고 가정. 별도의 "현재 학기" 개념(설정값 or Semester 테이블)은 아직 없음.
- [ ] `GET /api/courses/{courseId}/reviews`의 불필요한 인증 체크 버그는 course 도메인이 아니라 course-review 쪽 이슈라 [docs/bugs.md](bugs.md)에 별도로 기록함.
- [ ] **`data.sql` 시딩 전략은 실 서비스 전에 반드시 바꿔야 함.** 지금은 서버가 뜰 때마다
      `course_review`/`course_offering`/`course`/`professor`를 통째로 DELETE 후 재삽입하는데
      (개발 편의용), 이건 실제 유저가 작성한 `course_review`가 쌓인 뒤에 그대로 배포하면
      재기동할 때마다 그 후기가 전부 날아간다. 후보:
      1. `spring.sql.init.mode`를 프로필별로 분리(`local: always`, `prod: never`) — 가장 간단
      2. Flyway/Liquibase 같은 버전 관리형 마이그레이션 도구로 교체 — 한 번 실행된 시딩은 재실행 안 됨
      3. 교수/과목 데이터를 `data.sql` 대신 ADMIN 전용 API로 등록·관리 (6번 권한 관리 작업과 연결)
      운영 배포를 준비하는 시점에 최소 1번은 반드시 적용할 것.

## 참고 파일

- [Course.java](../src/main/java/com/inu/jeongbobada/domain/course/entity/Course.java)
- [CourseOffering.java](../src/main/java/com/inu/jeongbobada/domain/course/entity/CourseOffering.java)
- [CourseService.java](../src/main/java/com/inu/jeongbobada/domain/course/service/CourseService.java)
- [CourseReview.java](../src/main/java/com/inu/jeongbobada/domain/courseReview/entity/CourseReview.java)
- [CourseReviewService.java](../src/main/java/com/inu/jeongbobada/domain/courseReview/service/CourseReviewService.java)
