package com.reportweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public class InstrumentDTOs {

    @Data
    public static class InstrumentList {
        private Integer id;
        private String instrumentName;
        private String instrumentModel;
        private String instrumentNumber;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class CreateInstrument {
        @NotBlank(message = "仪器名称不能为空")
        @Size(max = 255, message = "仪器名称长度不能超过255个字符")
        private String instrumentName;

        @Size(max = 255, message = "仪器型号长度不能超过255个字符")
        private String instrumentModel;

        @Size(max = 100, message = "仪器编号长度不能超过100个字符")
        private String instrumentNumber;
    }

    @Data
    public static class UpdateInstrument {
        @NotBlank(message = "仪器名称不能为空")
        @Size(max = 255, message = "仪器名称长度不能超过255个字符")
        private String instrumentName;

        @Size(max = 255, message = "仪器型号长度不能超过255个字符")
        private String instrumentModel;

        @Size(max = 100, message = "仪器编号长度不能超过100个字符")
        private String instrumentNumber;
    }
}
