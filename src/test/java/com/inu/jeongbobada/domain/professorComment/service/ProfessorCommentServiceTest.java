package com.inu.jeongbobada.domain.professorComment.service;

import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentUpdateRequestDto;
import com.inu.jeongbobada.domain.professorComment.entity.ProfessorComment;
import com.inu.jeongbobada.domain.professorComment.entity.ProfessorCommentAnonymity;
import com.inu.jeongbobada.domain.professorComment.exception.ProfessorCommentErrorCode;
import com.inu.jeongbobada.domain.professorComment.repository.ProfessorCommentRepository;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorCommentServiceTest {

    private static final Long REQUEST_USER_ID = 1L;
    private static final Long OWNER_USER_ID = 2L;
    private static final Long PROFESSOR_ID = 10L;
    private static final Long COMMENT_ID = 100L;

    @Mock
    private ProfessorCommentRepository professorCommentRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private UserRepository userRepository;

    private ProfessorCommentService professorCommentService;

    @BeforeEach
    void setUp() {
        professorCommentService = new ProfessorCommentService(
            professorCommentRepository,
            professorRepository,
            userRepository
        );
    }

    @Test
    void 다른_사람의_교수_후기를_수정하면_403_예외가_발생한다() {
        // given
        ProfessorComment professorComment = otherUsersComment();

        ProfessorCommentUpdateRequestDto request =
            new ProfessorCommentUpdateRequestDto(
                5,
                "수정하려는 후기",
                ProfessorCommentAnonymity.TRUE
            );

        when(
            professorCommentRepository.findByProfessorCommentId(COMMENT_ID)
        ).thenReturn(Optional.of(professorComment));

        // when & then
        assertThatThrownBy(
            () -> professorCommentService.updateProfessorComment(
                REQUEST_USER_ID,
                PROFESSOR_ID,
                COMMENT_ID,
                request
            )
        )
            .isInstanceOf(BusinessException.class)
            .satisfies(exception -> {
                BusinessException businessException =
                    (BusinessException) exception;

                assertThat(businessException.getErrorCode())
                    .isEqualTo(
                        ProfessorCommentErrorCode
                            .PROFESSOR_COMMENT_FORBIDDEN
                    );

                assertThat(
                    businessException
                        .getErrorCode()
                        .getHttpStatus()
                        .value()
                ).isEqualTo(403);
            });
    }

    @Test
    void 다른_사람의_교수_후기를_삭제하면_403_예외가_발생한다() {
        // given
        ProfessorComment professorComment = otherUsersComment();

        when(
            professorCommentRepository.findByProfessorCommentId(COMMENT_ID)
        ).thenReturn(Optional.of(professorComment));

        // when & then
        assertThatThrownBy(
            () -> professorCommentService.deleteProfessorComment(
                REQUEST_USER_ID,
                PROFESSOR_ID,
                COMMENT_ID
            )
        )
            .isInstanceOf(BusinessException.class)
            .satisfies(exception -> {
                BusinessException businessException =
                    (BusinessException) exception;

                assertThat(businessException.getErrorCode())
                    .isEqualTo(
                        ProfessorCommentErrorCode
                            .PROFESSOR_COMMENT_FORBIDDEN
                    );

                assertThat(
                    businessException
                        .getErrorCode()
                        .getHttpStatus()
                        .value()
                ).isEqualTo(403);
            });

        verify(
            professorCommentRepository,
            never()
        ).delete(professorComment);
    }

    private ProfessorComment otherUsersComment() {
        User owner = mock(User.class);
        ProfessorComment professorComment =
            mock(ProfessorComment.class);

        when(owner.getUserId()).thenReturn(OWNER_USER_ID);
        when(professorComment.getUser()).thenReturn(owner);

        return professorComment;
    }
}
