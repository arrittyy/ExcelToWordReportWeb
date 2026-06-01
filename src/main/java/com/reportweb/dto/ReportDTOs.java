package com.reportweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportDTOs {

    @Data
    public static class ReportList {
        private Integer id;
        private Integer projectId;
        private String projectNumber;
        private String projectName;
        private String title;
        private String reportNumber;
        private String testMethod;
        private LocalDate testDate;
        private String location;
        private String status;
        private String inspector;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String userFullName;
        private Integer itemCount;
        
        // 特殊字段（JSON）
        private Map<String, Object> customFields;
        private Map<String, Object> detectionContent;
        /** 与单项 Word「检测内容」一致的整段叙述；无则前端可回退 formatDetectionContent */
        private String detectionContentNarrative;
        
        // 兼容性字段（从 customFields 中提取，用于向后兼容）
        private String equipmentCategory;
        private String equipmentName;
        private String componentSpec;
        private String instrumentModel;
        private String instrumentNumber;  // 仪器编号（新增）
        private Integer projectComponentId;  // 关联检测部件ID
        /** 多选部件 ID 顺序；单选时通常为空，由 projectComponentId 表示 */
        private List<Integer> projectComponentIds;
        private Integer projectInstrumentId;  // 关联仪器设备ID
        /** 报告级检测类型（与 reports.experiment_type_id 一致） */
        private Integer experimentTypeId;
        private String experimentTypeName;
        private String reportImage;  // 报告附图URL
        private String hasDefect;    // 是否存在缺陷
        private List<ReportItemDTO> reportItems;  // 报告项（包含检测数据）
        private List<ImageAttachmentDTO> imageAttachments;  // 附图列表
    }

    @Data
    public static class ReportDetail {
        private Integer id;
        private Integer projectId;
        private String projectNumber;
        private String projectName;
        private String title;
        private String reportNumber;
        private String inspector;
        private String testMethod;
        
        // 特殊字段（JSON）
        private Map<String, Object> customFields;
        private Map<String, Object> detectionContent;
        
        // 兼容性字段（从 customFields 中提取，用于向后兼容）
        private String equipment;
        private String testStandard;
        private String componentName;
        private String equipmentCategory;
        private String equipmentName;
        private String componentSpec;
        private String instrumentModel;
        private String instrumentNumber;  // 仪器编号（新增）
        private Integer projectComponentId;  // 关联检测部件ID
        private List<Integer> projectComponentIds;
        private Integer projectInstrumentId;  // 关联仪器设备ID
        private LocalDate testDate;
        private String location;
        private String status;
        private String reportImage;  // 报告附图URL
        private String hasDefect;    // 是否存在缺陷
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ReportItemDTO> reportItems;
        private List<ImageAttachmentDTO> imageAttachments;  // 附图列表
    }

    @Data
    public static class CreateReport {
        @NotNull(message = "项目ID不能为空")
        private Integer projectId;

        @NotNull(message = "检测类型ID不能为空")
        private Integer experimentTypeId;

        private Integer projectComponentId;  // 关联检测部件（可选）

        /** 多选时优先；与 projectComponentId 同时传时以前端约定为准（后端以非空列表为准） */
        private List<Integer> projectComponentIds;

        private Integer projectInstrumentId;  // 关联仪器设备（可选）

        @NotBlank(message = "报告标题不能为空")
        @Size(max = 200, message = "报告标题长度不能超过200个字符")
        private String title;

        @NotBlank(message = "检测人员不能为空")
        @Size(max = 300, message = "检测人员长度不能超过300个字符")
        private String inspector;

        // 特殊字段（JSON）
        private Map<String, Object> customFields;
        private Map<String, Object> detectionContent;

        @Size(max = 200, message = "检测方法长度不能超过200个字符")
        private String testMethod;

        @Size(max = 200, message = "使用仪器长度不能超过200个字符")
        private String equipment;

        @Size(max = 200, message = "检测标准长度不能超过200个字符")
        private String testStandard;

        @Size(max = 200, message = "部件名称长度不能超过200个字符")
        private String componentName;

        @Size(max = 100, message = "设备类别长度不能超过100个字符")
        private String equipmentCategory;

        @Size(max = 200, message = "设备名称长度不能超过200个字符")
        private String equipmentName;

        @Size(max = 500, message = "部件规格长度不能超过500个字符")
        private String componentSpec;

        @Size(max = 200, message = "仪器型号长度不能超过200个字符")
        private String instrumentModel;

        @Size(max = 100, message = "仪器编号长度不能超过100个字符")
        private String instrumentNumber;  // 仪器编号（新增）

        @NotNull(message = "检测日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate testDate;

        @NotBlank(message = "检测地点不能为空")
        @Size(max = 200, message = "检测地点长度不能超过200个字符")
        private String location;

        @Size(max = 500, message = "报告图片URL长度不能超过500个字符")
        private String reportImage;

        @Size(max = 10, message = "是否存在缺陷长度不能超过10个字符")
        private String hasDefect;

        private List<CreateReportItemDTO> reportItems;

        private List<ImageAttachmentDTO> imageAttachments;  // 附图列表

        // Manual getters and setters for critical fields
        public Integer getProjectId() {
            return projectId;
        }

        public void setProjectId(Integer projectId) {
            this.projectId = projectId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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

        public String getComponentName() {
            return componentName;
        }

        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        public String getEquipmentCategory() {
            return equipmentCategory;
        }

        public void setEquipmentCategory(String equipmentCategory) {
            this.equipmentCategory = equipmentCategory;
        }
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class UpdateReport extends CreateReport {
        @NotBlank(message = "状态不能为空")
        @Size(max = 20, message = "状态长度不能超过20个字符")
        private String status;
    }

    @Data
    public static class ReportItemDTO {
        private Integer id;
        private Integer experimentTypeId;
        private String experimentTypeName;
        private String experimentTypeCode;
        private String tableData;
        private String summary;
    }

    @Data
    public static class CreateReportItemDTO {
        @NotNull(message = "检测类型ID不能为空")
        private Integer experimentTypeId;

        @NotBlank(message = "表格数据不能为空")
        private String tableData;

        private String summary;
    }

    @Data
    public static class ReportSummaryDTO {
        private String reportNumber;      // 报告编号
        private String componentName;      // 部件名称
        private String category;          // 部件类别
        private String experimentTypeName; // 检测类型名称
        private String hasDefect;         // 是否存在缺陷（"是"/"否"/null）
    }
}


