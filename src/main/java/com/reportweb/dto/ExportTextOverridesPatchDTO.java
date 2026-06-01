package com.reportweb.dto;

import lombok.Data;

/** PUT /reports/{id}/export-text-overrides 请求体；null 表示不修改该键，空字符串表示清除覆盖 */
@Data
public class ExportTextOverridesPatchDTO {
    private String detectionNarrativeBody;
    private String conclusionParagraph;
    private String overviewWorkContentLine;
    private String overviewDefectLine;
    /** 非 null 时仅更新该 detectionContent 行的单项报告覆盖 */
    private Integer contentRowIndex;
}
