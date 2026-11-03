package com.inu.jeongbobada.domain.user.repository;

import com.inu.jeongbobada.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentId(String studentId);

    // 로그인 실패 횟수를 동시 요청에서도 빠뜨리지 않으려고 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.studentId = :studentId")
    Optional<User> findByStudentIdForUpdate(@Param("studentId") String studentId);

    Optional<User> findByNickname(String nickname);
    Optional<User> findByEmail(String email);

    boolean existsByStudentId(String studentId);
    boolean existsByNickname(String nickname);
}
