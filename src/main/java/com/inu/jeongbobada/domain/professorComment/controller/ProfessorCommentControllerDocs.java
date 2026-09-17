package com.inu.jeongbobada.domain.professorComment.controller;

import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentCreateRequestDto;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentUpdateRequestDto;
import com.inu.jeongbobada.domain.user.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "professorComment", description = "professorComment 관련 API")
@RequestMapping("/api/professors/{professorId}/professor-comments")
public interface ProfessorCommentControllerDocs {
    @Operation(summary = "professorComment create API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "교수 후기 작성 성공"
        )
    })
    @PostMapping("")
    ResponseEntity<com.inu.jeongbobada.global.common.ApiResponse<Void>> createProfessorComment(
        @PathVariable Long professorId,
        @Valid @RequestBody ProfessorCommentCreateRequestDto professorCommentCreateRequest,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
        );

    @Operation(summary = "professorComment update API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "교수 후기 수정 성공"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 교수 후기"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "수정할 권한이 없는 교수 후기"
        )
    })
    @PatchMapping("/{professorCommentId}")
    ResponseEntity<com.inu.jeongbobada.global.common.ApiResponse<Void>> updateProfessorComment(
        @PathVariable Long professorId,
        @PathVariable Long professorCommentId,
        @Valid @RequestBody ProfessorCommentUpdateRequestDto professorCommentUpdateRequest,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(summary = "professorComment delete API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "교수 후기 삭제 성공"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 교수 후기"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "삭제할 권한이 없는 교수 후기"
        )
    })
    @DeleteMapping("/{professorCommentId}")
    ResponseEntity<com.inu.jeongbobada.global.common.ApiResponse<Void>> deleteProfessorComment(
        @PathVariable Long professorId,
        @PathVariable Long professorCommentId,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}
