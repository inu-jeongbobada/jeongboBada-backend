-- 개발용 초기 데이터
-- 실제 확인 정보: 교수명, 전공 분야, 담당 과목
-- 임시 데이터: 학수번호, 학년, 시간표, 학기, 학점, 이수구분, 수업방식, 평가방식

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
    course_name,
    course_code,
    professor_name,
    grade,
    course_time,
    semester,
    course_detail,
    credits,
    course_type,
    is_online,
    professor_id,
    evaluation_type
) VALUES
(
    '데이터베이스',
    'ITE-DEV-001',
    '우요섭',
    'SECOND',
    '월 3,4 / 수 3,4',
    'FIRST',
    '관계형 데이터베이스의 구조, 설계 방법과 SQL 활용 방법을 학습한다.',
    'THIRD',
    'MAJOR_CORE',
    'OFFLINE',
    1,
    'RELATIVE'
),
(
    '인터넷프로그래밍',
    'ITE-DEV-002',
    '우요섭',
    'SECOND',
    '화 3,4 / 목 3,4',
    'SECOND',
    '웹과 인터넷 서비스 개발에 필요한 프로그래밍 기술을 학습한다.',
    'THIRD',
    'MAJOR_CORE',
    'OFFLINE',
    1,
    'RELATIVE'
),
(
    '클라우드컴퓨팅',
    'ITE-DEV-003',
    '우요섭',
    'FOURTH',
    '금 2,3,4',
    'FIRST',
    '분산 시스템과 클라우드 컴퓨팅의 구조 및 활용 방법을 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'BLENDED_LEARNING',
    1,
    'ABSOLUTE'
),
(
    '전기자기학',
    'ITE-DEV-004',
    '강승택',
    'SECOND',
    '월 5,6 / 수 5,6',
    'FIRST',
    '전기장과 자기장의 기본 원리 및 정보통신공학에서의 활용을 학습한다.',
    'THIRD',
    'MAJOR_CORE',
    'OFFLINE',
    2,
    'RELATIVE'
),
(
    '안테나공학',
    'ITE-DEV-005',
    '강승택',
    'FOURTH',
    '화 5,6 / 목 5,6',
    'SECOND',
    '안테나의 동작 원리, 방사 특성과 설계 방법을 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'OFFLINE',
    2,
    'RELATIVE'
),
(
    '정보보안실습',
    'ITE-DEV-006',
    '이은규',
    'THIRD',
    '월 7,8,9',
    'SECOND',
    '컴퓨터 시스템과 네트워크 보안 기술을 실습 중심으로 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'OFFLINE',
    3,
    'ABSOLUTE'
),
(
    '사물인터넷',
    'ITE-DEV-007',
    '이은규',
    'THIRD',
    '화 7,8 / 목 7',
    'FIRST',
    '센서, 네트워크 및 응용 서비스로 구성되는 사물인터넷 기술을 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'BLENDED_LEARNING',
    3,
    'RELATIVE'
),
(
    '인공지능',
    'ITE-DEV-008',
    '조경훈',
    'THIRD',
    '월 1,2 / 수 1,2',
    'SECOND',
    '인공지능의 기본 개념과 탐색, 학습 및 추론 알고리즘을 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'OFFLINE',
    4,
    'RELATIVE'
),
(
    '운영체제',
    'ITE-DEV-009',
    '김영필',
    'THIRD',
    '화 1,2 / 목 1,2',
    'FIRST',
    '프로세스, 스레드, 메모리 및 파일 시스템 등 운영체제의 원리를 학습한다.',
    'THIRD',
    'MAJOR_CORE',
    'OFFLINE',
    5,
    'RELATIVE'
),
(
    '임베디드컴퓨팅',
    'ITE-DEV-010',
    '김영필',
    'FOURTH',
    '금 5,6,7',
    'SECOND',
    '임베디드 시스템의 구조와 하드웨어 및 소프트웨어 개발 방법을 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'OFFLINE',
    5,
    'ABSOLUTE'
),
(
    '컴퓨터네트워크',
    'ITE-DEV-011',
    '김도엽',
    'THIRD',
    '월 5,6 / 수 5,6',
    'FIRST',
    '네트워크 계층 구조와 주요 인터넷 프로토콜의 동작 원리를 학습한다.',
    'THIRD',
    'MAJOR_CORE',
    'OFFLINE',
    6,
    'RELATIVE'
),
(
    '디지털신호처리',
    'ITE-DEV-012',
    '김경원',
    'THIRD',
    '화 3,4 / 목 3,4',
    'SECOND',
    '디지털 신호의 표현, 분석 및 처리에 필요한 기본 이론을 학습한다.',
    'THIRD',
    'MAJOR_ADVANCED',
    'OFFLINE',
    8,
    'RELATIVE'
);
