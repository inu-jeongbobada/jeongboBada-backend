package com.inu.jeongbobada.domain.professorComment.repository;

import com.inu.jeongbobada.domain.professorComment.entity.ProfessorComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessorCommentRepository extends JpaRepository<ProfessorComment, Long> {
    Optional<ProfessorComment> findByProfessorCommentId(Long professorCommentId);
}
