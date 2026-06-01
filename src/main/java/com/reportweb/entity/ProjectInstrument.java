package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_instruments")
@Data
public class ProjectInstrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "instrument_name", nullable = false, length = 255)
    private String instrumentName;

    @Column(name = "instrument_model", length = 255)
    private String instrumentModel;

    @Column(name = "instrument_number", length = 100)
    private String instrumentNumber;

    @Column(name = "global_instrument_id")
    private Integer globalInstrumentId;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "experiment_type_code", length = 20)
    private String experimentTypeCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation property
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
