# API 명세

개발 우선순위(AGENTS.md)에 맞춰 도메인별로 채워 나간다.

## 1. 인증 (user)
공통 응답 포맷: `{ "success": boolean, "data": T, "code": string | null, "message": string | null, "errors"?: [{ "field", "message" }] }`
(`success: false`일 때 `code`/`message`에 에러 정보 — 아래 [에러 코드](#에러-코드) 표 참고)

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| GET | /api/auth/check-nickname?nickname= | 회원가입 폼에서 닉네임 사용 가능 여부 실시간 확인 | Query: <br/> `nickname` | 200 <br/> { "success": true, "data": { "available": true } } | 구현완료 |
| POST | /api/auth/signup | 회원가입 (학번 기반) + 재학 증빙 서류 제출 | `multipart/form-data` <br/> `studentId`: "202012345" <br/> `password`: 영문+숫자 8~64자 <br/> `nickname`: 한글/영문/숫자 2~10자 <br/> `email`: "user@example.com" <br/> `file`: 재학 증빙 서류 (pdf·jpg, 최대 10MB) | 201 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/verification/resubmit | 반려 후 서류 다시 제출 (로그인 불필요) | `multipart/form-data` <br/> `studentId`: "202012345" <br/> `password`: 가입 비밀번호 <br/> `file`: 새 재학 증빙 서류 (pdf·jpg, 최대 10MB) | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/login | 로그인 (JWT 발급) | { <br/> "studentId": "202012345", <br/> "password": "..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/reissue | Access Token 재발급 | { <br/> "refreshToken": "eyJ..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/logout | 로그아웃 (서버의 refresh token 무효화) | { <br/> "refreshToken": "..." <br/>} (권장) <br/> 또는 Header: `Authorization: Bearer {accessToken}` (예전 방식) | 200 <br/> { "success": true, "data": null } <br/> 둘 다 없으면 400 | 구현완료 |
| POST | /api/auth/password-reset/send-code | 비밀번호 찾기 ① 인증코드 발송 | { <br/> "studentId": "202012345", <br/> "email": "가입 때 등록한 이메일" <br/>} | 200 <br/> { "success": true, "data": { "resendCount": 1, "maxResendCount": 5 } } | 구현완료 |
| POST | /api/auth/password-reset/verify-code | 비밀번호 찾기 ② 인증코드 확인 (코드는 소모되지 않음) | { <br/> "studentId": "202012345", <br/> "code": "숫자 6자리" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset | 비밀번호 찾기 ③ 비밀번호 재설정 (코드 1회 소모) | { <br/> "studentId": "202012345", <br/> "code": "숫자 6자리", <br/> "newPassword": "영문+숫자 8~64자" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| GET | /api/users/me | 내 정보 조회 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": { <br/> "nickname": "바다", <br/> "studentId": "202012345", <br/> "email": "user@example.com", <br/> "department": null, <br/> "role": "USER" <br/>} } | 구현완료 |
| POST | /api/users/me/email/send-code | 이메일 등록/변경 ① 새 이메일로 인증코드 발송 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "newEmail": "new@example.com" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| PATCH | /api/users/me/email | 이메일 등록/변경 ② 코드+현재 비밀번호 확인 후 변경 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "newEmail": "new@example.com", <br/> "code": "숫자 6자리", <br/> "currentPassword": "..." <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| GET | /api/admin/verifications?status= | 가입 검토 목록 (관리자) | Header: <br/> `Authorization: Bearer {accessToken}` (ADMIN) <br/> Query: `status` = `PENDING` / `APPROVED` / `REJECTED` | 200 <br/> { "success": true, "data": [ { <br/> "userId": 1, "nickname": "바다", "studentId": "202012345", <br/> "email": "user@example.com", "appliedAt": "2026-11-17T10:00:00", <br/> "status": "PENDING", "documentType": "pdf", <br/> "processedAt": null, "rejectionReason": null <br/>} ] } | 구현완료 |
| GET | /api/admin/verifications/{userId} | 가입 검토 상세 + 서류 열람 주소 (관리자) | Header: <br/> `Authorization: Bearer {accessToken}` (ADMIN) | 200 <br/> { "success": true, "data": { <br/> "applicant": { 목록 항목과 같음 }, <br/> "documentUrl": "https://...(5분 뒤 만료)" <br/>} } | 구현완료 |
| POST | /api/admin/verifications/{userId}/approve | 가입 승인 (관리자) | Header: <br/> `Authorization: Bearer {accessToken}` (ADMIN) | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/admin/verifications/{userId}/reject | 가입 거절 (관리자) | Header: <br/> `Authorization: Bearer {accessToken}` (ADMIN) <br/> { <br/> "reason": "필수, 200자 이하" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |

- 닉네임 중복확인은 **닉네임만 제공**한다. 학번/이메일 중복확인은 제공하지 않음 (이 학번/이메일로 가입돼 있다는 걸 누구나 조회할 수 있게 되는 user enumeration 방지) — 가입 시도 시 409로만 안내. 확인 시점과 실제 가입 시점 사이에 다른 사람이 선점할 수 있어 최종 검증은 `signup`에서 다시 함
- 회원가입에 `email`이 **필수**로 추가됨 (비밀번호 찾기용). 이미 가입된 이메일이면 409. 대소문자/앞뒤 공백은 무시하고 비교
- 회원가입은 **`multipart/form-data`**로 보낸다 (#142). 입력값은 폼 필드, 재학 증빙 서류는 `file` 파트. JSON으로 보내면 415 `UNSUPPORTED_MEDIA_TYPE`
  - 서류 오류: 없음 400 `FILE_EMPTY`, pdf·jpg가 아님 400 `FILE_TYPE_NOT_ALLOWED`, 10MB 초과 413 `FILE_TOO_LARGE`, 저장 실패 503 `FILE_UPLOAD_FAILED`. 학번·닉네임·이메일 중복(409)이면 서류는 저장하지 않는다
  - 가입하면 **승인 대기** 상태가 된다. **관리자가 승인하기 전에는 로그인할 수 없다** (아래 "승인 전 로그인")
- 승인 전 로그인 (#142 4단계): 학번·비밀번호가 맞아도 승인 대기·반려면 토큰 대신 **403**을 준다. 화면은 `code`로 팝업을 고르고 `data`를 보여준다
  ```json
  { "success": false, "code": "VERIFICATION_PENDING", "message": "가입 승인을 기다리고 있어요",
    "data": { "requestedAt": "2026-11-18T10:00:00" } }
  { "success": false, "code": "VERIFICATION_REJECTED", "message": "가입 신청이 반려되었어요. 서류를 다시 제출해주세요",
    "data": { "rejectionReason": "학번이 보이지 않아요", "nickname": "물먹은다람쥐" } }
  ```
  - 반려 응답의 `nickname`은 "서류 다시 올리기" 화면의 잠긴 닉네임 칸에 쓴다. 반려 사용자는 토큰이 없어 내 정보 조회(`/api/users/me`)를 쓸 수 없다
  - 상태는 비밀번호 확인 **뒤에** 본다. 비밀번호가 틀리면 상태와 상관없이 401 `INVALID_CREDENTIALS`
  - 토큰 재발급(`/api/auth/reissue`)도 같은 이유로 403 (차단 전에 받은 refresh token으로 계속 쓰는 것 방지)
  - 403이라 프론트의 "401 → 토큰 재발급" 처리와 겹치지 않는다
  - 반려면 "서류 다시 제출"(`/api/auth/verification/resubmit`), 비밀번호를 잊었으면 비밀번호 찾기로 이어진다. 둘 다 로그인 없이 쓸 수 있다
- 새 비밀번호(가입·변경·재설정)는 64자 이하이면서 **72바이트 이하**여야 한다. 영문·숫자는 1바이트, 한글은 3바이트로 계산하며, 넘으면 400 `INVALID_INPUT_VALUE`
- 반려 후 다시 제출(`POST /api/auth/verification/resubmit`): 반려된 사용자는 로그인 토큰이 없어서 **학번 + 비밀번호 + 새 서류**를 함께 보낸다. 성공하면 다시 승인 대기가 되고 신청일이 지금으로 바뀐다
  - 학번·비밀번호 확인은 로그인과 같다: 없는 학번 404 `STUDENT_ID_NOT_REGISTERED`, 틀리면 401 `INVALID_CREDENTIALS`(n/5회), 5회 실패면 429 `LOGIN_LOCKED`. **실패 횟수는 로그인과 함께 센다**
  - 반려 상태가 아니면 409 `VERIFICATION_NOT_REJECTED`. 서류 오류는 가입과 같다(`FILE_EMPTY`, `FILE_TYPE_NOT_ALLOWED`, `FILE_TOO_LARGE`, `FILE_UPLOAD_FAILED`)
  - 비밀번호를 잊었으면 비밀번호 찾기로 먼저 재설정한다 (로그인 없이 쓸 수 있다)
- 관리자 가입 검토(`/api/admin/verifications`)는 **ADMIN만** 호출할 수 있다. 토큰이 없으면 401, 일반 사용자면 403 `FORBIDDEN`
  - 목록: `PENDING`은 먼저 **신청한** 순(재제출하면 그 시각 기준), `APPROVED`·`REJECTED`는 최근 처리한 순. 검토 없이 승인 상태인 기존 계정은 승인 목록에 나오지 않는다. `appliedAt`은 신청일(재제출하면 그 날짜), `processedAt`은 승인일/거절일, `documentType`은 서류가 남아 있을 때(대기 중)만 값이 있다
  - 상세의 `documentUrl`은 **5분 뒤 만료**된다. 만료되면 상세를 다시 조회해 새 주소를 받는다. 서류가 없으면 null, 주소를 만들지 못하면 503 `FILE_URL_FAILED`
  - 승인·거절하면 서류를 바로 삭제하고 결과 메일을 보낸다. 메일·삭제가 실패해도 처리는 유지된다(서버 로그로 확인). 거절 사유는 필수다(비우거나 공백만 보내면 400 `INVALID_INPUT_VALUE`). 앞뒤 공백을 지워 저장하고 메일·로그인 팝업에 그대로 보여준다
  - 승인 대기가 아닌 계정을 처리하면 409 `VERIFICATION_ALREADY_PROCESSED`, 없는 사용자는 404 `USER_NOT_FOUND`
- 비밀번호 찾기: `send-code`는 학번이 없거나 이메일이 다르면 404 `PASSWORD_RESET_ACCOUNT_NOT_FOUND`, 60초 안에 재요청하면 429 `VERIFICATION_CODE_RESEND_TOO_FAST`, 재발송을 다 쓰면 429 `PASSWORD_RESET_RESEND_LIMIT_EXCEEDED`, 발송 실패는 503 `EMAIL_SEND_FAILED`. 인증코드는 6자리, 5분 유효. 재발송은 첫 발송을 빼고 하루 5회(한국 시간 자정에 초기화)이고, `resendCount`는 첫 발송이면 0, 재발송이면 n/5회의 n이다
- 내 정보 조회(`GET /api/users/me`)는 **본인 전용**이라 `studentId`·`email`을 내려준다. `email`은 이메일 기능 이전 가입자면 null, `department`는 가입 때 받지 않아 **현재 항상 null**. `role`은 `USER` 또는 `ADMIN`으로, 프론트가 관리자 화면을 보여줄지 정하는 용도다 (관리자 API는 서버가 따로 403으로 막는다). 이름·학년·부전공·프로필 사진은 마이페이지 디자인 확정 후 추가 예정(필드 추가만 하므로 기존 클라이언트는 영향 없음)
- 비밀번호 찾기: 서버는 `verify-code` 통과를 기억하지 않는다 — 화면에서 다음 단계로 넘어가는 용도이고, 재설정 요청(`/api/auth/password-reset`)에도 **같은 `code`를 다시 보내야** 한다
- 비밀번호 찾기 코드 확인·재설정의 실패는 모두 400이고 `code`로 구분한다: 불일치 `PASSWORD_RESET_CODE_MISMATCH`(`message`에 n/5회), 5번째 불일치 `PASSWORD_RESET_CODE_ATTEMPTS_EXCEEDED`(코드 폐기, 다시 발송받아야 함), 만료 `PASSWORD_RESET_CODE_EXPIRED`, 코드 없음·없는 학번 `INVALID_VERIFICATION_CODE`. 틀린 횟수는 `verify-code`와 재설정 API가 합쳐서 센다
- 비밀번호 재설정: 지금 비밀번호와 같으면 400 `PASSWORD_RESET_SAME_PASSWORD`이고 코드는 소모되지 않아 다른 비밀번호로 다시 보낼 수 있다. 재설정 성공 시 기존 로그인(refresh token)은 모두 무효화하고, 로그인 잠금과 실패 횟수도 지운다
- 이메일 등록/변경: 코드는 **새 이메일**로 발송. 현재 이메일과 같으면 400, 이미 쓰는 이메일이면 409, 60초 안에 재요청하면 429, 발송 실패는 503. 변경 시 코드를 받은 이메일과 같은 `newEmail`이어야 하고, 현재 비밀번호가 틀리면 400(`CURRENT_PASSWORD_MISMATCH`). 이메일이 없는 기존 계정도 같은 API로 처음 등록
- 로그인/재발급 응답의 `refreshToken`은 studentId처럼 민감정보 취급 — 화면에 노출하지 말고 저장 용도로만 사용
- 아직 인증 필터가 없어서(작업 중), 로그인 없이도 모든 API가 열려있는 상태. 추후 로그인 필요한 API가 생기면 이 문서에 표시 예정

## 2. 교수 정보 (professor)

목록/상세 조회는 인증 도메인과 동일하게 `ApiResponse`로 감싸서 응답한다 (course 도메인과 다름).

| Method | Path                                       | 설명        | Request | Response                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 상태   |
|--------|--------------------------------------------|-----------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------|
| GET    | /api/professors                            | 교수 목록 조회  | N/A     | 200 <br/> { "success": true, <br/> "data": [{ <br/> "professorId": 0, <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> professorRoom: "000" <br/> professorCall: "032-000-0000" }] }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | 구현완료 |
| GET    | /api/professors/{professorId}              | 교수 상세 조회  | N/A     | 200 <br/> { "success": true, <br/> "data": { <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> "professorDetail": "\~", <br/> professorRoom: "000" <br/> professorCall: "032-000-0000", <br/> "labDetail": { <br/>"labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://...", <br/> "labDetail": "\~", <br/> labStudy: [<br/> "OOO" <br/>]}, <br/> "courseList": [{ <br/> "courseId": 0, <br/> "courseName": "...", <br/> "professorName": "...", <br/> "grade": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "semester": "FIRST\|SECOND", <br/> "credits": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "courseCode": "...", <br/> "courseType": "MAJOR_CORE\|MAJOR_FOUNDATION\|MAJOR_ADVANCED" <br/>}]}} | 구현완료 |
| GET    | /api/professors/search(?professorName=...) | 교수 목록 검색  | N/A     | 200 <br/> { "success": true, <br/> "data": [{ <br/> "professorId": 0, <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> professorRoom: "000" <br/> professorCall: "032-000-0000" }] }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | 구현완료 |

[//]: # (| POST   | /api/professors/{professorId}/professor-comments                      | 교수 댓글 작성 | { <br/> "professorCommentRate": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 구현완료 |)

[//]: # (| PATCH  | /api/professors/{professorId}/professor-comments/{professorCommentId} | 교수 댓글 수정 | { <br/> "professorCommentRate": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 구현완료 |)

[//]: # (| DELETE | /api/professors/{professorId}/professor-comments/{professorCommentId} | 교수 댓글 삭제 | N/A                                                                                                                              | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 구현완료 |)

## 3. 전공 수업 (course)
다른 도메인과 동일하게 `ApiResponse`로 감싸서 응답한다.

과목(Course, 학기 안 타는 정보)과 개설강의(CourseOffering, 학기별 정보)가 분리돼 있다. 자세한 배경은 [course-architecture.md](course-architecture.md) 참고.

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| GET | /api/courses | 수업 목록 조회 (이번 학기 개설 강의 기준). `keyword`가 있으면 강의명·교수명 부분 일치 또는 초성 검색(공백 무시) | Query: `keyword` (선택, 최대 50자, 빈 값이면 전체 조회), `page` (기본 0, 0~10000), `size` (기본 10, 1~50) | 200 <br/> { "success": true, "data": { <br/> "courses": [{ <br/> "courseId": 0, <br/> "courseName": "...", <br/> "professorName": "...", <br/> "grade": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "semester": "FIRST\|SECOND", <br/> "credits": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "courseCode": "...", <br/> "courseType": "MAJOR_CORE\|MAJOR_FOUNDATION\|MAJOR_ADVANCED" <br/>}], <br/> "page": 0, "size": 10, "totalElements": 23, "totalPages": 3, "hasNext": true <br/>} } | 구현완료 |
| GET | /api/courses/{courseId} | 수업 상세 조회 | N/A | 200 <br/> { "success": true, "data": { <br/> "courseId": 0, <br/> "courseCode": "...", <br/> "courseName": "...", <br/> "courseDetail": "...", <br/> "offerings": [{ <br/> "courseOfferingId": 0, <br/> "professor": { "professorId": 0, "professorName": "...", "professorImageUrl": "https://..." }, <br/> "academicYear": 2026, <br/> "semester": "...", <br/> "grade": "...", <br/> "credits": "...", <br/> "courseTime": "...", <br/> "courseType": "...", <br/> "evaluationType": "ABSOLUTE\|RELATIVE", <br/> "isOnline": "BLENDED_LEARNING\|ONLINE\|OFFLINE" <br/>}] <br/>} } | 구현완료 |
| POST | /api/courses/{courseId}/reviews | 수업 후기 작성 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "professorId": 1, <br/> "academicYear": "YEAR_2023\|YEAR_2024\|YEAR_2025\|YEAR_2026", <br/> "semester": "FIRST\|SECOND", <br/> "rating": "ONE~FIVE", <br/> "content": "20~1000자", <br/> "gradingType": "VERY_GENEROUS\|GENEROUS\|NORMAL\|STRICT\|VERY_STRICT", <br/> "groupActivity": "NONE\|SOMETIMES\|ALWAYS", <br/> "examCount": "ZERO\|ONE\|TWO\|THREE_OR_MORE", <br/> "assignmentFrequency": "NONE\|LOW\|NORMAL\|HIGH", <br/> "quizFrequency": "NONE\|LOW\|NORMAL\|HIGH", <br/> "attendance": "NONE\|ELECTRONIC\|ROLL_CALL\|ASSIGNMENT_OR_QUIZ\|ONLINE_RECORD\|MULTIPLE" <br/>} <br/> **전부 필수**. 수강 연도·학기는 작성 정보이며 개설 연도·학기 대조·후기 필터에는 사용하지 않음 | 200 <br/> { "success": true, "data": "과목 후기가 등록되었습니다." } <br/> 필수값 누락·허용하지 않는 Enum·길이 오류 400(`errors`에 필드) / 과목·교수 담당 이력 없음 404 `COURSE_OFFERING_NOT_FOUND` / 같은 과목+교수에 이미 작성 409 `DUPLICATE_COURSE_REVIEW` | 구현완료 |
| PUT | /api/courses/{courseId}/reviews/{reviewId} | 본인이 작성한 수업 후기 수정 (로그인 필요, 교수·수강 시기 변경 불가) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> 작성 요청에서 `professorId`, `academicYear`, `semester`를 제외한 평가 필드 필수 | 200 <br/> { "success": true, "data": "과목 후기가 수정되었습니다." } <br/> 없는 후기 404 / 다른 사용자의 후기 403 | 구현완료 |
| DELETE | /api/courses/{courseId}/reviews/{reviewId} | 본인이 작성한 수업 후기 삭제 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": "과목 후기가 삭제되었습니다." } <br/> 없는 후기 404 / 다른 사용자의 후기 403 | 구현완료 |
| GET | /api/courses/{courseId}/reviews | 수업 후기 조회 | Query: `sort` (`LATEST` 기본, `RECOMMENDED`, `RATING`) | 200 <br/> { "success": true, "data": [{ <br/> "reviewId": 1, "rating": "FOUR", "content": "...", <br/> "academicYear": "YEAR_2026", "semester": "FIRST", <br/> "updatedAt": "...", "createdAt": "..." <br/>}] } | 구현완료 |
| GET | /api/courses/{courseId}/reviews/summary | 교수별 후기 요약 조회 | Query: `professorId` (필수) | 200 <br/> { "success": true, "data": { <br/> "reviewCount": 5, "summaryAvailable": true, "minimumReviewCount": 3, <br/> "averageRating": 4.2, <br/> "gradingTypeAverage": 3.8, "gradingType": "GENEROUS", <br/> "groupActivityAverage": 1.2, "groupActivity": "SOMETIMES", <br/> "examCountAverage": 1.6, "examCount": "TWO", <br/> "assignmentFrequencyAverage": 2.4, "assignmentFrequency": "NORMAL", <br/> "quizFrequencyAverage": 1.2, "quizFrequency": "LOW", <br/> "attendance": "ELECTRONIC" <br/>} } <br/> 후기 3개 미만이면 `summaryAvailable=false`와 요약 필드 `null`. 출결 최빈값 동률이면 한글 표시명의 가나다순으로 선택 | 구현완료 |

## 3-1. 학습자료 (material)
강의 상세의 "학습자료" 탭 (와이어프레임 10번, #187). **모든 API가 로그인 필요**하다 (조회 포함).

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| POST | /api/courses/{courseId}/materials | 학습자료 업로드 | Header: <br/> `Authorization: Bearer {accessToken}` <br/> `multipart/form-data` <br/> `title`: 5~50자 <br/> `content`: 10~500자 <br/> `materialTypes`: 1개 이상, 같은 이름으로 여러 번 (`LECTURE_NOTE` 강의 필기 / `SUMMARY` 개념·요약 정리 / `PROBLEM_SOLVING` 문제·풀이 / `OTHER` 기타) <br/> `file`: 1개 (pdf·jpg·png·pptx·docx·xlsx·hwp, 최대 20MB) | 201 <br/> { "success": true, "data": { "materialId": 1 } } | 구현완료 |
| GET | /api/courses/{courseId}/materials?page=0&size=20 | 학습자료 목록 (내 글 맨 위, 그다음 최신순) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> Query: `page` 0부터, `size` 1~50 (기본 20) | 200 <br/> { "success": true, "data": { <br/> "totalCount": 3, "hasNext": false, <br/> "materials": [{ "materialId": 1, "title": "...", "nickname": "바다", <br/> "fileType": "pdf", "createdAt": "2026-11-18T10:00:00", "isMine": true }] <br/>} } | 구현완료 |
| GET | /api/materials/{materialId} | 학습자료 상세 | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": { <br/> "materialId": 1, "title": "...", "materialTypes": ["SUMMARY"], "nickname": "바다", <br/> "createdAt": "...", "updatedAt": "...", "content": "...", <br/> "fileName": "중간고사 정리.pdf", "fileType": "pdf", "fileSize": 1048576, "isMine": true <br/>} } | 구현완료 |
| GET | /api/materials/{materialId}/download | 내려받기 주소 (5분 만료, 원래 파일명으로 저장) | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": { "downloadUrl": "https://..." } } | 구현완료 |
| DELETE | /api/materials/{materialId} | 학습자료 삭제 (작성자만, 첨부 파일도 삭제) | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": null } | 구현완료 |

- 업로드 실패: 입력값 400 `INVALID_INPUT_VALUE`(`errors`에 `title`·`content`·`materialTypes`), 파일 없음 400 `FILE_EMPTY`, 형식 400 `FILE_TYPE_NOT_ALLOWED`(`message`에 허용 형식), 20MB 초과 413 `FILE_TOO_LARGE`, 저장 실패 503 `FILE_UPLOAD_FAILED`, 없는 과목 404 `COURSE_NOT_FOUND`
- 목록의 `fileType`은 "형식" 칸, `isMine`은 삭제 버튼 표시용. 무한 스크롤은 `hasNext`가 false가 될 때까지 `page`를 올린다. 없는 과목이면 404
- `fileSize`는 바이트 단위다 (MB 표시는 화면에서 계산)
- 내려받기 주소는 5분 뒤 만료된다. 버튼을 누를 때마다 새로 받는다. 주소를 만들지 못하면 503 `FILE_URL_FAILED`
- 삭제: 다른 사람의 자료 403 `MATERIAL_NOT_OWNER`, 없는 자료 404 `MATERIAL_NOT_FOUND`
- 수정 기능은 없다 (화면 없음)

## 4. 커뮤니티 (community)
| Method | Path | 설명 | 상태 |
|---|---|---|---|

## 5. 진로 (career)
| Method | Path | 설명 | 상태 |
|---|---|---|---|

## 6. 마이페이지 (mypage)
| Method | Path | 설명 | 상태 |
|---|---|---|---|

## 토큰
로그인(`POST /api/auth/login`)하면 `accessToken`과 `refreshToken`을 받는다. 로그인이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더로 호출한다 (Swagger에서 자물쇠가 있는 API).

| 토큰 | 유효 기간 | 설정 |
|---|---|---|
| access token | 1시간 | `jwt.expiration` |
| refresh token | 14일 | `jwt.refresh-expiration` |

- **재발급**(`POST /api/auth/reissue`, body에 `refreshToken`): access·refresh를 **둘 다 새로** 받는다. 받은 refresh로 바꿔 저장해야 한다 — 이전 refresh는 그 순간 무효가 된다.
- **사용자당 refresh token은 1개**다. 다른 기기에서 로그인하거나 재발급하면 이전 refresh는 무효가 된다.
- **비밀번호 변경·비밀번호 재설정에 성공하면** 서버의 refresh가 지워져서 모든 기기에서 다시 로그인해야 한다.
- **로그인이 필요 없는 API**는 토큰이 만료·위조돼도 무시하고 정상 응답한다 (401이 나지 않는다).
- **로그아웃**(`POST /api/auth/logout`)은 서버의 refresh를 지운다. **body에 `refreshToken`을 보내는 방식을 권장**한다 — access token이 만료됐어도 로그아웃된다 (#129).
  - 예전 방식(`Authorization` 헤더만)도 계속 받지만, access token이 만료된 상태면 사용자를 특정하지 못해 **아무것도 지우지 못한다** (refresh가 만료까지 서버에 남음).
  - 이미 로그아웃됐거나 교체된 예전 refresh를 보내도 200이다 (다른 기기의 현재 로그인은 끊지 않는다). 둘 다 없으면 400 `INVALID_INPUT_VALUE`.
  - 프론트는 응답과 상관없이 기기의 토큰을 지운다.

### 401을 받았을 때 (프론트 인터셉터 기준)
`code`로 구분한다. 401이라고 모두 재발급 대상이 아니다.

| 응답 | 의미 | 프론트 처리 |
|---|---|---|
| 401 `AUTHENTICATION_REQUIRED` | access token 없음·만료·위조, 또는 탈퇴·삭제된 사용자의 토큰 (서로 구분하지 않음) | `reissue` 후 원래 요청 재시도. `reissue`도 실패하면 로그아웃 처리 |
| 401 `INVALID_REFRESH_TOKEN` | `reissue` 실패 — refresh 없음·만료·위조·이미 교체됨 | 로그아웃 처리 (재시도하지 않음) |
| 401 `INVALID_CREDENTIALS` | 로그인 비밀번호 틀림. `message`에 실패 횟수 포함 (예: `(2/5회)`) | 로그인 폼에 오류 표시. **재발급 대상 아님** |
| 400 `CURRENT_PASSWORD_MISMATCH` | 비밀번호·이메일 변경 때 현재 비밀번호 틀림 | 폼에 오류 표시. **401이 아니다** (예전엔 401이었음, #115) |
| 403 `FORBIDDEN`, `PROFESSOR_COMMENT_FORBIDDEN` | 로그인은 됐지만 권한 없음 (예: 남의 교수 후기 수정) | 재발급하지 않는다 |

## 에러 코드
공통 응답 포맷: `{ "success": false, "data": null, "code": "...", "message": "...", "errors"?: [...] }` (`errors`는 필드별 오류가 있을 때만)

- **프론트는 `code`로 분기한다.** `message`는 사용자에게 보여줄 문구라 바뀔 수 있다.
- **필드별 오류 `errors`** (#111): 입력값 문제로 400이 날 때, 어느 필드가 왜 잘못됐는지 `errors: [{ "field", "message" }]`로 함께 내려간다. 해당 없는 에러에는 `errors` 키 자체가 없다.
  - `message`는 대표 문구(첫 번째 오류)라서, 폼 칸마다 표시하려면 `errors`를 쓴다. 여러 필드가 틀리면 전부 들어 있다.
  - `field`는 JSON 필드 이름(`studentId`, 중첩이면 `reviews[0].rating`)이거나 경로 변수·쿼리 파라미터 이름(`courseId`, `sort`)이다.
  - enum 값이 틀리면 허용 값을 알려준다: `'rating' 값은 ONE, TWO, THREE, FOUR, FIVE 중 하나여야 합니다`
  - body가 없거나 JSON이 깨지면 `errors` 없이 `요청 본문이 비어 있거나 JSON 형식이 올바르지 않습니다`

```json
{
  "success": false,
  "code": "INVALID_INPUT_VALUE",
  "message": "학번은 필수입니다.",
  "errors": [
    { "field": "studentId", "message": "학번은 필수입니다." },
    { "field": "email", "message": "이메일은 필수입니다." }
  ]
}
```
- 상황 하나에 code 하나. HTTP 상태는 응답 status에 있으므로 code에 넣지 않는다 (#115).
- 새 에러를 추가할 때도 이 규칙을 따르고 이 표에 한 줄 추가한다.
- 이 표에 없는 4xx는 나가지 않는 것을 원칙으로 한다. 새 4xx가 필요해지면 그 기능 PR에서 전용 code를 추가한다 (#131).

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT_VALUE` | 요청 값 검증 실패, 잘못된 JSON·타입, 필수 헤더·쿼리 파라미터 누락 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인이 필요한 API에 토큰 없음/만료/위조 → **토큰 재발급 시도** |
| 403 | `FORBIDDEN` | 권한 부족 |
| 404 | `API_NOT_FOUND` | 존재하지 않는 경로 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 406 | `NOT_ACCEPTABLE` | `Accept`가 JSON이 아님 (예: `application/xml`) |
| 409 | `DUPLICATE_RESOURCE` | 동시 요청으로 DB UNIQUE 제약에 걸림 (가입 더블클릭 등). 사전 확인에 걸리면 도메인 코드(`DUPLICATE_NICKNAME` 등)가 나가고, 확인과 저장 사이에 다른 요청이 끼어든 경우에만 이 코드 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `Content-Type`이 없거나 API가 받는 형식이 아님 (대부분 JSON, 회원가입은 `multipart/form-data`. `message`에 받는 형식 안내) |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
| 400 | `FILE_EMPTY` | 업로드할 파일이 없거나 비어 있음 |
| 400 | `FILE_TYPE_NOT_ALLOWED` | 허용하지 않는 파일 형식, 또는 확장자·`Content-Type`·실제 내용이 서로 다름. `message`에 허용 형식 포함 (예: `pdf, jpg 파일만 올릴 수 있어요`) |
| 413 | `FILE_TOO_LARGE` | 파일 용량 초과. API별 한도를 넘으면 `message`에 한도 포함 (예: `10MB 이하 파일만 올릴 수 있어요`), 서버 전체 한도(10MB)를 넘으면 기본 문구 |
| 503 | `FILE_UPLOAD_FAILED` | 파일 저장소(S3) 업로드 실패 |
| 503 | `FILE_URL_FAILED` | 파일 열람 주소(presigned URL) 생성 실패 |
| 400 | `INVALID_VERIFICATION_CODE` | 이메일 변경 인증코드 없음/만료/불일치(사유는 구분하지 않음), 비밀번호 찾기 코드 없음/없는 학번 |
| 400 | `PASSWORD_RESET_CODE_MISMATCH` | 비밀번호 찾기 인증번호 불일치. `message`에 n/5회 포함 |
| 400 | `PASSWORD_RESET_CODE_ATTEMPTS_EXCEEDED` | 비밀번호 찾기 인증번호 5회 불일치로 인증 중단 |
| 400 | `PASSWORD_RESET_CODE_EXPIRED` | 비밀번호 찾기 인증번호 만료 |
| 400 | `PASSWORD_RESET_SAME_PASSWORD` | 재설정할 비밀번호가 지금 비밀번호와 같음 |
| 400 | `SAME_EMAIL` | 현재 이메일과 같은 이메일로 변경 요청 |
| 400 | `CURRENT_PASSWORD_MISMATCH` | 비밀번호·이메일 변경 시 현재 비밀번호 불일치 (401 아님 — 재발급 대상 아님) |
| 401 | `INVALID_CREDENTIALS` | 로그인 비밀번호 틀림. `message`에 실패 횟수 포함 (예: `(2/5회)`) |
| 401 | `INVALID_REFRESH_TOKEN` | refresh token 무효/만료 → **로그아웃 처리** |
| 404 | `USER_NOT_FOUND` | 존재하지 않는 사용자 |
| 404 | `STUDENT_ID_NOT_REGISTERED` | 로그인 시 가입되지 않은 학번 |
| 409 | `DUPLICATE_STUDENT_ID` | 이미 가입된 학번 |
| 409 | `DUPLICATE_NICKNAME` | 이미 사용 중인 닉네임 |
| 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일 |
| 404 | `PASSWORD_RESET_ACCOUNT_NOT_FOUND` | 비밀번호 찾기에서 학번·이메일이 일치하는 계정 없음 |
| 429 | `VERIFICATION_CODE_RESEND_TOO_FAST` | 인증코드 재발송 60초 제한 |
| 429 | `PASSWORD_RESET_RESEND_LIMIT_EXCEEDED` | 비밀번호 찾기 재발송 하루 5회 초과 |
| 429 | `LOGIN_LOCKED` | 로그인 5회 실패로 5분 잠금. 남은 시간(초)은 `Retry-After` 헤더 |
| 403 | `VERIFICATION_PENDING` | 승인 대기 계정의 로그인·토큰 재발급. `data.requestedAt`에 신청일 |
| 403 | `VERIFICATION_REJECTED` | 반려 계정의 로그인·토큰 재발급. `data.rejectionReason`에 반려 사유(사유가 필수가 되기 전에 반려된 계정은 없을 수 있음), `data.nickname`에 닉네임 |
| 409 | `VERIFICATION_NOT_REJECTED` | 반려 상태가 아닌데 서류를 다시 제출함 (이미 검토 중이거나 승인됨) |
| 409 | `VERIFICATION_ALREADY_PROCESSED` | 관리자가 승인 대기가 아닌(이미 승인·거절된) 가입 신청을 처리하려 함 |
| 503 | `EMAIL_SEND_FAILED` | 메일 발송 실패 |
| 404 | `PROFESSOR_NOT_FOUND` | 존재하지 않는 교수 |
| 404 | `COURSE_NOT_FOUND` | 존재하지 않는 과목 |
| 404 | `COURSE_OFFERING_NOT_FOUND` | 해당 교수가 개설한 과목이 아님 |
| 409 | `DUPLICATE_COURSE_REVIEW` | 같은 과목+교수+연도+학기에 이미 강의평을 작성함 (동시 요청이 사전 확인을 통과하면 `DUPLICATE_RESOURCE`) |
| 404 | `MATERIAL_NOT_FOUND` | 존재하지 않는 학습자료 |
| 403 | `MATERIAL_NOT_OWNER` | 다른 사람이 올린 학습자료를 삭제하려 함 |
| 404 | `COURSE_REVIEW_NOT_FOUND` | 존재하지 않거나 URL의 과목과 일치하지 않는 강의평 |

[//]: # (| 404 | `PROFESSOR_COMMENT_NOT_FOUND` | 존재하지 않는 교수 후기 |)

[//]: # (| 403 | `PROFESSOR_COMMENT_FORBIDDEN` | 본인이 쓴 교수 후기가 아님 |)

[//]: # (| 404 | `PROFESSOR_COMMENT_PROFESSOR_MISMATCH` | URL의 교수와 후기의 교수가 다름 &#40;작성자 확인이 먼저라 남의 후기면 403이 먼저 나감&#41; |)

## 작성 규칙
- 엔드포인트 구현 시 "상태"를 "구현완료"로 바꾸고 요청/응답 예시를 아래에 추가한다.
- 실제 상세 스펙은 springdoc-openapi(Swagger UI)가 소스이므로, 이 문서는 전체 그림을 빠르게 훑기 위한 목차 용도로만 쓴다.
