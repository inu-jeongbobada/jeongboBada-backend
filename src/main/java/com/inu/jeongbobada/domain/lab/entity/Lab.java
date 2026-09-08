package com.inu.jeongbobada.domain.lab.entity;

import com.inu.jeongbobada.domain.professor.entity.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "LAB")
public class Lab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LAB_ID")
    private Long labId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFESSOR_ID", nullable = false)
    private Professor professor;

    @Column(name = "LAB_NAME", nullable = false, length = 100)
    private String labName;

    @Column(name = "LAB_DETAIL", columnDefinition = "TEXT", nullable = false)
    private String labDetail;

    @Column(name = "LAB_URL", nullable = false)
    private String labUrl;

    public Lab(Professor professor, String labName, String labDetail, String labUrl) {
        this.professor = professor;
        this.labName = labName;
        this.labDetail = labDetail;
        this.labUrl = labUrl;
    }
}
