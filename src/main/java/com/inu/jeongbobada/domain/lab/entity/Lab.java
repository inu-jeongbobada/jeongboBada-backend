package com.inu.jeongbobada.domain.lab.entity;

import com.inu.jeongbobada.domain.professor.entity.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "LAB_NAME", length = 100)
    private String labName;

    @Column(name = "LAB_DETAIL", columnDefinition = "TEXT")
    private String labDetail;

    @Column(name = "LAB_URL")
    private String labUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "LAB_STUDY", columnDefinition = "json")
    private String [] labStudy = new String[10];

    public Lab(
        Professor professor,
        String labName,
        String labDetail,
        String labUrl,
        String [] labStudy
    ) {
        this.professor = professor;
        this.labName = labName;
        this.labDetail = labDetail;
        this.labUrl = labUrl;
        this.labStudy = labStudy;
    }
}
