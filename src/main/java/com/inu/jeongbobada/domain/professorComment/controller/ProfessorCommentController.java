package com.inu.jeongbobada.domain.professorComment.controller;

import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentCreateRequestDto;
import com.inu.jeongbobada.domain.professorComment.dto.ProfessorCommentUpdateRequestDto;
import com.inu.jeongbobada.domain.professorComment.service.ProfessorCommentService;
import com.inu.jeongbobada.domain.user.security.CustomUserDetails;
import com.inu.jeongbobada.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfessorCommentController implements ProfessorCommentControllerDocs {
    private final ProfessorCommentService professorCommentService;

    @Override
    public ResponseEntity<ApiResponse<Void>> createProfessorComment(
        Long professorId,
        ProfessorCommentCreateRequestDto professorCommentCreateRequest,
        CustomUserDetails customUserDetails
    ) {
        Long userId = customUserDetails.getUserId();

        professorCommentService.createProfessorComment(userId, professorId, professorCommentCreateRequest);

        ApiResponse<Void> apiResponse = ApiResponse.created(null);

        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> updateProfessorComment(
        Long professorId,
        Long professorCommentId,
        ProfessorCommentUpdateRequestDto professorCommentUpdateRequest,
        CustomUserDetails customUserDetails
    ) {
        Long userId = customUserDetails.getUserId();

        professorCommentService.updateProfessorComment(userId, professorId, professorCommentId, professorCommentUpdateRequest);

        ApiResponse<Void> apiResponse = ApiResponse.ok(null);

        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteProfessorComment(
        Long professorId,
        Long professorCommentId,
        CustomUserDetails customUserDetails
    ) {
        Long userId = customUserDetails.getUserId();

        professorCommentService.deleteProfessorComment(userId, professorId, professorCommentId);

        ApiResponse<Void> apiResponse = ApiResponse.ok(null);

        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
