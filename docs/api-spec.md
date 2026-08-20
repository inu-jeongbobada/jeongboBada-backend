# API 명세

개발 우선순위(CLAUDE.md)에 맞춰 도메인별로 채워 나간다.

## 1. 인증 (user)
| Method | Path | 설명 | 상태 |
|---|---|---|---|
| POST | /api/auth/signup | 회원가입 (학번 기반) | 미구현 |
| POST | /api/auth/login | 로그인 (JWT 발급) | 미구현 |

## 2. 교수 정보 (professor)
| Method | Path                                                                | 설명       | Request                                                                                                             | Response                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 상태  |
|--------|---------------------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----|
| GET    | /api/professor                                                      | 교수 목록 조회 | N/A                                                                                                                 | [{ <br/> "professorId": 0, <br/> "professorName": "OOO", <br/> "professorImageUrl": "https://...", <br/> "labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://..."<br/>}]                                                                                                                                                                                                                                                                                               | 미구현 |
| GET    | /api/professor/{professorId}                                        | 교수 상세 조회 | N/A                                                                                                                 | { <br/> "professorId": 0, <br/> "professorName": "OOO", <br/> "professorImageUrl": https://...", <br/> "professorDetail": "~", <br/> "labId": 0, <br/> "labName": "OOOOO", <br/> "labUrl": "https://...", <br/> "labDetail": "~", <br/> "professorComment": [{ <br/> "professorCommentId": 0, <br/> "professorCommentDetail": "~", <br/> "professorCommentRate": 0, <br/> "professorCommentDate": "YYYY-MM-DD", <br/> "professorCommentAnonymity": "True or False" <br/>}] <br/>} | 미구현 |
| POST   | /api/professor/{professorId}/professor-comment                      | 교수 댓글 작성 | { <br/> "userId": 0, <br/> "professorCommentDetail": "~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | { <br/> "message": "댓글 작성이 완료되었습니다." <br/>}                                                                                                                                                                                                                                                                                                                                                                                                                                       | 미구현 |
| PATCH  | /api/professor/{professorId}/professor-comment/{professorCommentId} | 교수 댓글 수정 | { <br/> "userId": 0, <br/> "professorCommentDetail": "~", <br/> "professorCommentAnonymity": "TRUE or FALSE" <br/>} | { <br/> "message": "댓글 수정이 완료되었습니다." <br/>}                                                                                                                                                                                                                                                                                                                                                                                                                                       | 미구현 |
| DELETE | /api/professor/{professorId}/professor-comment/{professorCommentId} | 교수 댓글 삭제 | N/A                                                                                                                 | { <br/> "message": "댓글 삭제가 완료되었습니다." <br/>}                                                                                                                                                                                                                                                                                                                                                                                                                                       | 미구현 |

## 3. 전공 수업 (course)
| Method | Path | 설명 | 상태 |
|---|---|---|---|
| GET | /api/courses | 수업 목록 조회 | 미구현 |
| GET | /api/courses/{id}/reviews | 수업 후기 조회 (학점별 필터) | 미구현 |

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
