-- 개발용 초기 데이터
-- 실제 확인 정보: 교수명, 전공 분야, 담당 과목
-- 임시 데이터: 학수번호, 학년, 시간표, 학기, 학점, 이수구분, 수업방식, 평가방식
--
-- course = 학기가 바뀌어도 안 변하는 "과목 자체" (courseCode로 식별)
-- course_offering = 특정 학년도/학기에 특정 교수가 개설한 강의 (학기마다 새로 들어오거나 갱신됨)

-- course_review가 course/professor를 FK로 참조하므로 가장 먼저 지워야 한다
-- (앱을 통해 리뷰를 하나라도 등록한 적이 있으면 course_review에 행이 남아있어서,
--  이걸 안 지우고 course를 지우려 하면 FK 제약 위반으로 기동이 실패한다).
DELETE FROM course_review;
DELETE FROM course_offering;
DELETE FROM course;
DELETE FROM professor;

INSERT INTO professor (
    professor_id,
    professor_name,
    professor_detail,
    professor_image_url
) VALUES
(1, '우요섭', '한국어 정보처리, 빅데이터, 분산네트워크, 데이터베이스', NULL),
(2, '강승택', '전자파 해석, 초고주파 전자회로, 안테나, 마이크로파 공학', NULL),
(3, '이은규', '컴퓨터 보안, 사물인터넷, 에너지 IT, 차량 네트워킹', NULL),
(4, '조경훈', '지능시스템, 로보틱스, 최적제어, 제어시스템 머신러닝', NULL),
(5, '김영필', '운영체제, 분산 시스템, 클라우드 컴퓨팅', NULL),
(6, '김도엽', '통신 네트워크 최적화, 머신러닝, 알고리즘 설계', NULL),
(7, '안승환', '인공지능, 생성모형, 분포학습', NULL),
(8, '김경원', '무선통신, 무선채널, 무선네트워크', NULL);

INSERT INTO course (
    course_id,
    course_name,
    course_code,
    course_detail,
    active
) VALUES
(1, '데이터베이스', 'ITE-DEV-001', '관계형 데이터베이스의 구조, 설계 방법과 SQL 활용 방법을 학습한다.', TRUE),
(2, '인터넷프로그래밍', 'ITE-DEV-002', '웹과 인터넷 서비스 개발에 필요한 프로그래밍 기술을 학습한다.', TRUE),
(3, '클라우드컴퓨팅', 'ITE-DEV-003', '분산 시스템과 클라우드 컴퓨팅의 구조 및 활용 방법을 학습한다.', TRUE),
(4, '전기자기학', 'ITE-DEV-004', '전기장과 자기장의 기본 원리 및 정보통신공학에서의 활용을 학습한다.', TRUE),
(5, '안테나공학', 'ITE-DEV-005', '안테나의 동작 원리, 방사 특성과 설계 방법을 학습한다.', TRUE),
(6, '정보보안실습', 'ITE-DEV-006', '컴퓨터 시스템과 네트워크 보안 기술을 실습 중심으로 학습한다.', TRUE),
(7, '사물인터넷', 'ITE-DEV-007', '센서, 네트워크 및 응용 서비스로 구성되는 사물인터넷 기술을 학습한다.', TRUE),
(8, '인공지능', 'ITE-DEV-008', '인공지능의 기본 개념과 탐색, 학습 및 추론 알고리즘을 학습한다.', TRUE),
(9, '운영체제', 'ITE-DEV-009', '프로세스, 스레드, 메모리 및 파일 시스템 등 운영체제의 원리를 학습한다.', TRUE),
(10, '임베디드컴퓨팅', 'ITE-DEV-010', '임베디드 시스템의 구조와 하드웨어 및 소프트웨어 개발 방법을 학습한다.', TRUE),
(11, '컴퓨터네트워크', 'ITE-DEV-011', '네트워크 계층 구조와 주요 인터넷 프로토콜의 동작 원리를 학습한다.', TRUE),
(12, '디지털신호처리', 'ITE-DEV-012', '디지털 신호의 표현, 분석 및 처리에 필요한 기본 이론을 학습한다.', TRUE);

INSERT INTO course_offering (
    course_offering_id,
    course_id,
    professor_id,
    academic_year,
    semester,
    grade,
    credits,
    course_time,
    course_type,
    evaluation_type,
    is_online,
    active
) VALUES
(1, 1, 1, 2026, 'FIRST', 'SECOND', 'THIRD', '월 3,4 / 수 3,4', 'MAJOR_CORE', 'RELATIVE', 'OFFLINE', TRUE),
(2, 2, 1, 2026, 'SECOND', 'SECOND', 'THIRD', '화 3,4 / 목 3,4', 'MAJOR_CORE', 'RELATIVE', 'OFFLINE', TRUE),
(3, 3, 1, 2026, 'FIRST', 'FOURTH', 'THIRD', '금 2,3,4', 'MAJOR_ADVANCED', 'ABSOLUTE', 'BLENDED_LEARNING', TRUE),
(4, 4, 2, 2026, 'FIRST', 'SECOND', 'THIRD', '월 5,6 / 수 5,6', 'MAJOR_CORE', 'RELATIVE', 'OFFLINE', TRUE),
(5, 5, 2, 2026, 'SECOND', 'FOURTH', 'THIRD', '화 5,6 / 목 5,6', 'MAJOR_ADVANCED', 'RELATIVE', 'OFFLINE', TRUE),
(6, 6, 3, 2026, 'SECOND', 'THIRD', 'THIRD', '월 7,8,9', 'MAJOR_ADVANCED', 'ABSOLUTE', 'OFFLINE', TRUE),
(7, 7, 3, 2026, 'FIRST', 'THIRD', 'THIRD', '화 7,8 / 목 7', 'MAJOR_ADVANCED', 'RELATIVE', 'BLENDED_LEARNING', TRUE),
(8, 8, 4, 2026, 'SECOND', 'THIRD', 'THIRD', '월 1,2 / 수 1,2', 'MAJOR_ADVANCED', 'RELATIVE', 'OFFLINE', TRUE),
(9, 9, 5, 2026, 'FIRST', 'THIRD', 'THIRD', '화 1,2 / 목 1,2', 'MAJOR_CORE', 'RELATIVE', 'OFFLINE', TRUE),
(10, 10, 5, 2026, 'SECOND', 'FOURTH', 'THIRD', '금 5,6,7', 'MAJOR_ADVANCED', 'ABSOLUTE', 'OFFLINE', TRUE),
(11, 11, 6, 2026, 'FIRST', 'THIRD', 'THIRD', '월 5,6 / 수 5,6', 'MAJOR_CORE', 'RELATIVE', 'OFFLINE', TRUE),
(12, 12, 8, 2026, 'SECOND', 'THIRD', 'THIRD', '화 3,4 / 목 3,4', 'MAJOR_ADVANCED', 'RELATIVE', 'OFFLINE', TRUE);
