package com.inu.jeongbobada.domain.professorComment.entity;

import com.inu.jeongbobada.domain.professor.entity.Professor;
import com.inu.jeongbobada.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PROFESSOR_COMMENT")
public class ProfessorComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROFESSOR_COMMENT_ID")
    private Long professorCommentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESSOR_ID", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "PROFESSOR_COMMENT_DETAIL", columnDefinition = "TEXT", nullable = false)
    private String professorCommentDetail;

    @Column(name = "PROFESSOR_COMMENT_RATE", nullable = false)
    private int professorCommentRate;

    @CreationTimestamp
    @Column(name = "PROFESSOR_COMMENT_DATE", nullable = false)
    private LocalDateTime professorCommentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROFESSOR_COMMENT_ANONYMITY", nullable = false)
    private ProfessorCommentAnonymity professorCommentAnonymity;

    public ProfessorComment(Professor professor, User user, String professorCommentDetail, int professorCommentRate, ProfessorCommentAnonymity professorCommentAnonymity) {
        this.professor = professor;
        this.user = user;
        this.professorCommentDetail = professorCommentDetail;
        this.professorCommentRate = professorCommentRate;
        this.professorCommentAnonymity = professorCommentAnonymity;
    }
}
