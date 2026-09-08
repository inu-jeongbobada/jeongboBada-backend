package com.inu.jeongbobada.domain.professor.service;

import com.inu.jeongbobada.domain.professor.dto.ProfessorDetailResponseDto;
import com.inu.jeongbobada.domain.professor.dto.ProfessorListResponseDto;
import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.professor.exception.ProfessorErrorCode;
import com.inu.jeongbobada.domain.professor.repository.ProfessorRepository;
import com.inu.jeongbobada.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {
    private final ProfessorRepository professorRepository;

    public List<ProfessorListResponseDto> getProfessorLists() {
        List<Professor> professorLists = professorRepository.findAll();

        return professorLists.stream().map(ProfessorListResponseDto::from).toList();
    }

    public ProfessorDetailResponseDto getProfessorDetail(Long professorId) {
        Professor professorDetail = professorRepository.findByProfessorId(professorId)
            .orElseThrow(() -> new BusinessException(ProfessorErrorCode.PROFESSOR_NOT_FOUND));

        return ProfessorDetailResponseDto.from(professorDetail);
    }
}
