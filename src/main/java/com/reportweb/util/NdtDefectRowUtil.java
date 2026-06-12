package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 无损检测缺陷行判定：与 Word 缺陷表收集、概述分段逻辑一致。
 */
public final class NdtDefectRowUtil {

    public static final String RECORD_ONLY_DEFECT_KEY = "是否为记录缺陷";

    private NdtDefectRowUtil() {
    }

    public static boolean isDefectRowEmpty(JsonNode row) {
        if (row == null) {
            return true;
        }
        String[] keys = {"序号", "起点位置", "终点位置", "长度", "级别", "备注",
                "编号", "缺陷位置", "幅值", "相位", "减薄量",
                "位置", "波幅", "波幅(dB)", "波幅（dB）", "深度", "深度(mm)", "深度（mm）",
                "长度(mm)", "长度（mm）", "高度", "高度(mm)", "高度（mm）"};
        for (String key : keys) {
            if (row.has(key) && !row.get(key).asText("").trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRecordOnlyDefectRow(JsonNode row, boolean supportsRecordOnlyFlag) {
        if (!supportsRecordOnlyFlag || row == null || !row.isObject()) {
            return false;
        }
        JsonNode n = row.get(RECORD_ONLY_DEFECT_KEY);
        if (n == null || n.isNull()) {
            return false;
        }
        String s = n.asText("").trim();
        if (s.isEmpty()) {
            return false;
        }
        return "是".equalsIgnoreCase(s)
                || "true".equalsIgnoreCase(s)
                || "1".equals(s)
                || "yes".equalsIgnoreCase(s)
                || "y".equalsIgnoreCase(s);
    }

    public static boolean isEffectiveDefectRow(JsonNode row, boolean supportsRecordOnlyFlag) {
        if (row == null || !row.isObject() || TableDataMergeUtil.isTrailingSlashPlaceholderRow(row)) {
            return false;
        }
        if (isRecordOnlyDefectRow(row, supportsRecordOnlyFlag)) {
            return false;
        }
        return !isDefectRowEmpty(row);
    }

    /** 块内是否存在有效缺陷行（跳过占位行与全空行）。 */
    public static boolean blockHasMeaningfulDefectRows(JsonNode rows) {
        return blockHasMeaningfulDefectRows(rows, false);
    }

    /** 块内是否存在有效缺陷行（跳过占位行、记录行与全空行）。 */
    public static boolean blockHasMeaningfulDefectRows(JsonNode rows, boolean supportsRecordOnlyFlag) {
        if (rows == null || !rows.isArray()) {
            return false;
        }
        for (JsonNode row : rows) {
            if (isEffectiveDefectRow(row, supportsRecordOnlyFlag)) {
                return true;
            }
        }
        return false;
    }

    /** 块内有效缺陷行数量（与 Word 缺陷表收集一致）。 */
    public static int countEffectiveDefectRows(JsonNode rows, boolean supportsRecordOnlyFlag) {
        if (rows == null || !rows.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode row : rows) {
            if (isEffectiveDefectRow(row, supportsRecordOnlyFlag)) {
                count++;
            }
        }
        return count;
    }
}
