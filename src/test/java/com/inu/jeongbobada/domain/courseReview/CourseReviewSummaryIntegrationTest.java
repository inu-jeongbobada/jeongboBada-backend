package com.inu.jeongbobada.domain.courseReview;

import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.enums.Semester;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.AcademicYear;
import com.inu.jeongbobada.domain.courseReview.enums.Attendance;
import com.inu.jeongbobada.domain.courseReview.enums.ExamCount;
import com.inu.jeongbobada.domain.courseReview.enums.Frequency;
import com.inu.jeongbobada.domain.courseReview.enums.GradingType;
import com.inu.jeongbobada.domain.courseReview.enums.GroupActivity;
import com.inu.jeongbobada.domain.courseReview.enums.Rating;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.user.SignupRequests;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.file.FileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseReviewSummaryIntegrationTest {

    @MockitoBean
    private FileStorage fileStorage;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    private final List<User> users = new ArrayList<>();
    private Course course;
    private Professor professor;
    private Professor otherProfessor;

    @BeforeEach
    void setUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        for (int i = 0; i < 3; i++) {
            String studentId = String.format("8%07d%d", n, i);
            String nickname = String.format("s%04d%d", n % 10_000, i);
            mockMvc.perform(SignupRequests.signup(
                    studentId, "password1", nickname, "summary" + n + i + "@example.com"))
                .andExpect(status().isCreated());
            SignupRequests.approve(jdbcTemplate, studentId);
            users.add(userRepository.findByStudentId(studentId).orElseThrow());
        }

        course = courseRepository.save(new Course("요약테스트과목", "S" + n, "요약 테스트 과목 설명"));
        professor = professorRepository.save(new Professor(
            null, "요약교수", "요약 교수 설명", null, "300", "032-000-0010", "summary@inu.ac.kr"));
        otherProfessor = professorRepository.save(new Professor(
            null, "다른요약교수", "다른 교수 설명", null, "301", "032-000-0011", "other-summary@inu.ac.kr"));
    }

    @AfterEach
    void cleanUp() {
        if (course != null) {
            courseReviewRepository.deleteAll(
                courseReviewRepository.findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId()));
            courseRepository.delete(course);
        }
        if (professor != null) {
            professorRepository.delete(professor);
        }
        if (otherProfessor != null) {
            professorRepository.delete(otherProfessor);
        }
        userRepository.deleteAll(users);
        users.clear();
    }

    @Test
    void 교수별_후기_요약을_토큰_없이_조회한다() throws Exception {
        courseReviewRepository.saveAll(List.of(
            review(users.get(0), professor, Rating.FIVE, Attendance.ELECTRONIC),
            review(users.get(1), professor, Rating.FOUR, Attendance.ELECTRONIC),
            review(users.get(2), professor, Rating.THREE, Attendance.ROLL_CALL),
            review(users.get(0), otherProfessor, Rating.ONE, Attendance.NONE)
        ));

        mockMvc.perform(get("/api/courses/{courseId}/reviews/summary", course.getCourseId())
                .param("professorId", professor.getProfessorId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reviewCount").value(3))
            .andExpect(jsonPath("$.data.summaryAvailable").value(true))
            .andExpect(jsonPath("$.data.averageRating").value(4.0))
            .andExpect(jsonPath("$.data.groupActivity").value("SOMETIMES"))
            .andExpect(jsonPath("$.data.attendance").value("ELECTRONIC"));
    }

    @Test
    void 존재하지_않는_과목이면_404() throws Exception {
        mockMvc.perform(get("/api/courses/{courseId}/reviews/summary", Long.MAX_VALUE)
                .param("professorId", professor.getProfessorId().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    private CourseReview review(User user, Professor reviewProfessor, Rating rating, Attendance attendance) {
        return new CourseReview(
            user,
            course,
            reviewProfessor,
            AcademicYear.YEAR_2026,
            Semester.FIRST,
            rating,
            "강의 요약 통합 테스트를 위한 충분히 긴 후기 내용입니다.",
            GroupActivity.SOMETIMES,
            attendance,
            ExamCount.TWO,
            Frequency.NORMAL,
            Frequency.LOW,
            GradingType.GENEROUS
        );
    }
}
