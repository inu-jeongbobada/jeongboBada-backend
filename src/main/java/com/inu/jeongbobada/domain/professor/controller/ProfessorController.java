package com.inu.jeongbobada.domain.professor.controller;

import com.inu.jeongbobada.domain.professor.dto.ProfessorDetailResponseDto;
import com.inu.jeongbobada.domain.professor.dto.ProfessorListResponseDto;
import com.inu.jeongbobada.domain.professor.service.ProfessorService;
import com.inu.jeongbobada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProfessorController implements ProfessorControllerDocs {
    private final ProfessorService professorService;

    @Override
    public ResponseEntity<ApiResponse<List<ProfessorListResponseDto>>> getProfessorLists() {
        List<ProfessorListResponseDto> professorLists = professorService.readProfessorLists();

        ApiResponse<List<ProfessorListResponseDto>> apiResponse = ApiResponse.ok(professorLists);

        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    public ResponseEntity<ApiResponse<ProfessorDetailResponseDto>> getProfessorDetail(@PathVariable Long professorId) {
        ProfessorDetailResponseDto professorDetail = professorService.readProfessorDetail(professorId);

        ApiResponse<ProfessorDetailResponseDto> apiResponse = ApiResponse.ok(professorDetail);

        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
