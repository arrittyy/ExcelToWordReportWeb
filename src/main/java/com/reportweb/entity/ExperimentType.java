package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "experiment_types")
@Data
@EqualsAndHashCode(exclude = {"reportItems"})
@ToString(exclude = {"reportItems"})
public class ExperimentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "table_schema", columnDefinition = "TEXT", nullable = false)
    private String tableSchema; // JSON string

    @Column(name = "report_fields_schema", columnDefinition = "TEXT", nullable = false)
    private String reportFieldsSchema; // JSON string

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Navigation properties
    @OneToMany(mappedBy = "experimentType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReportItem> reportItems = new ArrayList<>();
}


