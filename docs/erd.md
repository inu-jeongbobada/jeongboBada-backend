# ERD

CLAUDE.md에 정의된 10개 테이블 기준 초안. PK는 전부 `BIGINT AUTO_INCREMENT`.

## 테이블 목록

| 테이블               | 설명                | 담당 도메인 패키지 | 상태  |
|-------------------|-------------------|------------|-----|
| USERS             | 사용자 (학번 기반 인증)    | user       | 미정의 |
| PROFESSOR         | 교수 정보             | professor  | 미정의 |
| course            | 전공 수업             | course     | 미정의 |
| course_review     | 수업 후기 (학점별 필터 대상) | course     | 미정의 |
| PROFESSOR_COMMENT | 교수 후기             | professor  | 미정의 |
| LAB               | 연구실 정보            | lab        | 미정의 |
| post              | 게시글               | community  | 미정의 |
| comment           | 댓글                | community  | 미정의 |
| career_story      | 진로 수기             | career     | 미정의 |
| favorite          | 즐겨찾기              | mypage     | 미정의 |
| notification      | 알림                | mypage     | 미정의 |

## 다이어그램

<!-- 컬럼/관계 확정되면 mermaid ERD로 채우기 -->
```mermaid
erDiagram
```

## 작성 규칙
- 컬럼이 확정된 테이블만 "상태"를 "확정"으로 바꾸고 다이어그램에 반영한다.
- 연관관계(FK)는 방향과 삭제 정책(CASCADE 등)까지 명시한다.
