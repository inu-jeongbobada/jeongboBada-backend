# API 명세

개발 우선순위(CLAUDE.md)에 맞춰 도메인별로 채워 나간다.

## 1. 인증 (user)
| Method | Path | 설명 | 상태 |
|---|---|---|---|
| POST | /api/auth/signup | 회원가입 (학번 기반) | 미구현 |
| POST | /api/auth/login | 로그인 (JWT 발급) | 미구현 |

## 2. 교수 정보 (professor)
| Method | Path | 설명 | 상태 |
|---|---|---|---|
| GET | /api/professors | 교수 목록 조회 | 미구현 |
| GET | /api/professors/{id} | 교수 상세 조회 | 미구현 |

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
