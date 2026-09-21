# API 명세

개발 우선순위(CLAUDE.md)에 맞춰 도메인별로 채워 나간다.

## 1. 인증 (user)
공통 응답 포맷: `{ "success": boolean, "data": T, "code": string | null, "message": string | null }`
(`success: false`일 때 `code`/`message`에 에러 정보)

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| POST | /api/auth/signup | 회원가입 (학번 기반) | { <br/> "studentId": "202012345", <br/> "password": "영문+숫자 8~64자", <br/> "nickname": "한글/영문/숫자/-/_  2~10자", <br/> "email": "user@example.com" <br/>} | 201 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/login | 로그인 (JWT 발급) | { <br/> "studentId": "202012345", <br/> "password": "..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/reissue | Access Token 재발급 | { <br/> "refreshToken": "eyJ..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/logout | 로그아웃 | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset/send-code | 비밀번호 찾기 ① 인증코드 발송 | { <br/> "studentId": "202012345", <br/> "email": "가입 때 등록한 이메일" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset/verify-code | 비밀번호 찾기 ② 인증코드 확인 (코드는 소모되지 않음) | { <br/> "studentId": "202012345", <br/> "code": "숫자 6자리" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/password-reset | 비밀번호 찾기 ③ 비밀번호 재설정 (코드 1회 소모) | { <br/> "studentId": "202012345", <br/> "code": "숫자 6자리", <br/> "newPassword": "영문+숫자 8~64자" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/users/me/email/send-code | 이메일 등록/변경 ① 새 이메일로 인증코드 발송 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "newEmail": "new@example.com" <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |
| PATCH | /api/users/me/email | 이메일 등록/변경 ② 코드+현재 비밀번호 확인 후 변경 (로그인 필요) | Header: <br/> `Authorization: Bearer {accessToken}` <br/> { <br/> "newEmail": "new@example.com", <br/> "code": "숫자 6자리", <br/> "currentPassword": "..." <br/>} | 200 <br/> { "success": true, "data": null } | 구현완료 |

- 회원가입에 `email`이 **필수**로 추가됨 (비밀번호 찾기용). 이미 가입된 이메일이면 409. 대소문자/앞뒤 공백은 무시하고 비교
- 비밀번호 찾기: `send-code`는 **학번이 없거나 이메일이 달라도, 메일 발송이 실패해도 항상 같은 200** (가입 여부 노출 방지). 인증코드는 6자리, 5분 유효, 재발송은 60초 뒤부터
- 비밀번호 찾기: 서버는 `verify-code` 통과를 기억하지 않는다 — 화면에서 다음 단계로 넘어가는 용도이고, 재설정 요청(`/api/auth/password-reset`)에도 **같은 `code`를 다시 보내야** 한다
- 인증코드가 없음/만료/불일치/없는 학번인 경우 모두 400 `USER_400`으로 동일하게 응답. 5번 틀리면 코드가 폐기되어 다시 발송받아야 함. 재설정 성공 시 기존 로그인(refresh token)은 모두 무효화
- 이메일 등록/변경: 코드는 **새 이메일**로 발송. 현재 이메일과 같으면 400, 이미 쓰는 이메일이면 409, 60초 안에 재요청하면 429, 발송 실패는 503. 변경 시 코드를 받은 이메일과 같은 `newEmail`이어야 하고, 현재 비밀번호가 틀리면 401. 이메일이 없는 기존 계정도 같은 API로 처음 등록
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

## 작성 규칙
- 엔드포인트 구현 시 "상태"를 "구현완료"로 바꾸고 요청/응답 예시를 아래에 추가한다.
- 실제 상세 스펙은 springdoc-openapi(Swagger UI)가 소스이므로, 이 문서는 전체 그림을 빠르게 훑기 위한 목차 용도로만 쓴다.
