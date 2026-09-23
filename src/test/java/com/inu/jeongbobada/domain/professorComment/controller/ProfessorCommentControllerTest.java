package com.inu.jeongbobada.domain.professorComment.controller;

import com.inu.jeongbobada.domain.professorComment.exception.ProfessorCommentErrorCode;
import com.inu.jeongbobada.domain.professorComment.service.ProfessorCommentService;
import com.inu.jeongbobada.domain.user.security.CustomUserDetails;
import com.inu.jeongbobada.domain.user.service.StudentUserDetailsService;
import com.inu.jeongbobada.global.exception.BusinessException;
import com.inu.jeongbobada.global.exception.code.GlobalErrorCode;
import com.inu.jeongbobada.global.security.JwtAccessDeniedHandler;
import com.inu.jeongbobada.global.security.JwtAuthenticationEntryPoint;
import com.inu.jeongbobada.global.security.JwtAuthenticationFilter;
import com.inu.jeongbobada.global.security.JwtTokenProvider;
import com.inu.jeongbobada.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// test 프로필: SecurityConfig가 JwtProperties(jwt.secret 필수 검증)를 등록해서, CI에서도 값이 있도록 application-test.properties를 쓴다
@WebMvcTest(ProfessorCommentController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class ProfessorCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorCommentService professorCommentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private StudentUserDetailsService studentUserDetailsService;

    @Test
    void 로그인하지_않고_교수_후기_API를_요청하면_401을_반환한다()
        throws Exception {

        mockMvc.perform(delete(
                "/api/professors/{professorId}/professor-comments/{commentId}",
                1L,
                10L
            ))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(
                jsonPath("$.code")
                    .value(GlobalErrorCode.AUTHENTICATION_REQUIRED.getCode())
            );
    }

    @Test
    void 다른_사람의_교수_후기를_수정하면_403을_반환한다()
        throws Exception {

        doThrow(
            new BusinessException(
                ProfessorCommentErrorCode.PROFESSOR_COMMENT_FORBIDDEN
            )
        )
            .when(professorCommentService)
            .updateProfessorComment(
                eq(1L),
                eq(10L),
                eq(100L),
                any()
            );

        mockMvc.perform(patch(
                "/api/professors/{professorId}/professor-comments/{commentId}",
                10L,
                100L
            )
                .with(authentication(authenticationOf(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professorCommentRate": 5,
                      "professorCommentDetail": "수정하려는 후기",
                      "professorCommentAnonymity": "TRUE"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(
                jsonPath("$.code").value(
                    ProfessorCommentErrorCode
                        .PROFESSOR_COMMENT_FORBIDDEN
                        .getCode()
                )
            );
    }

    @Test
    void 다른_사람의_교수_후기를_삭제하면_403을_반환한다()
        throws Exception {

        doThrow(
            new BusinessException(
                ProfessorCommentErrorCode.PROFESSOR_COMMENT_FORBIDDEN
            )
        )
            .when(professorCommentService)
            .deleteProfessorComment(1L, 10L, 100L);

        mockMvc.perform(delete(
                "/api/professors/{professorId}/professor-comments/{commentId}",
                10L,
                100L
            )
                .with(authentication(authenticationOf(1L))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(
                jsonPath("$.code").value(
                    ProfessorCommentErrorCode
                        .PROFESSOR_COMMENT_FORBIDDEN
                        .getCode()
                )
            );
    }

    private UsernamePasswordAuthenticationToken authenticationOf(
        Long userId
    ) {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getUserId()).thenReturn(userId);
        when(principal.getAuthorities()).thenReturn(java.util.List.of());

        return new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
    }
}
