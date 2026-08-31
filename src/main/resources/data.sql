-- 개발용 초기 데이터. 서버 기동 시(spring.sql.init.mode=always)마다 매번 실행되므로
-- 먼저 지우고 다시 넣는 방식으로 idempotent하게 유지한다. (자식 테이블 course부터 삭제)
DELETE FROM course;
DELETE FROM professor;

INSERT INTO professor (professor_id, professor_name, professor_detail, professor_image_url) VALUES
(1, '홍길동', '데이터베이스 및 소프트웨어공학 전공', 'https://example.com/professors/1.jpg'),
(2, '김철수', '인공지능 및 알고리즘 전공', 'https://example.com/professors/2.jpg');

INSERT INTO course (
    course_name, course_code, professor_name, grade, course_time, semester,
    course_detail, credits, course_type, is_online, professor_id, evaluation_type
) VALUES
('데이터베이스', 'CSE301', '홍길동', 'THIRD', '월 3,4 / 수 3,4', 'FIRST', '관계형 데이터베이스 설계와 SQL을 다룬다.', 'THIRD', 'MAJOR_CORE', 'OFFLINE', 1, 'RELATIVE'),
('알고리즘', 'CSE302', '김철수', 'THIRD', '화 1,2 / 목 1,2', 'FIRST', '알고리즘 설계 기법과 복잡도 분석을 다룬다.', 'THIRD', 'MAJOR_CORE', 'BLENDED_LEARNING', 2, 'RELATIVE'),
('소프트웨어공학', 'CSE401', '홍길동', 'FOURTH', '금 3,4,5', 'SECOND', '소프트웨어 개발 생명주기와 프로젝트 관리 기법을 다룬다.', 'THIRD', 'MAJOR_ADVANCED', 'ONLINE', 1, 'ABSOLUTE');
