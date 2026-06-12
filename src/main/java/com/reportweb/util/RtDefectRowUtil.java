package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 射线（RT）缺陷行判定：仅「缺陷位置、性质及数量」列有效内容计为缺陷行（与 {@link com.reportweb.service.DefectDetectionService} 一致）。
 */
public final class RtDefectRowUtil {

    public static final String RT_DEFECT_COLUMN_KEY = "缺陷位置、性质及数量";

    private RtDefectRowUtil() {
    }

    /** trim 后非空且非 /、／ 视为有效缺陷描述。 */
    public static boolean isMeaningfulDefectNatureQuantity(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return false;
        }
        return !"/".equals(s) && !"／".equals(s);
    }

    public static boolean hasMeaningfulDefectColumn(JsonNode row) {
        if (row == null || !row.isObject()) {
            return false;
        }
        JsonNode n = row.get(RT_DEFECT_COLUMN_KEY);
        if (n == null || n.isNull()) {
            return false;
        }
        return isMeaningfulDefectNatureQuantity(n.asText());
    }

    public static boolean hasEffectiveDefectColumn(JsonNode row, boolean supportsRecordOnlyFlag) {
        if (!hasMeaningfulDefectColumn(row)) {
            return false;
        }
        return !NdtDefectRowUtil.isRecordOnlyDefectRow(row, supportsRecordOnlyFlag);
    }

    /** 块内有效射线缺陷行数量（「缺陷位置、性质及数量」列有效且非记录行）。 */
    public static int countEffectiveDefectRows(JsonNode rows, boolean supportsRecordOnlyFlag) {
        if (rows == null || !rows.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode row : rows) {
            if (hasEffectiveDefectColumn(row, supportsRecordOnlyFlag)) {
                count++;
            }
        }
        return count;
    }
}
