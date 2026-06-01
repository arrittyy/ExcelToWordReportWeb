package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_items")
@Data
@EqualsAndHashCode(exclude = {"report", "experimentType"})
@ToString(exclude = {"report", "experimentType"})
public class ReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "report_id", nullable = false)
    private Integer reportId;

    @Column(name = "experiment_type_id", nullable = false)
    private Integer experimentTypeId;

    @Column(name = "table_data", columnDefinition = "TEXT", nullable = false)
    private String tableData; // JSON string

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Navigation properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", insertable = false, updatable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_type_id", insertable = false, updatable = false)
    private ExperimentType experimentType;

    // Manual getters and setters for critical fields
    public ExperimentType getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(ExperimentType experimentType) {
        this.experimentType = experimentType;
    }

    public String getTableData() {
        return tableData;
    }

    public void setTableData(String tableData) {
        this.tableData = tableData;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}


