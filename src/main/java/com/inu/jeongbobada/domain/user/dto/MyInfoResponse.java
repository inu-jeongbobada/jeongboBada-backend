package com.inu.jeongbobada.domain.user.dto;

import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.domain.user.entity.UserRole;

// 본인 조회(GET /api/users/me) 전용 — studentId·email이 들어 있으므로
// 다른 사용자에게 보이는 응답(후기·댓글 등)에서 재사용하지 않는다.
// email: 이메일 기능 이전 가입자는 null, department: 가입 때 받지 않아 현재는 항상 null
// role: 프론트가 관리자 화면을 보여줄지 정하는 용도. 실제 권한은 관리자 API가 hasRole('ADMIN')으로 다시 막는다
public record MyInfoResponse(
    String nickname,
    String studentId,
    String email,
    String department,
    UserRole role
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
            user.getNickname(), user.getStudentId(), user.getEmail(), user.getDepartment(), user.getUserRole());
    }
}
