package com.inu.jeongbobada.domain.professor.repository;

import com.inu.jeongbobada.domain.professor.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    Optional<Professor> findByProfessorId(Long ProfessorId);
}
