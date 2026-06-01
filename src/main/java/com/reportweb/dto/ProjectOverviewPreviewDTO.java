package com.reportweb.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** GET /projects/{id}/overview-preview 响应：总报告摘要概述只读预览 */
@Data
public class ProjectOverviewPreviewDTO {

    /** 摘要页委托说明段 */
    private String abstractParagraph;
    /** 1 概述正文（项目描述） */
    private String section1Body;
    /** 是否存在第 2 章条目 */
    private boolean showChapter2;
    /** 按类别分组（顺序与 Word 概述一致） */
    private List<OverviewPreviewCategoryDTO> categories = new ArrayList<>();

    @Data
    public static class OverviewPreviewCategoryDTO {
        private String category;
        /** 2.x 中的 x；无第 2 章条目时为 0 */
        private int chapter2CategoryIndex;
        /** 3.x 中的 x；无第 3 章条目时为 0 */
        private int chapter3CategoryIndex;
        /** @deprecated 兼容字段，等于 chapter2CategoryIndex 或 chapter3CategoryIndex */
        private int categoryIndex;
        private List<OverviewPreviewComponentDTO> chapter2Components = new ArrayList<>();
        private List<OverviewPreviewComponentDTO> chapter3Components = new ArrayList<>();
    }

    @Data
    public static class OverviewPreviewComponentDTO {
        private String componentName;
        /** 2.x.y / 3.x.y 中的 y */
        private int componentIndex;
        private List<OverviewPreviewItemDTO> items = new ArrayList<>();
    }

    @Data
    public static class OverviewPreviewItemDTO {
        /** 如 2.1.1.1 */
        private String number;
        private String text;
    }
}
