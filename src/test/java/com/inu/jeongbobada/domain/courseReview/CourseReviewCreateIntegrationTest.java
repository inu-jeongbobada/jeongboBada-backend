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
import com.inu.jeongbobada.domain.courseReview.entity.CourseReview;
import com.inu.jeongbobada.domain.courseReview.enums.AcademicYear;
import com.inu.jeongbobada.domain.courseReview.repository.CourseReviewRepository;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.user.SignupRequests;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.file.FileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #105: 강의평 작성에서 필수값 누락은 500이 아니라 400(errors에 필드), 중복 작성은 409 DUPLICATE_COURSE_REVIEW.
// CI DB는 비어 있으므로 과목·교수·개설강의를 테스트가 직접 만들고 지운다.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseReviewCreateIntegrationTest {

    // 가입 때 재학 증빙 서류를 S3에 올리는 부분. 이 테스트는 서류와 무관해서 S3를 부르지 않게 바꾼다
    @MockitoBean
    private FileStorage fileStorage;

    private static final String VALID_BODY = """
        {
          "professorId": %d,
          "academicYear": "YEAR_2026",
          "semester": "FIRST",
          "rating": "FOUR",
          "content": "과제가 많지만 설명이 친절해서 배울 게 많은 수업이었습니다.",
          "groupActivity": "NONE",
          "attendance": "ELECTRONIC",
          "examCount": "TWO",
          "assignmentFrequency": "NORMAL",
          "quizFrequency": "LOW",
          "gradingType": "NORMAL"
        }
        """;

    private static final String VALID_UPDATE_BODY = """
        {
          "rating": "FIVE",
          "content": "수정한 후기 내용이며 강의 설명과 실습이 모두 만족스러웠습니다.",
          "groupActivity": "ALWAYS",
          "attendance": "ROLL_CALL",
          "examCount": "ONE",
          "assignmentFrequency": "HIGH",
          "quizFrequency": "NORMAL",
          "gradingType": "GENEROUS"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String studentId;
    private final List<String> additionalStudentIds = new ArrayList<>();
    private Course course;
    private Professor professor;
    private CourseOffering offering;

    @BeforeEach
    void setUp() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        studentId = String.format("9%08d", n);
        mockMvc.perform(SignupRequests.signup(studentId, "password1", "rv" + (n % 10_000), "rv" + n + "@example.com"))
            .andExpect(status().isCreated());
        SignupRequests.approve(jdbcTemplate, studentId);

        course = courseRepository.save(new Course("테스트과목", "T" + n, "테스트 과목 설명"));
        professor = professorRepository.save(new Professor(null, "테스트교수", "테스트 교수 설명", null, "200", "032-000-0000", "email@inu.ac.kr"));
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
        additionalStudentIds.forEach(id ->
            userRepository.findByStudentId(id).ifPresent(userRepository::delete));
        additionalStudentIds.clear();
    }

    @Test
    void 강의평을_작성한다() throws Exception {
        createReview(login(), VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        CourseReview review = courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId())
            .getFirst();
        assertThat(review.getAcademicYear()).isEqualTo(AcademicYear.YEAR_2026);
        assertThat(review.getSemester()).isEqualTo(Semester.FIRST);
    }

    // 수강 시기는 후기 정보일 뿐 중복 기준이 아니므로, 학기가 달라도 같은 과목·교수에는 한 번만 작성한다.
    @Test
    void 다른_학기에도_같은_과목과_교수의_후기는_중복_작성할_수_없다() throws Exception {
        String token = login();
        createReview(token, VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk());

        String secondSemesterBody = VALID_BODY.formatted(professor.getProfessorId())
            .replace("\"semester\": \"FIRST\"", "\"semester\": \"SECOND\"");

        createReview(token, secondSemesterBody)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_COURSE_REVIEW"));

        assertThat(courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId()))
            .hasSize(1);
    }

    @Test
    void 빈_요청이면_400이고_errors에_필수_필드가_담긴다() throws Exception {
        createReview(login(), "{}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                "professorId", "academicYear", "semester", "rating", "content", "gradingType")));
    }

    @Test
    void 허용하지_않는_수강_연도이면_400() throws Exception {
        String body = VALID_BODY.formatted(professor.getProfessorId())
            .replace("\"academicYear\": \"YEAR_2026\"", "\"academicYear\": \"YEAR_2022\"");

        createReview(login(), body)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 후기_조회_응답에_수강_연도와_학기가_포함된다() throws Exception {
        createReview(login(), VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/{courseId}/reviews", course.getCourseId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].academicYear").value("YEAR_2026"))
            .andExpect(jsonPath("$.data[0].semester").value("FIRST"));
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
    void 내용이_1000자를_초과하면_400() throws Exception {
        String body = VALID_BODY.formatted(professor.getProfessorId())
            .replace("과제가 많지만 설명이 친절해서 배울 게 많은 수업이었습니다.", "가".repeat(1001));

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

    @Test
    void 본인_후기를_삭제한_뒤_같은_강의에_다시_작성할_수_있다() throws Exception {
        String token = login();
        String body = VALID_BODY.formatted(professor.getProfessorId());
        createReview(token, body).andExpect(status().isOk());

        CourseReview review = courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId())
            .getFirst();

        mockMvc.perform(delete("/api/courses/{courseId}/reviews/{reviewId}",
                course.getCourseId(), review.getReviewId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value("과목 후기가 삭제되었습니다."));

        assertThat(courseReviewRepository.existsById(review.getReviewId())).isFalse();
        createReview(token, body).andExpect(status().isOk());
    }

    @Test
    void 다른_사용자의_후기는_삭제할_수_없다() throws Exception {
        String ownerToken = login();
        createReview(ownerToken, VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk());

        Long reviewId = courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId())
            .getFirst()
            .getReviewId();

        String otherStudentId = signupAdditionalUser();
        String otherToken = login(otherStudentId);

        mockMvc.perform(delete("/api/courses/{courseId}/reviews/{reviewId}",
                course.getCourseId(), reviewId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(courseReviewRepository.existsById(reviewId)).isTrue();
    }

    @Test
    void 토큰이_없으면_후기를_삭제할_수_없다() throws Exception {
        mockMvc.perform(delete("/api/courses/{courseId}/reviews/{reviewId}",
                course.getCourseId(), 999999L))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void 본인_후기를_수정한다() throws Exception {
        String token = login();
        createReview(token, VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk());

        CourseReview review = courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId())
            .getFirst();

        updateReview(token, review.getReviewId(), VALID_UPDATE_BODY)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value("과목 후기가 수정되었습니다."));

        CourseReview updated = courseReviewRepository.findById(review.getReviewId()).orElseThrow();
        assertThat(updated.getRating().name()).isEqualTo("FIVE");
        assertThat(updated.getContent()).isEqualTo("수정한 후기 내용이며 강의 설명과 실습이 모두 만족스러웠습니다.");
        assertThat(updated.getProfessor().getProfessorId()).isEqualTo(professor.getProfessorId());
        assertThat(updated.getGroupActivity().name()).isEqualTo("ALWAYS");
        assertThat(updated.getAttendance().name()).isEqualTo("ROLL_CALL");
        assertThat(updated.getExamCount().name()).isEqualTo("ONE");
        assertThat(updated.getAssignmentFrequency().name()).isEqualTo("HIGH");
        assertThat(updated.getQuizFrequency().name()).isEqualTo("NORMAL");
    }

    @Test
    void 다른_사용자의_후기는_수정할_수_없다() throws Exception {
        String ownerToken = login();
        createReview(ownerToken, VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk());

        Long reviewId = courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId())
            .getFirst()
            .getReviewId();

        String otherToken = login(signupAdditionalUser());

        updateReview(otherToken, reviewId, VALID_UPDATE_BODY)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 토큰이_없으면_후기를_수정할_수_없다() throws Exception {
        mockMvc.perform(put("/api/courses/{courseId}/reviews/{reviewId}",
                course.getCourseId(), 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void 후기_수정_내용이_20자_미만이면_400() throws Exception {
        String token = login();
        createReview(token, VALID_BODY.formatted(professor.getProfessorId()))
            .andExpect(status().isOk());

        Long reviewId = courseReviewRepository
            .findAllByCourse_CourseIdOrderByCreatedAtDescReviewIdDesc(course.getCourseId())
            .getFirst()
            .getReviewId();

        updateReview(token, reviewId, VALID_UPDATE_BODY.replace(
                "수정한 후기 내용이며 강의 설명과 실습이 모두 만족스러웠습니다.", "짧음"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
            .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    private ResultActions createReview(String accessToken, String body) throws Exception {
        return mockMvc.perform(post("/api/courses/{courseId}/reviews", course.getCourseId())
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private ResultActions updateReview(String accessToken, Long reviewId, String body) throws Exception {
        return mockMvc.perform(put("/api/courses/{courseId}/reviews/{reviewId}",
                course.getCourseId(), reviewId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private String login() throws Exception {
        return login(studentId);
    }

    private String login(String loginStudentId) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":"%s","password":"password1"}
                    """.formatted(loginStudentId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private String signupAdditionalUser() throws Exception {
        int n = ThreadLocalRandom.current().nextInt(100_000_000);
        String additionalStudentId = String.format("8%08d", n);
        additionalStudentIds.add(additionalStudentId);

        mockMvc.perform(SignupRequests.signup(additionalStudentId, "password1", "o" + (n % 100_000), "other" + n + "@example.com"))
            .andExpect(status().isCreated());
        SignupRequests.approve(jdbcTemplate, additionalStudentId);

        return additionalStudentId;
    }
}
