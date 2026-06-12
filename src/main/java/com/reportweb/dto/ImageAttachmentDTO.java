package com.reportweb.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageAttachmentDTO {
    private Integer id;
    private List<String> imageUrls; // 图片URL列表
    private String description; // 附图描述
    private Integer displayOrder; // 显示顺序
    /** 与 imageUrls 下标一一对应；金相 MET 标尺裁剪标记 */
    private List<Boolean> metCroppedFlags;
}
