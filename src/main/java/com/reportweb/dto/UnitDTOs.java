package com.reportweb.dto;

import com.reportweb.dto.UnitComponentDTOs.UnitComponentList;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class UnitDTOs {

    @Data
    public static class UnitList {
        private Integer id;
        private Integer powerPlantId;
        private String unitName;
        private String unitNumber;
        private String installedCapacity;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class UnitResponse {
        private Integer id;
        private Integer powerPlantId;
        private String unitName;
        private String unitNumber;
        private String installedCapacity;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<UnitComponentList> components;
    }

    @Data
    public static class CreateUnit {
        @Size(max = 50, message = "机组编号长度不能超过50个字符")
        private String unitNumber;

        @Size(max = 100, message = "装机容量长度不能超过100个字符")
        private String installedCapacity;
    }

    @Data
    public static class UpdateUnit {
        @Size(max = 50, message = "机组编号长度不能超过50个字符")
        private String unitNumber;

        @Size(max = 100, message = "装机容量长度不能超过100个字符")
        private String installedCapacity;
    }
}
