package com.inu.jeongbobada.domain.material.entity;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.material.enums.MaterialType;
import com.inu.jeongbobada.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "MATERIAL")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MATERIAL_ID")
    private Long materialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COURSE_ID", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "TITLE",length = 100,nullable = false)
    private String title;

    @Column(name = "CONTENT", columnDefinition = "TEXT",nullable = false)
    private String content;


    @Enumerated(EnumType.STRING)
    @Column(name = "COURSE_TYPE",nullable = false)
    private MaterialType materialType;

    @Column(name = "FILE_URL", length = 500)
    private String fileUrl;

    @Column(name = "ORIGINAL_FILE_NAME", length = 255)
    private String originalFileName;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;


    public Material(
        Course course,
        User user,
        String title,
        String content,
        MaterialType materialType,
        String fileUrl,
        String originalFileName,
        Long fileSize
    ) {
        this.course = course;
        this.user = user;
        this.title = title;
        this.content = content;
        this.materialType = materialType;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
    }




}
