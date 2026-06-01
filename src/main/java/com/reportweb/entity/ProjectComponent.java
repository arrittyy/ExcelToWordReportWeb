package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_components")
@Data
public class ProjectComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "component_name", nullable = false, length = 255)
    private String componentName;

    @Column(name = "material", length = 100)
    private String material;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "pipe_diameter", length = 50)
    private String pipeDiameter;

    @Column(name = "wall_thickness", length = 50)
    private String wallThickness;

    /** PHI | M | NONE；null 表示按部件名称自动（螺栓/螺帽→M，否则Φ） */
    @Column(name = "spec_prefix", length = 8)
    private String specPrefix;

    @Column(name = "thread_pitch", length = 50)
    private String threadPitch;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}



