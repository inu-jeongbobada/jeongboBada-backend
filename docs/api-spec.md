# API 명세

개발 우선순위(AGENTS.md)에 맞춰 도메인별로 채워 나간다.

## 1. 인증 (user)
공통 응답 포맷: `{ "success": boolean, "data": T, "code": string | null, "message": string | null, "errors"?: [{ "field", "message" }] }`
(`success: false`일 때 `code`/`message`에 에러 정보 — 아래 [에러 코드](#에러-코드) 표 참고)

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| GET | /api/auth/check-nickname?nickname= | 회원가입 폼에서 닉네임 사용 가능 여부 실시간 확인 | Query: <br/> `nickname` | 200 <br/> { "success": true, "data": { "available": true } } | 구현완료 |
| POST | /api/auth/signup | 회원가입 (학번 기반) | { <br/> "studentId": "202012345", <br/> "password": "영문+숫자 8~64자", <br/> "nickname": "한글/영문/숫자/-/_  2~10자", <br/> "email": "user@example.com" <br/>} | 201 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/login | 로그인 (JWT 발급) | { <br/> "studentId": "202012345", <br/> "password": "..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/reissue | Access Token 재발급 | { <br/> "refreshToken": "eyJ..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/logout | 로그아웃 | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset/send-code | 비밀번호 찾기 ① 인증코드 발송 | { <br/> "studentId": "202012345", <br/> "email": "가입 때 등록한 이메일" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset/verify-code | 비밀번호 찾기 ② 인증코드 확인 (코드는 소모되지 않음) | { <br/> "studentId": "202012345", <br/> "code": "숫자 6자리" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset | 비밀번호 찾기 ③ 비밀번호 재설정 (코드 1회 소모) | { <br/> "studentId": "202012345", <br/> "code": "숫자 6자리", <br/> "newPassword": "영문+숫자 8~64자" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/users/me/email/send-code | 이메일 등록/변경 ① 새 이메일로 인증코드 발송 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "newEmail": "new@example.com" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| PATCH | /api/users/me/email | 이메일 등록/변경 ② 코드+현재 비밀번호 확인 후 변경 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "newEmail": "new@example.com", <br/> "code": "숫자 6자리", <br/> "currentPassword": "..." <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |

- 닉네임 중복확인은 **닉네임만 제공**한다. 학번/이메일 중복확인은 제공하지 않음 (이 학번/이메일로 가입돼 있다는 걸 누구나 조회할 수 있게 되는 user enumeration 방지) — 가입 시도 시 409로만 안내. 확인 시점과 실제 가입 시점 사이에 다른 사람이 선점할 수 있어 최종 검증은 `signup`에서 다시 함
- 회원가입에 `email`이 **필수**로 추가됨 (비밀번호 찾기용). 이미 가입된 이메일이면 409. 대소문자/앞뒤 공백은 무시하고 비교
- 비밀번호 찾기: `send-code`는 **학번이 없거나 이메일이 달라도, 메일 발송이 실패해도 항상 같은 200** (가입 여부 노출 방지). 인증코드는 6자리, 5분 유효, 재발송은 60초 뒤부터
- 비밀번호 찾기: 서버는 `verify-code` 통과를 기억하지 않는다 — 화면에서 다음 단계로 넘어가는 용도이고, 재설정 요청(`/api/auth/password-reset`)에도 **같은 `code`를 다시 보내야** 한다
- 인증코드가 없음/만료/불일치/없는 학번인 경우 모두 400 `INVALID_VERIFICATION_CODE`로 동일하게 응답. 5번 틀리면 코드가 폐기되어 다시 발송받아야 함. 재설정 성공 시 기존 로그인(refresh token)은 모두 무효화
- 이메일 등록/변경: 코드는 **새 이메일**로 발송. 현재 이메일과 같으면 400, 이미 쓰는 이메일이면 409, 60초 안에 재요청하면 429, 발송 실패는 503. 변경 시 코드를 받은 이메일과 같은 `newEmail`이어야 하고, 현재 비밀번호가 틀리면 400(`CURRENT_PASSWORD_MISMATCH`). 이메일이 없는 기존 계정도 같은 API로 처음 등록
- 로그인/재발급 응답의 `refreshToken`은 studentId처럼 민감정보 취급 — 화면에 노출하지 말고 저장 용도로만 사용
- 아직 인증 필터가 없어서(작업 중), 로그인 없이도 모든 API가 열려있는 상태. 추후 로그인 필요한 API가 생기면 이 문서에 표시 예정

## 2. 교수 정보 (professor)

목록/상세 조회는 인증 도메인과 동일하게 `ApiResponse`로 감싸서 응답한다 (course 도메인과 다름).

| Method | Path                                                                  | 설명       | Request                                                                                                                          | Response                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | 상태   |
|--------|-----------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------|
| GET    | /api/professors                                                       | 교수 목록 조회 | N/A                                                                                                                              | 200 <br/> { "success": true, "data": [{ <br/> "professorId": 0, <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> "labList": { <br/>"labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://..." <br/>} <br/>}] }                                                                                                                                                                                                                                                                                          | 구현완료 |
| GET    | /api/professors/{professorId}                                         | 교수 상세 조회 | N/A                                                                                                                              | 200 <br/> { "success": true, "data": { <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> "professorDetail": "\~", <br/> "labDetail": { <br/>"labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://...", <br/> "labDetail": "\~" <br/>}, <br/> "professorCommentDetails": [{ <br/> "professorCommentId": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentRate": 0, <br/> "professorCommentDate": "YYYY-MM-DDTHH:mm:ss", <br/> "professorCommentAnonymity": "TRUE\|FALSE" <br/>}] <br/>} } | 구현완료 |
| POST   | /api/professors/{professorId}/professor-comments                      | 교수 댓글 작성 | { <br/> "professorCommentRate": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 구현완료 |
| PATCH  | /api/professors/{professorId}/professor-comments/{professorCommentId} | 교수 댓글 수정 | { <br/> "professorCommentRate": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 구현완료 |
| DELETE | /api/professors/{professorId}/professor-comments/{professorCommentId} | 교수 댓글 삭제 | N/A                                                                                                                              | N/A                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 구현완료 |

## 3. 전공 수업 (course)
다른 도메인과 동일하게 `ApiResponse`로 감싸서 응답한다.

과목(Course, 학기 안 타는 정보)과 개설강의(CourseOffering, 학기별 정보)가 분리돼 있다. 자세한 배경은 [course-architecture.md](course-architecture.md) 참고.

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| GET | /api/courses | 수업 목록 조회 (이번 학기 개설 강의 기준) | N/A | 200 <br/> { "success": true, "data": [{ <br/> "courseId": 0, <br/> "courseName": "...", <br/> "professorName": "...", <br/> "grade": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "semester": "FIRST\|SECOND", <br/> "credits": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "courseCode": "...", <br/> "courseType": "MAJOR_CORE\|MAJOR_FOUNDATION\|MAJOR_ADVANCED" <br/>}] } | 구현완료 |
| GET | /api/courses/{courseId} | 수업 상세 조회 | N/A | 200 <br/> { "success": true, "data": { <br/> "courseId": 0, <br/> "courseCode": "...", <br/> "courseName": "...", <br/> "courseDetail": "...", <br/> "offerings": [{ <br/> "courseOfferingId": 0, <br/> "professor": { "professorId": 0, "professorName": "...", "professorImageUrl": "https://..." }, <br/> "academicYear": 2026, <br/> "semester": "...", <br/> "grade": "...", <br/> "credits": "...", <br/> "courseTime": "...", <br/> "courseType": "...", <br/> "evaluationType": "ABSOLUTE\|RELATIVE", <br/> "isOnline": "BLENDED_LEARNING\|ONLINE\|OFFLINE" <br/>}] <br/>} } | 구현완료 |
| GET | /api/courses/{id}/reviews | 수업 후기 조회 (학점별 필터) | N/A | N/A | 미구현 |

## 4. 커뮤니티 (community)
| Method | Path | 설명 | 상태 |
|---|---|---|---|

## 5. 진로 (career)
| Method | Path | 설명 | 상태 |
|---|---|---|---|

## 6. 마이페이지 (mypage)
| Method | Path | 설명 | 상태 |
|---|---|---|---|

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

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT_VALUE` | 요청 값 검증 실패, 잘못된 JSON·타입, 필수 헤더·쿼리 파라미터 누락 |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인이 필요한 API에 토큰 없음/만료/위조 → **토큰 재발급 시도** |
| 403 | `FORBIDDEN` | 권한 부족 |
| 404 | `API_NOT_FOUND` | 존재하지 않는 경로 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 406 | `NOT_ACCEPTABLE` | `Accept`가 JSON이 아님 (예: `application/xml`) |
| 409 | `DUPLICATE_RESOURCE` | 동시 요청으로 DB UNIQUE 제약에 걸림 (가입 더블클릭 등). 사전 확인에 걸리면 도메인 코드(`DUPLICATE_NICKNAME` 등)가 나가고, 확인과 저장 사이에 다른 요청이 끼어든 경우에만 이 코드 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | JSON을 받는 API에 `Content-Type`이 없거나 JSON이 아님 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |
| 400 | `INVALID_VERIFICATION_CODE` | 인증코드 없음/만료/불일치 (사유는 구분하지 않음) |
| 400 | `SAME_EMAIL` | 현재 이메일과 같은 이메일로 변경 요청 |
| 400 | `CURRENT_PASSWORD_MISMATCH` | 비밀번호·이메일 변경 시 현재 비밀번호 불일치 (401 아님 — 재발급 대상 아님) |
| 401 | `INVALID_CREDENTIALS` | 로그인 실패 (학번 없음과 비밀번호 틀림을 구분하지 않음) |
| 401 | `INVALID_REFRESH_TOKEN` | refresh token 무효/만료 → **로그아웃 처리** |
| 404 | `USER_NOT_FOUND` | 존재하지 않는 사용자 |
| 409 | `DUPLICATE_STUDENT_ID` | 이미 가입된 학번 |
| 409 | `DUPLICATE_NICKNAME` | 이미 사용 중인 닉네임 |
| 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일 |
| 429 | `VERIFICATION_CODE_RESEND_TOO_FAST` | 인증코드 재발송 60초 제한 |
| 503 | `EMAIL_SEND_FAILED` | 메일 발송 실패 |
| 404 | `PROFESSOR_NOT_FOUND` | 존재하지 않는 교수 |
| 404 | `COURSE_NOT_FOUND` | 존재하지 않는 과목 |
| 404 | `COURSE_OFFERING_NOT_FOUND` | 해당 교수가 개설한 과목이 아님 |
| 404 | `PROFESSOR_COMMENT_NOT_FOUND` | 존재하지 않는 교수 후기 |
| 403 | `PROFESSOR_COMMENT_FORBIDDEN` | 본인이 쓴 교수 후기가 아님 |
| 404 | `PROFESSOR_COMMENT_PROFESSOR_MISMATCH` | URL의 교수와 후기의 교수가 다름 (작성자 확인이 먼저라 남의 후기면 403이 먼저 나감) |

## 작성 규칙
- 엔드포인트 구현 시 "상태"를 "구현완료"로 바꾸고 요청/응답 예시를 아래에 추가한다.
- 실제 상세 스펙은 springdoc-openapi(Swagger UI)가 소스이므로, 이 문서는 전체 그림을 빠르게 훑기 위한 목차 용도로만 쓴다.
