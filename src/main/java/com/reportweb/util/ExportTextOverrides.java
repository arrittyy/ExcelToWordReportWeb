package com.reportweb.util;

import com.reportweb.entity.Report;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单项报告导出 Word / 摘要用的自定义文字，存于 {@code custom_fields._exportTextOverrides}。
 * UT/PAUT 多行检测内容可按 {@link #BY_CONTENT_ROW} 分段覆盖（含概述）。
 */
public final class ExportTextOverrides {

    public static final String CUSTOM_FIELDS_KEY = "_exportTextOverrides";

    public static final String DETECTION_NARRATIVE_BODY = "detectionNarrativeBody";
    public static final String CONCLUSION_PARAGRAPH = "conclusionParagraph";
    public static final String OVERVIEW_WORK_CONTENT_LINE = "overviewWorkContentLine";
    public static final String OVERVIEW_DEFECT_LINE = "overviewDefectLine";

    /** 按 detectionContent.rows 下标分段的覆盖（键为 "0","1",…） */
    public static final String BY_CONTENT_ROW = "byContentRow";

    private static final String[] SEGMENT_SCOPED_KEYS = {
            DETECTION_NARRATIVE_BODY,
            CONCLUSION_PARAGRAPH,
            OVERVIEW_WORK_CONTENT_LINE,
            OVERVIEW_DEFECT_LINE
    };

    private ExportTextOverrides() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readRaw(Report report) {
        if (report == null || report.getCustomFields() == null) {
            return Map.of();
        }
        Object raw = report.getCustomFields().get(CUSTOM_FIELDS_KEY);
        if (!(raw instanceof Map)) {
            return Map.of();
        }
        return (Map<String, Object>) raw;
    }

    public static String readString(Report report, String fieldKey) {
        return readStringAtContentRow(report, 0, fieldKey);
    }

    /** 非空且非空白时视为用户覆盖 */
    public static boolean hasNonBlank(Report report, String fieldKey) {
        return !readString(report, fieldKey).isEmpty();
    }

    public static boolean hasNonBlankAtContentRow(Report report, int contentRowIndex, String fieldKey) {
        return !readStringAtContentRow(report, contentRowIndex, fieldKey).isEmpty();
    }

    public static String detectionNarrativeBody(Report report) {
        return detectionNarrativeBody(report, 0);
    }

    public static String conclusionParagraph(Report report) {
        return conclusionParagraph(report, 0);
    }

    public static String detectionNarrativeBody(Report report, int contentRowIndex) {
        return readStringAtContentRow(report, contentRowIndex, DETECTION_NARRATIVE_BODY);
    }

    public static String conclusionParagraph(Report report, int contentRowIndex) {
        return readStringAtContentRow(report, contentRowIndex, CONCLUSION_PARAGRAPH);
    }

    public static String overviewWorkContentLine(Report report) {
        return overviewWorkContentLine(report, 0);
    }

    public static String overviewDefectLine(Report report) {
        return overviewDefectLine(report, 0);
    }

    public static String overviewWorkContentLine(Report report, int contentRowIndex) {
        return overviewWorkContentLine(report, contentRowIndex, false);
    }

    public static String overviewDefectLine(Report report, int contentRowIndex) {
        return overviewDefectLine(report, contentRowIndex, false);
    }

    /**
     * @param multiSegmentReport {@code true} 时第 0 段不读顶层遗留概述（避免多部件合并叙述误作首段）
     */
    public static String overviewWorkContentLine(Report report, int contentRowIndex, boolean multiSegmentReport) {
        return readStringAtContentRow(report, contentRowIndex, OVERVIEW_WORK_CONTENT_LINE, !multiSegmentReport);
    }

    /**
     * 第 0 段始终可读顶层遗留（与 {@link #overviewWorkContentLine} 不同，避免多段时丢失已保存的缺陷概述句）。
     * {@code multiSegmentReport} 保留以兼容调用方，读取逻辑不再依赖该参数。
     */
    public static String overviewDefectLine(Report report, int contentRowIndex, boolean multiSegmentReport) {
        return readStringAtContentRow(report, contentRowIndex, OVERVIEW_DEFECT_LINE, true);
    }

    /**
     * 按段读取：{@code byContentRow[i]} →（仅 i==0 且允许时）顶层遗留字段 → 空。
     */
    public static String readStringAtContentRow(Report report, int contentRowIndex, String fieldKey) {
        return readStringAtContentRow(report, contentRowIndex, fieldKey, true);
    }

    public static String readStringAtContentRow(Report report, int contentRowIndex, String fieldKey,
            boolean allowLegacyTopLevelForRowZero) {
        Map<String, Object> root = readRaw(report);
        String fromRow = readStringFromRowMap(readByContentRowMap(root).get(rowKey(contentRowIndex)), fieldKey);
        if (!fromRow.isEmpty()) {
            return fromRow;
        }
        if (contentRowIndex == 0 && allowLegacyTopLevelForRowZero) {
            Object v = root.get(fieldKey);
            return v != null ? v.toString().trim() : "";
        }
        return "";
    }

    /** 拷贝 custom_fields 并移除导出覆盖键，用于计算「自动生成」预览默认值（勿持久化该副本）。 */
    public static Map<String, Object> copyCustomFieldsWithoutExportOverrides(Map<String, Object> existingCustomFields) {
        Map<String, Object> base = existingCustomFields != null
                ? new LinkedHashMap<>(existingCustomFields)
                : new LinkedHashMap<>();
        base.remove(CUSTOM_FIELDS_KEY);
        return base;
    }

    /**
     * 用请求体整图替换 custom_fields 时，若新图中未带 {@link #CUSTOM_FIELDS_KEY}，则保留库中已有导出文字覆盖，避免被前端空对象冲掉。
     */
    public static Map<String, Object> mergeReplacePreservingExportOverrides(Map<String, Object> previousCustomFields,
                                                                            Map<String, Object> incomingCustomFields) {
        if (incomingCustomFields == null) {
            return previousCustomFields;
        }
        Map<String, Object> out = new LinkedHashMap<>(incomingCustomFields);
        if (!out.containsKey(CUSTOM_FIELDS_KEY) && previousCustomFields != null) {
            Object prevOverrides = previousCustomFields.get(CUSTOM_FIELDS_KEY);
            if (prevOverrides != null) {
                out.put(CUSTOM_FIELDS_KEY, prevOverrides);
            }
        }
        return out;
    }

    /**
     * 合并顶层覆盖字段；{@code contentRowIndex == null} 时写入顶层（兼容旧客户端）。
     */
    public static Map<String, Object> mergeIntoCustomFields(Map<String, Object> existingCustomFields,
                                                           Map<String, String> patch) {
        return mergeIntoCustomFields(existingCustomFields, patch, null);
    }

    /**
     * @param contentRowIndex 非 null 时更新 {@link #BY_CONTENT_ROW} 中该段全部可覆盖字段
     */
    public static Map<String, Object> mergeIntoCustomFields(Map<String, Object> existingCustomFields,
                                                           Map<String, String> patch,
                                                           Integer contentRowIndex) {
        Map<String, Object> base = existingCustomFields != null
                ? new LinkedHashMap<>(existingCustomFields)
                : new LinkedHashMap<>();

        Map<String, Object> overrides = new LinkedHashMap<>(readRawFromMap(base));
        if (patch == null || patch.isEmpty()) {
            finalizeOverrides(base, overrides);
            return base;
        }

        if (contentRowIndex != null) {
            mergePerRowPatch(overrides, patch, contentRowIndex);
        } else {
            mergeTopLevelPatch(overrides, patch);
        }
        finalizeOverrides(base, overrides);
        return base;
    }

    private static void mergeTopLevelPatch(Map<String, Object> overrides, Map<String, String> patch) {
        for (Map.Entry<String, String> e : patch.entrySet()) {
            if (e.getKey() == null || !isSegmentScopedKey(e.getKey())) {
                continue;
            }
            applyKey(overrides, e.getKey(), e.getValue());
        }
    }

    private static void mergePerRowPatch(Map<String, Object> overrides, Map<String, String> patch, int contentRowIndex) {
        migrateLegacyTopLevelSegmentKeysToByContentRow(overrides);

        Map<String, Map<String, Object>> byRow = readByContentRowMap(overrides);
        String rk = rowKey(contentRowIndex);
        Map<String, Object> rowMap = new LinkedHashMap<>(byRow.getOrDefault(rk, Map.of()));

        for (Map.Entry<String, String> e : patch.entrySet()) {
            if (e.getKey() == null || !isSegmentScopedKey(e.getKey())) {
                continue;
            }
            applyKey(rowMap, e.getKey(), e.getValue());
        }

        if (rowMap.isEmpty()) {
            byRow.remove(rk);
        } else {
            byRow.put(rk, rowMap);
        }
        writeByContentRowMap(overrides, byRow);

        // 顶层遗留同名字段在按段写入后移除，避免与 byContentRow 双源
        for (String k : SEGMENT_SCOPED_KEYS) {
            overrides.remove(k);
        }
    }

    /** 顶层分段字段迁入 byContentRow["0"] */
    private static void migrateLegacyTopLevelSegmentKeysToByContentRow(Map<String, Object> overrides) {
        Map<String, Object> legacyRow = new LinkedHashMap<>();
        for (String k : SEGMENT_SCOPED_KEYS) {
            Object v = overrides.get(k);
            if (v != null && !v.toString().isBlank()) {
                legacyRow.put(k, v.toString().trim());
            }
            overrides.remove(k);
        }
        if (legacyRow.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> byRow = readByContentRowMap(overrides);
        Map<String, Object> row0 = new LinkedHashMap<>(byRow.getOrDefault("0", Map.of()));
        for (Map.Entry<String, Object> e : legacyRow.entrySet()) {
            if (!row0.containsKey(e.getKey())) {
                row0.put(e.getKey(), e.getValue());
            }
        }
        byRow.put("0", row0);
        writeByContentRowMap(overrides, byRow);
    }

    private static void writeByContentRowMap(Map<String, Object> overrides, Map<String, Map<String, Object>> byRow) {
        if (byRow.isEmpty()) {
            overrides.remove(BY_CONTENT_ROW);
            return;
        }
        Map<String, Object> stored = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> br : byRow.entrySet()) {
            stored.put(br.getKey(), new LinkedHashMap<>(br.getValue()));
        }
        overrides.put(BY_CONTENT_ROW, stored);
    }

    private static void applyKey(Map<String, Object> target, String key, String val) {
        if (val == null || val.isBlank()) {
            target.remove(key);
        } else {
            target.put(key, val);
        }
    }

    private static void finalizeOverrides(Map<String, Object> base, Map<String, Object> overrides) {
        if (overrides.isEmpty()) {
            base.remove(CUSTOM_FIELDS_KEY);
        } else {
            base.put(CUSTOM_FIELDS_KEY, overrides);
        }
    }

    private static boolean isSegmentScopedKey(String key) {
        for (String k : SEGMENT_SCOPED_KEYS) {
            if (k.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String rowKey(int contentRowIndex) {
        return String.valueOf(Math.max(0, contentRowIndex));
    }

    private static String readStringFromRowMap(Map<String, Object> rowMap, String fieldKey) {
        if (rowMap == null) {
            return "";
        }
        Object v = rowMap.get(fieldKey);
        return v != null ? v.toString().trim() : "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> readByContentRowMap(Map<String, Object> overrides) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        if (overrides == null) {
            return out;
        }
        Object raw = overrides.get(BY_CONTENT_ROW);
        if (!(raw instanceof Map)) {
            return out;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
            if (e.getKey() == null || !(e.getValue() instanceof Map)) {
                continue;
            }
            out.put(e.getKey().toString(), new LinkedHashMap<>((Map<String, Object>) e.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readRawFromMap(Map<String, Object> customFields) {
        if (customFields == null) {
            return new HashMap<>();
        }
        Object raw = customFields.get(CUSTOM_FIELDS_KEY);
        if (!(raw instanceof Map)) {
            return new HashMap<>();
        }
        return new LinkedHashMap<>((Map<String, Object>) raw);
    }
}
