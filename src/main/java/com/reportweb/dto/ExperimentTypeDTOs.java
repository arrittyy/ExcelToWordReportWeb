package com.reportweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class ExperimentTypeDTOs {

    @Data
    public static class ExperimentTypeList {
        private Integer id;
        private String name;
        private String code;
        private String tableSchema;
        private String reportFieldsSchema;
        private Boolean isActive;
    }

    @Data
    public static class ExperimentTypeDetail {
        private Integer id;
        private String name;
        private String code;
        private String tableSchema;
        private String reportFieldsSchema;
        private Boolean isActive;
    }

    @Data
    public static class CreateExperimentType {
        @NotBlank(message = "检测类型名称不能为空")
        @Size(max = 100, message = "检测类型名称长度不能超过100个字符")
        private String name;

        @NotBlank(message = "检测类型代码不能为空")
        @Size(max = 20, message = "检测类型代码长度不能超过20个字符")
        private String code;

        @NotBlank(message = "表格结构不能为空")
        private String tableSchema;

        @NotBlank(message = "报告字段结构不能为空")
        private String reportFieldsSchema;

        private Boolean isActive = true;
    }

    @Data
    public static class UpdateExperimentType extends CreateExperimentType {
        @NotNull(message = "ID不能为空")
        private Integer id;
    }
}


