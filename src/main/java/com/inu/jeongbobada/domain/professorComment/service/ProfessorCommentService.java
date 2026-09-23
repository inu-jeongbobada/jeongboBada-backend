package com.inu.jeongbobada.domain.professorComment.service;

import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.exception.ProfessorErrorCode;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentCreateRequestDto;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentUpdateRequestDto;
import com.inu.jeongbobada.domain.professorComment.entity.ProfessorComment;
import com.inu.jeongbobada.domain.professorComment.exception.ProfessorCommentErrorCode;
import com.inu.jeongbobada.domain.professorComment.repository.ProfessorCommentRepository;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.repository.UserRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfessorCommentService {
    private final ProfessorCommentRepository professorCommentRepository;
    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createProfessorComment(Long userId, Long professorId, ProfessorCommentCreateRequestDto professorCommentCreateRequestDto) {
        Professor professor = professorRepository.findByProfessorId(professorId)
            .orElseThrow(() -> new BusinessException(ProfessorErrorCode.PROFESSOR_NOT_FOUND));
        User user = userRepository.getReferenceById(userId);

        ProfessorComment professorComment = ProfessorComment.create(
            professor,
            user,
            professorCommentCreateRequestDto.getProfessorCommentRate(),
            professorCommentCreateRequestDto.getProfessorCommentDetail(),
            professorCommentCreateRequestDto.getProfessorCommentAnonymity()
        );

        professorCommentRepository.save(professorComment);
    }

    @Transactional
    public void updateProfessorComment(Long userId, Long professorId, Long professorCommentId, ProfessorCommentUpdateRequestDto professorCommentUpdateRequestDto) {
        ProfessorComment professorComment = professorCommentRepository.findByProfessorCommentId(professorCommentId)
            .orElseThrow(() -> new BusinessException(ProfessorCommentErrorCode.PROFESSOR_COMMENT_NOT_FOUND));

        if (!professorComment.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ProfessorCommentErrorCode.PROFESSOR_COMMENT_FORBIDDEN);
        }

        if (!professorComment.getProfessor().getProfessorId().equals(professorId)) {
            throw new BusinessException(ProfessorCommentErrorCode.PROFESSOR_COMMENT_PROFESSOR_MISMATCH);
        }

        professorComment.update(
            professorCommentUpdateRequestDto.getProfessorCommentRate(),
            professorCommentUpdateRequestDto.getProfessorCommentDetail(),
            professorCommentUpdateRequestDto.getProfessorCommentAnonymity()
        );
    }

    @Transactional
    public void deleteProfessorComment(Long userId, Long professorId, Long professorCommentId) {
        ProfessorComment professorComment = professorCommentRepository.findByProfessorCommentId((professorCommentId))
            .orElseThrow(() -> new BusinessException(ProfessorCommentErrorCode.PROFESSOR_COMMENT_NOT_FOUND));

        if (!professorComment.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ProfessorCommentErrorCode.PROFESSOR_COMMENT_FORBIDDEN);
        }

        if (!professorComment.getProfessor().getProfessorId().equals(professorId)) {
            throw new BusinessException(ProfessorCommentErrorCode.PROFESSOR_COMMENT_PROFESSOR_MISMATCH);
        }

        professorCommentRepository.delete(professorComment);
    }
}
