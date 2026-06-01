package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "units")
@Data
@EqualsAndHashCode(exclude = {"powerPlant"})
@ToString(exclude = {"powerPlant"})
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "power_plant_id", nullable = false)
    private Integer powerPlantId;

    @Column(name = "unit_name", nullable = false, length = 200)
    private String unitName;

    @Column(name = "unit_number", length = 50)
    private String unitNumber;

    @Column(name = "installed_capacity", length = 100)
    private String installedCapacity;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_units_power_plant"))
    private PowerPlant powerPlant;

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
