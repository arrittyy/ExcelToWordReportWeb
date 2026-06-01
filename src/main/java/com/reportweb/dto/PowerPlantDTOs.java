package com.reportweb.dto;

import com.reportweb.dto.UnitDTOs.UnitList;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class PowerPlantDTOs {

    @Data
    public static class PowerPlantList {
        private Integer id;
        private String name;
        private String region;
        private String shortName;
        private String province;
        private String city;
        private String address;
        private String phone;
        private String fax;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class PowerPlantResponse {
        private Integer id;
        private String name;
        private String region;
        private String shortName;
        private String province;
        private String city;
        private String address;
        private String phone;
        private String fax;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<UnitList> units;
    }

    @Data
    public static class CreatePowerPlant {
        @NotBlank(message = "电厂名称不能为空")
        @Size(max = 200, message = "电厂名称长度不能超过200个字符")
        private String name;

        @NotBlank(message = "大区不能为空")
        @Size(max = 100, message = "大区长度不能超过100个字符")
        private String region;

        @Size(max = 100, message = "简称长度不能超过100个字符")
        private String shortName;

        @NotBlank(message = "省份不能为空")
        @Size(max = 50, message = "省份长度不能超过50个字符")
        private String province;

        @NotBlank(message = "城市不能为空")
        @Size(max = 50, message = "城市长度不能超过50个字符")
        private String city;

        @NotBlank(message = "地址不能为空")
        @Size(max = 500, message = "地址长度不能超过500个字符")
        private String address;

        @Size(max = 50, message = "电话长度不能超过50个字符")
        private String phone;

        @Size(max = 50, message = "传真长度不能超过50个字符")
        private String fax;

        @Size(max = 1000, message = "备注长度不能超过1000个字符")
        private String remark;
    }

    @Data
    public static class UpdatePowerPlant {
        @NotBlank(message = "电厂名称不能为空")
        @Size(max = 200, message = "电厂名称长度不能超过200个字符")
        private String name;

        @NotBlank(message = "大区不能为空")
        @Size(max = 100, message = "大区长度不能超过100个字符")
        private String region;

        @Size(max = 100, message = "简称长度不能超过100个字符")
        private String shortName;

        @NotBlank(message = "省份不能为空")
        @Size(max = 50, message = "省份长度不能超过50个字符")
        private String province;

        @NotBlank(message = "城市不能为空")
        @Size(max = 50, message = "城市长度不能超过50个字符")
        private String city;

        @NotBlank(message = "地址不能为空")
        @Size(max = 500, message = "地址长度不能超过500个字符")
        private String address;

        @Size(max = 50, message = "电话长度不能超过50个字符")
        private String phone;

        @Size(max = 50, message = "传真长度不能超过50个字符")
        private String fax;

        @Size(max = 1000, message = "备注长度不能超过1000个字符")
        private String remark;
    }
}
