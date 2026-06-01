package com.reportweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public class UnitComponentDTOs {

    @Data
    public static class UnitComponentList {
        private Integer id;
        private Integer unitId;
        private String componentName;
        private String material;
        private String category;
        private String pipeDiameter;
        private String wallThickness;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class CreateUnitComponent {
        @NotBlank(message = "部件名称不能为空")
        @Size(max = 255, message = "部件名称长度不能超过255个字符")
        private String componentName;

        @Size(max = 100, message = "部件材质长度不能超过100个字符")
        private String material;

        @NotBlank(message = "类别不能为空")
        @Size(max = 100, message = "部件类别长度不能超过100个字符")
        private String category;

        @Size(max = 50, message = "管径长度不能超过50个字符")
        private String pipeDiameter;

        @Size(max = 50, message = "壁厚长度不能超过50个字符")
        private String wallThickness;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark;
    }

    @Data
    public static class UpdateUnitComponent {
        @NotBlank(message = "部件名称不能为空")
        @Size(max = 255, message = "部件名称长度不能超过255个字符")
        private String componentName;

        @Size(max = 100, message = "部件材质长度不能超过100个字符")
        private String material;

        @NotBlank(message = "类别不能为空")
        @Size(max = 100, message = "部件类别长度不能超过100个字符")
        private String category;

        @Size(max = 50, message = "管径长度不能超过50个字符")
        private String pipeDiameter;

        @Size(max = 50, message = "壁厚长度不能超过50个字符")
        private String wallThickness;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark;
    }
}
