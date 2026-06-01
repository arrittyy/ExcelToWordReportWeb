package com.reportweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public class ProjectComponentDTOs {

    @Data
    public static class ComponentList {
        private Integer id;
        private Integer projectId;
        private String componentName;
        private String material;
        private String category;
        private String pipeDiameter;
        private String wallThickness;
        /** PHI | M | NONE；null 表示按名称自动 */
        private String specPrefix;
        private String threadPitch;
        /** 与 Word/报告一致的拼接规格（单部件） */
        private String displaySpec;
        private String remark; // 备注
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class CreateComponent {
        // projectId通过URL路径参数传递,不需要在请求体中
        
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

        /** PHI | M | NONE；不传或空表示按部件名称自动 */
        @Size(max = 8, message = "规格前缀长度不能超过8个字符")
        private String specPrefix;

        @Size(max = 50, message = "牙距长度不能超过50个字符")
        private String threadPitch;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark; // 备注
    }

    @Data
    public static class UpdateComponent {
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

        @Size(max = 8, message = "规格前缀长度不能超过8个字符")
        private String specPrefix;

        @Size(max = 50, message = "牙距长度不能超过50个字符")
        private String threadPitch;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark; // 备注
    }
}





