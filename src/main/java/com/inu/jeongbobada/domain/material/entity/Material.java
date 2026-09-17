package com.inu.jeongbobada.domain.material.entity;
import com.inu.jeongbobada.domain.course.entity.Course;
import com.inu.jeongbobada.domain.material.enums.MaterialType;
import com.inu.jeongbobada.domain.user.entity.User;
import com.inu.jeongbobada.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "MATERIAL",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_USER_COURSE_MATERIAL",
            columnNames = {"USER_ID", "COURSE_ID"}
        )
    }
)
public class Material extends BaseEntity {
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

    @NotBlank
    @Size(min = 10, max = 50)
    @Column(name = "TITLE",length = 50,nullable = false)
    private String title;

    @NotBlank
    @Size(min = 20, max = 500)
    @Column(name = "CONTENT", length = 500,nullable = false)
    private String content;


    @Enumerated(EnumType.STRING)
    @Column(name = "MATERIAL_TYPE",nullable = false)
    private MaterialType materialType;

    // 파일 업로드 선택형-> null 허용

    @Column(name = "FILE_URL", length = 500)
    private String fileUrl;

    @Column(name = "ORIGINAL_FILE_NAME", length = 100)
    private String originalFileName;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

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
