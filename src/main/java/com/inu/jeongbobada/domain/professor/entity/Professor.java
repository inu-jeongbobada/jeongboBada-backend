package com.inu.jeongbobada.domain.professor.entity;

import com.inu.jeongbobada.domain.course.entity.Course;
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

    @Column(name = "PROFESSOR_DETAIL", columnDefinition = "TEXT", nullable = false)
    private String professorDetail;

    // 아직 프로필 사진 데이터가 없어 nullable 허용 (없으면 프론트에서 기본 이미지로 대체)
    @Column(name = "PROFESSOR_IMAGE_URL")
    private String professorImageUrl;

    // mappedBy -> 다른 DB 테이블에서 FK 관리
    @OneToOne(mappedBy = "professor", fetch = FetchType.LAZY)
    private Lab lab;

    @OneToMany(mappedBy = "professor", fetch = FetchType.LAZY)
    private List<ProfessorComment> professorComments;

    @OneToMany(mappedBy = "professor", fetch = FetchType.LAZY)
    private List<Course> courses;

    public Professor(Long professorId, String professorName, String professorDetail, String professorImageUrl) {
        this.professorId = professorId;
        this.professorName = professorName;
        this.professorDetail = professorDetail;
        this.professorImageUrl = professorImageUrl;
    }
}
