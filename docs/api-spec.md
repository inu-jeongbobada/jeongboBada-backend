# API 명세

개발 우선순위(CLAUDE.md)에 맞춰 도메인별로 채워 나간다.

## 1. 인증 (user)
공통 응답 포맷: `{ "success": boolean, "data": T, "code": string | null, "message": string | null }`
(`success: false`일 때 `code`/`message`에 에러 정보)

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| POST | /api/auth/signup | 회원가입 (학번 기반) | { <br/> "studentId": "202012345", <br/> "password": "영문+숫자 8~64자", <br/> "nickname": "한글/영문/숫자/-/_  2~10자" <br/>} | 201 <br/> { "success": true, "data": null } | 구현완료 |
| POST | /api/auth/login | 로그인 (JWT 발급) | { <br/> "studentId": "202012345", <br/> "password": "..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/reissue | Access Token 재발급 | { <br/> "refreshToken": "eyJ..." <br/>} | 200 <br/> { "success": true, "data": { <br/> "accessToken": "eyJ...", <br/> "refreshToken": "eyJ..." <br/>} } | 구현완료 |
| POST | /api/auth/logout | 로그아웃 | Header: <br/> `Authorization: Bearer {accessToken}` | 200 <br/> { "success": true, "data": null } | 구현완료 |

- 로그인/재발급 응답의 `refreshToken`은 studentId처럼 민감정보 취급 — 화면에 노출하지 말고 저장 용도로만 사용
- 아직 인증 필터가 없어서(작업 중), 로그인 없이도 모든 API가 열려있는 상태. 추후 로그인 필요한 API가 생기면 이 문서에 표시 예정

## 2. 교수 정보 (professor)
목록/상세 조회는 인증 도메인과 동일하게 `ApiResponse`로 감싸서 응답한다 (course 도메인과 다름).

| Method | Path                                                                  | 설명       | Request                                                                                                              | Response                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | 상태  |
|--------|-----------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----|
| GET    | /api/professors                                                       | 교수 목록 조회 | N/A                                                                                                                  | 200 <br/> { "success": true, "data": [{ <br/> "professorId": 0, <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> "labList": { <br/>"labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://..." <br/>} <br/>}] } | 구현완료 |
| GET    | /api/professors/{professorId}                                         | 교수 상세 조회 | N/A                                                                                                                  | 200 <br/> { "success": true, "data": { <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> "professorDetail": "\~", <br/> "labDetail": { <br/>"labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://...", <br/> "labDetail": "\~" <br/>}, <br/> "professorCommentDetails": [{ <br/> "professorCommentId": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentRate": 0, <br/> "professorCommentDate": "YYYY-MM-DDTHH:mm:ss", <br/> "professorCommentAnonymity": "TRUE\|FALSE" <br/>}] <br/>} } | 구현완료 |
| POST   | /api/professors/{professorId}/professor-comments                      | 교수 댓글 작성 | { <br/> "userId": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | { <br/> "message": "댓글 작성이 완료되었습니다." <br/>}                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 미구현 |
| PATCH  | /api/professors/{professorId}/professor-comments/{professorCommentId} | 교수 댓글 수정 | { <br/> "userId": 0, <br/> "professorCommentDetail": "\~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | { <br/> "message": "댓글 수정이 완료되었습니다." <br/>}                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 미구현 |
| DELETE | /api/professors/{professorId}/professor-comments/{professorCommentId} | 교수 댓글 삭제 | N/A                                                                                                                  | { <br/> "message": "댓글 삭제가 완료되었습니다." <br/>}                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 미구현 |

## 3. 전공 수업 (course)
⚠️ 이 도메인은 `ApiResponse`로 안 감싸고 **DTO를 그대로** 응답한다 (인증 도메인과 포맷이 다름).

| Method | Path | 설명 | Request | Response | 상태 |
|---|---|---|---|---|---|
| GET | /api/courses | 수업 목록 조회 | N/A | 200 <br/> [{ <br/> "courseName": "...", <br/> "professorName": "...", <br/> "grade": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "semester": "FIRST\|SECOND", <br/> "credits": "FIRST\|SECOND\|THIRD\|FOURTH", <br/> "courseCode": "...", <br/> "courseType": "MAJOR_CORE\|MAJOR_FOUNDATION\|MAJOR_ADVANCED" <br/>}] | 구현완료 |
| GET | /api/courses/{courseId} | 수업 상세 조회 | N/A | 200 <br/> { <br/> "courseName": "...", <br/> "professorName": "...", <br/> "courseDetail": "...", <br/> "grade": "...", <br/> "semester": "...", <br/> "credits": "...", <br/> "courseCode": "...", <br/> "courseTime": "...", <br/> "courseType": "...", <br/> "professor": { <br/> "professorId": 0, <br/> "professorName": "...", <br/> "professorImageUrl": "https://..." <br/>}, <br/> "evaluationType": "ABSOLUTE\|RELATIVE", <br/> "isOnline": "BLENDED_LEARNING\|ONLINE\|OFFLINE" <br/>} | 구현완료 |
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
