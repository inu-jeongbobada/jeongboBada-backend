package com.inu.jeongbobada.domain.courseReview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.course.entity.CourseOffering;
import com.inu.jeongbobada.domain.course.enums.CourseType;
import com.inu.jeongbobada.domain.course.enums.Credits;
import com.inu.jeongbobada.domain.course.enums.EvaluationType;
import com.inu.jeongbobada.domain.course.enums.Grade;
import com.inu.jeongbobada.domain.course.enums.IsOnline;
import com.inu.jeongbobada.domain.course.enums.Semester;
import com.inu.jeongbobada.domain.course.repository.CourseOfferingRepository;
import com.inu.jeongbobada.domain.course.repository.CourseRepository;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #105: 강의평 작성에서 필수값 누락은 500이 아니라 400(errors에 필드), 중복 작성은 409 DUPLICATE_COURSE_REVIEW.
// CI DB는 비어 있으므로 과목·교수·개설강의를 테스트가 직접 만들고 지운다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseReviewCreateIntegrationTest {

    private static final String VALID_BODY = """
        {
          "professorId": %d,
          "rating": "FOUR",
          "content": "과제가 많지만 설명이 친절해서 배울 게 많은 수업이었습니다.",
          "textbook": "OPTIONAL",
          "assignmentDifficulty": "NORMAL",
          "assignmentAmount": "HIGH",
          "groupActivity": "OCCASIONAL",
          "attendance": "STRICT",
          "examCount": "TWO",
          "quizDifficulty": "EASY",
          "examDifficulty": "HARD",
          "quizCount": "ONE",
          "gradingType": "NORMAL"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String studentId;
    private Course course;
    private Professor professor;
    private CourseOffering offering;

    @BeforeEach
    void setUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1","nickname":"rv%d","email":"rv%d@example.com"}
                    """.formatted(studentId, n % 1_000_000, n)))
            .andExpect(status().isCreated());

        course = courseRepository.save(new Course("테스트과목", "T" + n, "테스트 과목 설명"));
        professor = professorRepository.save(new Professor(null, "테스트교수", "테스트 교수 설명", null));
        offering = courseOfferingRepository.save(new CourseOffering(
            course, professor, 2026, Semester.FIRST, Grade.SECOND, Credits.THIRD,
            "월 1,2", CourseType.MAJOR_CORE, EvaluationType.RELATIVE, IsOnline.OFFLINE));
    }

    @AfterEach
    void cleanUp() {
        courseReviewRepository.deleteAll(
            courseReviewRepository.findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId()));
        courseOfferingRepository.delete(offering);
        courseRepository.delete(course);
        professorRepository.delete(professor);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void 강의평을_작성한다() throws Exception {
        createReview(login(), VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 빈_요청이면_400이고_errors에_필수_필드가_담긴다() throws Exception {
        createReview(login(), "{}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[*].field").value(hasItems("professorId", "rating", "content", "gradingType")));
    }

    @Test
    void professorId가_없으면_400() throws Exception {
        String body = VALID_BODY.formatted(professor.getProfessorId())
            .replaceFirst("\"professorId\": \\d+,", "");

        createReview(login(), body)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("professorId"));
    }

    @Test
    void 내용이_20자_미만이면_400() throws Exception {
        String body = VALID_BODY.formatted(professor.getProfessorId())
            .replace("과제가 많지만 설명이 친절해서 배울 게 많은 수업이었습니다.", "짧음");

        createReview(login(), body)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    @Test
    void 같은_강의에_두_번_작성하면_409_DUPLICATE_COURSE_REVIEW() throws Exception {
        String token = login();
        String body = VALID_BODY.formatted(professor.getProfessorId());
        createReview(token, body).andExpect(status().isOk());

        createReview(token, body)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_COURSE_REVIEW"));
    }

    @Test
    void 토큰이_없으면_잘못된_요청이어도_401() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/reviews", course.getCourseId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private ResultActions createReview(String accessToken, String body) throws Exception {
        return mockMvc.perform(post("/api/courses/{courseId}/reviews", course.getCourseId())
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1"}
                    """.formatted(studentId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }
}
