package com.reportweb.dto;

import lombok.Data;

/** GET /reports/{id}/export-text-preview 响应 */
@Data
public class ExportTextPreviewDTO {
    private String detectionNarrativeBodyDefault;
    private String detectionNarrativeBodySaved;
    private String conclusionParagraphDefault;
    private String conclusionParagraphSaved;
    private String overviewWorkContentLineDefault;
    private String overviewWorkContentLineSaved;
    /** 含自定义正文时，将「详见后附单项报告编号…。」替换为当前库内编号后的导出用文案（与 Word 一致） */
    private String overviewWorkContentLineEffective;
    private String overviewDefectLineDefault;
    private String overviewDefectLineSaved;
    /** 是否当前判定有缺陷（第四条预览展示） */
    private boolean showDefectSection;

    /** 当前预览对应的 detectionContent.rows 下标 */
    private int contentRowIndex;
    /** 检测内容 table 行数（分段数） */
    private int contentRowCount;
    /** 当前行检测类型（如对接焊缝），供前端 Segmented 标签 */
    private String contentRowType;
    /** 检测内容多行分段时，概述亦按段编辑 */
    private boolean overviewMultiSegment;
}
