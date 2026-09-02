package com.inu.jeongbobada.domain.professor.service;

import com.inu.jeongbobada.domain.professor.dto.ProfessorDetailResponseDto;
import com.inu.jeongbobada.domain.professor.dto.ProfessorListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessorService {
    public List<ProfessorListResponseDto> readProfessorLists() {

    }

    public ProfessorDetailResponseDto readProfessorDetail(Long professorId) {

    }
}
