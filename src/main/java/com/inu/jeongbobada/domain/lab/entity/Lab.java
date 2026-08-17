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
    @JoinColumn(name = "PROFESSOR_ID")
    private Professor professor;

    @Column(name = "LAB_NAME")
    private String labName;

    @Column(name = "LAB_DETAIL")
    private String labDetail;

    @Column(name = "LAB_URL")
    private String labURL;
}
