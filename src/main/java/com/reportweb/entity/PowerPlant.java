package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "power_plants")
@Data
@EqualsAndHashCode(exclude = {"units"})
@ToString(exclude = {"units"})
public class PowerPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 200, unique = true)
    private String name;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(name = "province", nullable = false, length = 50)
    private String province;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "fax", length = 50)
    private String fax;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation properties
    @OneToMany(mappedBy = "powerPlant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Unit> units = new ArrayList<>();

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
