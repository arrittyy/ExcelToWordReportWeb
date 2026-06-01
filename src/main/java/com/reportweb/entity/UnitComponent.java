package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "unit_components")
@Data
@EqualsAndHashCode(exclude = {"unit"})
@ToString(exclude = {"unit"})
public class UnitComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "unit_id", nullable = false)
    private Integer unitId;

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

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_unit_components_unit"))
    private Unit unit;

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
