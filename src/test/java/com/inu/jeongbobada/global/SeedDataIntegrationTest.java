package com.inu.jeongbobada.global;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

// data.sql은 서버가 뜰 때마다 실행된다 (배포 서버 포함). 다시 실행해도 사용자가 쓴 데이터를 지우지 않고,
// 시드를 두 번 넣지 않아야 한다. 예전에는 DELETE 후 다시 넣어서 재시작마다 강의평이 사라졌다.
// 테스트 프로필은 data.sql을 자동 실행하지 않으므로 직접 실행하고, 끝나면 트랜잭션을 롤백해 DB를 비운다.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeedDataIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 시드를_다시_실행해도_사용자_강의평은_남고_시드는_늘지_않는다() {
        runSeed();
        int professors = count("professor");
        int courses = count("course");
        int offerings = count("course_offering");

        jdbcTemplate.update("""
            INSERT INTO users (student_id, password, nickname, role, created_at, updated_at)
            VALUES ('999000111', 'x', 'seedtest', 'USER', NOW(), NOW())
            """);
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM users WHERE student_id = '999000111'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO course_review (user_id, course_id, professor_id, academic_year, semester,
                like_count, rating, content, group_activity, attendance, exam_count,
                assignment_frequency, quiz_frequency, grading_type, created_at, updated_at)
            VALUES (?, 1, 1, 'YEAR_2026', 'FIRST', 0, 'ONE', '재시작해도 남아야 하는 후기',
                'NONE', 'ELECTRONIC', 'ZERO', 'NONE', 'NONE', 'VERY_GENEROUS', NOW(), NOW())
            """, userId);

        runSeed();

        assertThat(count("course_review WHERE user_id = " + userId)).isEqualTo(1);
        assertThat(count("professor")).isEqualTo(professors);
        assertThat(count("course")).isEqualTo(courses);
        assertThat(count("course_offering")).isEqualTo(offerings);
    }

    // 트랜잭션에 묶인 연결로 실행해야 테스트가 끝날 때 함께 롤백된다
    private void runSeed() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(connection,
            new EncodedResource(new ClassPathResource("data.sql"), StandardCharsets.UTF_8));
    }

    private int count(String tableAndCondition) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableAndCondition, Integer.class);
        return count == null ? 0 : count;
    }
}
