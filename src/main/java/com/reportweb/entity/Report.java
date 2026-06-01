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
@Table(name = "reports")
@Data
@EqualsAndHashCode(exclude = {"project", "user", "reportItems", "imageAttachments"})
@ToString(exclude = {"project", "user", "reportItems", "imageAttachments"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // 归属项目
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "title", length = 200, nullable = false)
    private String title; // 单项报告标题

    @Column(name = "report_number", length = 50, nullable = false)
    private String reportNumber;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "experiment_type_id", nullable = false)
    private Integer experimentTypeId;

    @Column(name = "project_component_id")
    private Integer projectComponentId; // 关联检测部件（可选，多选时为第一个）

    /** 多选部件 ID 顺序；为空且 {@link #projectComponentId} 非空时读逻辑视为单元素列表 */
    @Column(name = "project_component_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> projectComponentIds;

    @Column(name = "project_instrument_id")
    private Integer projectInstrumentId; // 关联仪器设备（可选）

    // 检测基础信息（新增）
    @Column(name = "inspector", length = 300, nullable = false)
    private String inspector = "/"; // 检测人员

    @Column(name = "test_method", length = 200)
    private String testMethod; // 检测方法（如：连续法、剩磁法）

    @Column(name = "equipment", length = 200)
    private String equipment; // 使用仪器/设备

    @Column(name = "test_standard", length = 200)
    private String testStandard; // 检测标准（如：GB/T xxx-2020）

    @Column(name = "component_name", length = 200)
    private String componentName; // 部件名称（被检测的部件）

    // 设备信息（新增）
    @Column(name = "equipment_category", length = 100)
    private String equipmentCategory; // 设备类别

    @Column(name = "equipment_name", length = 200)
    private String equipmentName; // 设备名称

    @Column(name = "component_spec", length = 500)
    private String componentSpec; // 部件规格

    @Column(name = "instrument_model", length = 200)
    private String instrumentModel; // 仪器型号

    @Column(name = "instrument_number", length = 100)
    private String instrumentNumber; // 仪器编号（新增）

    // 原有字段
    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "location", length = 200, nullable = false)
    private String location = "/";

    @Column(name = "report_image", length = 500)
    private String reportImage; // 报告附图URL

    @Column(name = "has_defect", length = 10)
    private String hasDefect; // 是否存在缺陷

    @Column(name = "status", length = 20, nullable = false)
    private String status = "Draft"; // Draft, Completed

    // 特殊字段（JSON）
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> customFields; // 存储检测类型特有字段

    @Column(name = "detection_content", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> detectionContent; // 存储检测内容子卡片数据

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_reports_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_reports_user"))
    private User user;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReportItem> reportItems = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ImageAttachment> imageAttachments = new ArrayList<>();

    // Manual getters and setters for critical fields
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReportNumber() {
        return reportNumber;
    }

    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(String inspector) {
        this.inspector = inspector;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getTestStandard() {
        return testStandard;
    }

    public void setTestStandard(String testStandard) {
        this.testStandard = testStandard;
    }

    public LocalDate getTestDate() {
        return testDate;
    }

    public void setTestDate(LocalDate testDate) {
        this.testDate = testDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<ReportItem> getReportItems() {
        return reportItems;
    }

    public void setReportItems(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}



