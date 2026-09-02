package com.inu.jeongbobada.domain.professor.controller;

import com.inu.jeongbobada.domain.professor.dto.ProfessorDetailResponseDto;
import com.inu.jeongbobada.domain.professor.dto.ProfessorListResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "professor", description = "professor 관련 API")
@RequestMapping("/api/professors")
public interface ProfessorControllerDocs {
    @Operation(summary = "professor List API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "교수 목록 조회 성공"
        )
    })
    @GetMapping("")
    com.inu.jeongbobada.global.common.ApiResponse<List<ProfessorListResponseDto>> getProfessorLists();

    @Operation(summary = "professor Detail API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "교수 상세 조회 성공"
        )
    })
    @GetMapping("/{professorId}")
    com.inu.jeongbobada.global.common.ApiResponse<ProfessorDetailResponseDto> getProfessorDetail(@PathVariable Long professorId);
}
