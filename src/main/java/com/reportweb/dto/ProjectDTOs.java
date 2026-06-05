package com.reportweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.reportweb.dto.ReportDTOs.ReportList;
import com.reportweb.entity.WordExportJobStatus;
import com.reportweb.entity.WordExportJobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProjectDTOs {

    @Data
    public static class ProjectList {
        private Integer id;
        private String projectNumber;
        /** 第三方项目编号（可选） */
        private String thirdPartyProjectNumber;
        /** 第三方名称（可选） */
        private String thirdPartyName;
        private String projectName;
        private String projectType;
        private String customer;
        private String customerContact;
        private Integer powerPlantId;
        private Integer unitId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Integer reportCount;
        private LocalDateTime createdAt;
        private String userFullName;
        private String responsiblePerson;
        
        // 无损检测相关字段
        private String reviewerNdt;
        private LocalDate reviewDateNdt;
        private String approverNdt;
        private LocalDate approvalDateNdt;
        private String writerNdt;
        private LocalDate writerDateNdt;
        
        // 理化检测相关字段
        private String reviewerChem;
        private LocalDate reviewDateChem;
        private String approverChem;
        private LocalDate approvalDateChem;
        private String writerChem;
        private LocalDate writerDateChem;
        /** 无损审批步骤 0=编写 1=待审核 2=待批准 3=已通过 */
        private Integer approvalStepNdt;
        /** 理化审批步骤 */
        private Integer approvalStepChem;
        /** 无损：曾不通过的节点 1=审核 2=批准 */
        private Integer rejectionStepNdt;
        private Integer rejectionStepChem;
        private String staff;
        private Map<String, Map<String, String>> ndtSignatureLevels;
        /** 第三方单项签批信息：key = experimentTypeId 字符串 */
        private Map<String, Map<String, String>> thirdPartyApprovalByExperimentType;
    }

    @Data
    public static class ProjectDetail {
        private Integer id;
        private String projectNumber;
        /** 第三方项目编号（可选） */
        private String thirdPartyProjectNumber;
        /** 第三方名称（可选） */
        private String thirdPartyName;
        private String projectName;
        private String projectType;
        private String customer;
        private String customerContact;
        private Integer powerPlantId;
        private Integer unitId;
        private String unitNumber; // 机组编号
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String userFullName;
        private String responsiblePerson;
        
        // 无损检测相关字段
        private String reviewerNdt;
        private LocalDate reviewDateNdt;
        private String approverNdt;
        private LocalDate approvalDateNdt;
        private String writerNdt;
        private LocalDate writerDateNdt;
        
        // 理化检测相关字段
        private String reviewerChem;
        private LocalDate reviewDateChem;
        private String approverChem;
        private LocalDate approvalDateChem;
        private String writerChem;
        private LocalDate writerDateChem;
        private Integer approvalStepNdt;
        private Integer approvalStepChem;
        private Integer rejectionStepNdt;
        private Integer rejectionStepChem;
        private String staff;
        private Map<String, Map<String, String>> ndtSignatureLevels;
        private String summaryNotificationSignedRelPath;
        private String summaryNotificationSignedOriginalName;
        private String summaryThirdPartyFullRelPath;
        private String summaryThirdPartyFullOriginalName;
        private List<ImageAttachmentDTO> reportFigures;
        private List<Integer> selectedExperimentTypeIds;
        private List<ReportList> reports;
        /** 总检测日志按部件顺序（JSON 字符串，与库列一致） */
        private String aggregateDetectionLogOrder;
        /** 第三方单项签批信息：key = experimentTypeId 字符串 */
        private Map<String, Map<String, String>> thirdPartyApprovalByExperimentType;
    }

    @Data
    public static class AggregateDetectionLogOrderUpdate {
        private Integer version;
        private List<String> componentKeys;
        private Map<String, List<Integer>> reportIdsByComponent;
        /** 全局检测类型顺序（名称）；可选，与前端 aggregateDetectionLogOrder.experimentTypeOrder 一致 */
        private List<String> experimentTypeOrder;
    }

    @Data
    public static class ApprovalLogItem {
        private Long id;
        private Integer projectId;
        private String track;
        private String action;
        private String actorName;
        private java.time.LocalDateTime createdAt;
    }

    @Data
    public static class ReportChangeLogItem {
        private Long id;
        private Integer projectId;
        private Integer reportId;
        private String action;
        private Integer experimentTypeId;
        private String experimentTypeName;
        private String experimentTypeCode;
        private String reportNumber;
        private String testMethod;
        private String status;
        private java.util.Map<String, Object> changeSummary;
        private String operatorUserId;
        private String operatorUserName;
        private String source;
        private java.time.LocalDateTime createdAt;
        /** 报告已删除时为 true，仅展示用，不可跳转详情 */
        private Boolean reportDeleted;
    }

    @Data
    public static class ReportChangeLogSummaryRow {
        private Integer experimentTypeId;
        private String experimentTypeName;
        private String experimentTypeCode;
        private long createdCount;
        private long updatedCount;
        private long deletedCount;
        private long currentReportCount;
    }

    @Data
    public static class ReportChangeLogSummaryResponse {
        private List<ReportChangeLogSummaryRow> byExperimentType;
    }

    @Data
    public static class CreateProject {
        @NotBlank(message = "项目编号不能为空")
        @Size(max = 50, message = "项目编号长度不能超过50个字符")
        private String projectNumber;

        @Size(max = 100, message = "第三方项目编号长度不能超过100个字符")
        private String thirdPartyProjectNumber;

        @Size(max = 200, message = "第三方名称长度不能超过200个字符")
        private String thirdPartyName;

        @NotBlank(message = "项目名称不能为空")
        @Size(max = 200, message = "项目名称长度不能超过200个字符")
        private String projectName;

        @NotBlank(message = "项目类型不能为空")
        @Size(max = 50, message = "项目类型长度不能超过50个字符")
        private String projectType;

        private String customer;
        private String customerContact;
        private Integer powerPlantId;
        private Integer unitId;

        @NotNull(message = "开始日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @Size(max = 1000, message = "描述长度不能超过1000个字符")
        private String description;

        private String responsiblePerson;
        
        // 无损检测相关字段
        private String reviewerNdt;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reviewDateNdt;
        private String approverNdt;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate approvalDateNdt;
        private String writerNdt;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate writerDateNdt;
        private Map<String, Map<String, String>> ndtSignatureLevels;
        
        // 理化检测相关字段
        private String reviewerChem;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reviewDateChem;
        private String approverChem;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate approvalDateChem;
        private String writerChem;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate writerDateChem;
        
        private String staff;
        private List<Integer> selectedExperimentTypeIds;
        /** 第三方单项签批信息：key = experimentTypeId 字符串 */
        private Map<String, Map<String, String>> thirdPartyApprovalByExperimentType;
    }

    @Data
    public static class UpdateProject {
        @NotBlank(message = "项目编号不能为空")
        @Size(max = 50, message = "项目编号长度不能超过50个字符")
        private String projectNumber;

        @Size(max = 100, message = "第三方项目编号长度不能超过100个字符")
        private String thirdPartyProjectNumber;

        @Size(max = 200, message = "第三方名称长度不能超过200个字符")
        private String thirdPartyName;

        @NotBlank(message = "项目名称不能为空")
        @Size(max = 200, message = "项目名称长度不能超过200个字符")
        private String projectName;

        @NotBlank(message = "项目类型不能为空")
        @Size(max = 50, message = "项目类型长度不能超过50个字符")
        private String projectType;

        private String customer;
        private String customerContact;
        private Integer powerPlantId;
        private Integer unitId;

        @NotNull(message = "开始日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        @NotBlank(message = "状态不能为空")
        @Size(max = 20, message = "状态长度不能超过20个字符")
        private String status;

        @Size(max = 1000, message = "描述长度不能超过1000个字符")
        private String description;

        private String responsiblePerson;
        
        // 无损检测相关字段
        private String reviewerNdt;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reviewDateNdt;
        private String approverNdt;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate approvalDateNdt;
        private String writerNdt;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate writerDateNdt;
        private Map<String, Map<String, String>> ndtSignatureLevels;
        
        // 理化检测相关字段
        private String reviewerChem;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reviewDateChem;
        private String approverChem;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate approvalDateChem;
        private String writerChem;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate writerDateChem;
        
        private String staff;
        private List<Integer> selectedExperimentTypeIds;
        /** 第三方单项签批信息：key = experimentTypeId 字符串 */
        private Map<String, Map<String, String>> thirdPartyApprovalByExperimentType;
    }

    /** 个人代办项：按姓名匹配角色与当前节点 */
    @Data
    public static class TodoItem {
        private Integer projectId;
        private String projectNumber;
        private String projectName;
        private String customer;
        /** 轨道：ndt / chem */
        private String track;
        /** 角色：writer / reviewer / approver */
        private String role;
        /** 当前步骤 0=编写 1=待审核 2=待批准 3=已通过 */
        private Integer step;
        /** 当前节点中文描述 */
        private String stepLabel;
    }

    @Data
    public static class SubmitApprovalRequest {
        /** ndt, chem 或两者都提交 */
        private String track; // "ndt" | "chem" | "both"
    }

    @Data
    public static class ApprovalActionRequest {
        private String track; // "ndt" | "chem"
    }

    /** 生成技术监督检测通知单（仅传报告 ID，可跨多个检测日期） */
    @Data
    public static class DetectionNotificationRequest {
        @NotEmpty(message = "请至少选择一条报告")
        private List<Integer> reportIds;
    }

    @Data
    public static class CreateWordExportJobRequest {
        @NotNull(message = "导出类型不能为空")
        private WordExportJobType type;
        private List<Integer> reportIds;
    }

    @Data
    public static class WordExportJobResponse {
        private String jobId;
        private WordExportJobType type;
        private WordExportJobStatus status;
        private String suggestedFileName;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }
}

