package com.inu.jeongbobada.domain.professorComment.entity;

import com.inu.jeongbobada.domain.professor.entity.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "PROFESSOR_COMMENT")
public class ProfessorComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROFESSOR_COMMENT_ID")
    private Long professorCommentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESSOR_ID")
    private Professor professor;

    @Column(name = "PROFESSOR_COMMENT_DETAIL")
    private String professorCommentDetail;

    @Column(name = "PROFESSOR_COMMENT_RATE")
    private int professorCommentRate;

    @Column(name = "PROFESSOR_COMMENT_DATE")
    private String professorCommentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROFESSOR_COMMENT_ANNONYMITY")
    private ProfessorCommentAnonymity professorCommentAnonymity;

    // user 아직 작성 X
}
