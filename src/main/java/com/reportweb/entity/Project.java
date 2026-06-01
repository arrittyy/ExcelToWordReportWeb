package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "projects")
@Data
@EqualsAndHashCode(exclude = {"user", "reports", "projectImageAttachments"})
@ToString(exclude = {"user", "reports", "projectImageAttachments"})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "project_number", length = 50, nullable = false, unique = true)
    private String projectNumber;

    /** 第三方项目编号（可选，不参与唯一性校验） */
    @Column(name = "third_party_project_number", length = 100)
    private String thirdPartyProjectNumber;

    /** 第三方名称（可选） */
    @Column(name = "third_party_name", length = 200)
    private String thirdPartyName;

    @Column(name = "project_name", length = 200, nullable = false)
    private String projectName;

    @Column(name = "project_type", length = 50)
    private String projectType;

    @Column(name = "customer", length = 200)
    private String customer;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_contact", length = 200)
    private String customerContact;

    @Column(name = "power_plant_id")
    private Integer powerPlantId;

    @Column(name = "unit_id")
    private Integer unitId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "InProgress";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "selected_experiment_type_ids", columnDefinition = "TEXT")
    private String selectedExperimentTypeIds; // Stored as JSON string

    @Column(name = "responsible_person", length = 100)
    private String responsiblePerson;

    // 无损检测相关字段
    @Column(name = "reviewer_ndt", length = 100)
    private String reviewerNdt;

    @Column(name = "review_date_ndt")
    private LocalDate reviewDateNdt;

    @Column(name = "approver_ndt", length = 100)
    private String approverNdt;

    @Column(name = "approval_date_ndt")
    private LocalDate approvalDateNdt;

    @Column(name = "writer_ndt", length = 100)
    private String writerNdt;

    @Column(name = "writer_date_ndt")
    private LocalDate writerDateNdt;

    // 理化检测相关字段
    @Column(name = "reviewer_chem", length = 100)
    private String reviewerChem;

    @Column(name = "review_date_chem")
    private LocalDate reviewDateChem;

    @Column(name = "approver_chem", length = 100)
    private String approverChem;

    @Column(name = "approval_date_chem")
    private LocalDate approvalDateChem;

    @Column(name = "writer_chem", length = 100)
    private String writerChem;

    @Column(name = "writer_date_chem")
    private LocalDate writerDateChem;

    /** 无损检测审批步骤: 0=编写 1=待审核 2=待批准 3=已通过 */
    @Column(name = "approval_step_ndt")
    private Integer approvalStepNdt = 0;

    /** 理化检测审批步骤: 0=编写 1=待审核 2=待批准 3=已通过 */
    @Column(name = "approval_step_chem")
    private Integer approvalStepChem = 0;

    /** 无损检测：曾不通过的节点（1=审核 2=批准），null 表示无 */
    @Column(name = "rejection_step_ndt")
    private Integer rejectionStepNdt;

    /** 理化检测：曾不通过的节点 */
    @Column(name = "rejection_step_chem")
    private Integer rejectionStepChem;

    @Column(name = "staff", length = 200)
    private String staff;

    /** 无损签名级别：按 writer/reviewer + 方法编码(MT/PT/UT/RT/ET) 存储 */
    @Column(name = "ndt_signature_levels", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Map<String, String>> ndtSignatureLevels;

    /**
     * 第三方单项签批：按检测类型 ID（字符串 key）存编写/审核/批准及日期，可选 writerLevel/reviewerLevel（无损 MT/PT/UT/RT/ET）。
     */
    @Column(name = "third_party_approval_by_experiment_type", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Map<String, String>> thirdPartyApprovalByExperimentType;

    /** 总报告附件：通知单签字版（相对 file.upload-dir 的路径，如 projects/1/notification_signed.docx） */
    @Column(name = "summary_notification_signed_rel_path", length = 500)
    private String summaryNotificationSignedRelPath;

    @Column(name = "summary_notification_signed_original_name", length = 255)
    private String summaryNotificationSignedOriginalName;

    /** 总报告附件：第三方完整版（路径 + 原始文件名） */
    @Column(name = "summary_third_party_full_rel_path", length = 500)
    private String summaryThirdPartyFullRelPath;

    @Column(name = "summary_third_party_full_original_name", length = 255)
    private String summaryThirdPartyFullOriginalName;

    /** 总检测日志（按部件）用户自定义顺序，JSON：version、componentKeys、reportIdsByComponent */
    @Column(name = "aggregate_detection_log_order", columnDefinition = "TEXT")
    private String aggregateDetectionLogOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_projects_user"))
    private User user;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reports = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectImageAttachment> projectImageAttachments = new ArrayList<>();

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

