BEGIN;

-- 기존 후기에는 수강 연도·학기가 없어서 서버가 임의로 정할 수 없다.
-- 데이터가 있다면 먼저 각 후기의 수강 시기를 확인해 백필한 뒤 이 스크립트를 실행한다.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM course_review) THEN
        RAISE EXCEPTION 'course_review에 기존 데이터가 있습니다. academic_year와 semester를 먼저 백필해야 합니다.';
    END IF;
END
$$;

ALTER TABLE course_review
    ADD COLUMN IF NOT EXISTS academic_year VARCHAR(255),
    ADD COLUMN IF NOT EXISTS semester VARCHAR(255),
    ADD COLUMN IF NOT EXISTS assignment_frequency VARCHAR(255),
    ADD COLUMN IF NOT EXISTS quiz_frequency VARCHAR(255);

ALTER TABLE course_review
    ALTER COLUMN academic_year TYPE VARCHAR(255) USING academic_year::VARCHAR,
    ALTER COLUMN academic_year SET NOT NULL,
    ALTER COLUMN semester SET NOT NULL,
    ALTER COLUMN assignment_frequency SET NOT NULL,
    ALTER COLUMN quiz_frequency SET NOT NULL,
    ALTER COLUMN content TYPE VARCHAR(1000);

ALTER TABLE course_review
    DROP CONSTRAINT IF EXISTS uk_user_course_professor_review,
    DROP CONSTRAINT IF EXISTS uk_user_course_professor_term_review;

ALTER TABLE course_review
    ADD CONSTRAINT uk_user_course_professor_review
        UNIQUE (user_id, course_id, professor_id);

ALTER TABLE course_review
    DROP COLUMN IF EXISTS enrollment_grade,
    DROP COLUMN IF EXISTS text_book,
    DROP COLUMN IF EXISTS assignment_difficulty,
    DROP COLUMN IF EXISTS assignment_amount,
    DROP COLUMN IF EXISTS quiz_difficulty,
    DROP COLUMN IF EXISTS exam_difficulty,
    DROP COLUMN IF EXISTS quiz_count;

COMMIT;
