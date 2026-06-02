package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "project_report_change_log")
@Data
public class ProjectReportChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "report_id", nullable = false)
    private Integer reportId;

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "experiment_type_id", nullable = false)
    private Integer experimentTypeId;

    @Column(name = "experiment_type_name", length = 200, nullable = false)
    private String experimentTypeName;

    @Column(name = "experiment_type_code", length = 20, nullable = false)
    private String experimentTypeCode;

    @Column(name = "report_number", length = 50)
    private String reportNumber;

    @Column(name = "test_method", length = 200)
    private String testMethod;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "change_summary", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> changeSummary;

    @Column(name = "operator_user_id", length = 450, nullable = false)
    private String operatorUserId;

    @Column(name = "operator_user_name", length = 200)
    private String operatorUserName;

    @Column(name = "source", length = 32, nullable = false)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
