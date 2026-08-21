package com.inu.jeongbobada.domain.professor.entity;

import com.inu.jeongbobada.domain.professorComment.entity.ProfessorComment;
import com.inu.jeongbobada.domain.lab.entity.Lab;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PROFESSOR")
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROFESSOR_ID")
    private Long professorId;

    @Column(name = "PROFESSOR_NAME", nullable = false, length = 20)
    private String professorName;

    @Column(name = "PROFESSOR_DETAIL", nullable = false)
    private String professorDetail;

    @Column(name = "PROFESSOR_IMAGE_URL", nullable = false)
    private String professorImageUrl;

    // mappedBy -> 다른 DB 테이블에서 FK 관리
    @OneToOne(mappedBy = "professor", fetch = FetchType.LAZY)
    private Lab lab;

    @OneToMany(mappedBy = "professor", fetch = FetchType.LAZY)
    private List<ProfessorComment> professorComments;

    // course 아직 작성 X
    // course 작성 후 생성자 작성
}
