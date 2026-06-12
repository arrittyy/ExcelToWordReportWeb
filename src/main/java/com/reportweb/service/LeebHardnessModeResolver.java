package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.util.TableDataMergeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 里氏硬度螺栓/螺帽模式判定：检测内容「类型」优先，tableData 行内「类型」兜底。
 */
public final class LeebHardnessModeResolver {

    private LeebHardnessModeResolver() {
    }

    public static boolean typeTextIsBolt(String type) {
        return type != null && type.contains("螺栓");
    }

    public static boolean typeTextIsNut(String type) {
        return type != null && type.contains("螺帽");
    }

    public static boolean isBoltOrNutTypeText(String type) {
        return typeTextIsBolt(type) || typeTextIsNut(type);
    }

    /**
     * 管件/对接焊缝里氏类型（与前端 {@code shouldShowLeebModalForType} 一致，兼容「管件」「对接焊缝」历史文案）。
     */
    public static boolean typeTextIsPipeJointWeld(String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }
        String t = type.trim();
        return t.contains("管件") || t.contains("对接焊缝");
    }

    /**
     * 是否为螺栓/螺帽里氏硬度报告（应用「里氏-螺栓」范围校核）。
     */
    public static boolean isBoltOrNutMode(Report report, ObjectMapper objectMapper) {
        if (report == null) {
            return false;
        }
        if (detectionContentHasBoltOrNut(report, objectMapper)) {
            return true;
        }
        return tableDataHasBoltOrNutType(report, objectMapper);
    }

    public static boolean detectionContentHasBoltType(Report report, ObjectMapper objectMapper) {
        for (String type : contentRowTypes(report, objectMapper)) {
            if (typeTextIsBolt(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean detectionContentHasNutType(Report report, ObjectMapper objectMapper) {
        for (String type : contentRowTypes(report, objectMapper)) {
            if (typeTextIsNut(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean detectionContentHasBoltOrNut(Report report, ObjectMapper objectMapper) {
        for (String type : contentRowTypes(report, objectMapper)) {
            if (isBoltOrNutTypeText(type)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> contentRowTypes(Report report, ObjectMapper objectMapper) {
        if (report == null || report.getDetectionContent() == null || objectMapper == null) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = objectMapper.valueToTree(report.getDetectionContent());
            if (node == null || !node.isObject() || !node.has("rows") || !node.get("rows").isArray()) {
                return Collections.emptyList();
            }
            List<String> types = new ArrayList<>();
            for (JsonNode row : node.get("rows")) {
                types.add(row.has("type") ? row.get("type").asText("") : "");
            }
            return types;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static boolean tableDataHasBoltOrNutType(Report report, ObjectMapper objectMapper) {
        if (report == null || report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return false;
        }
        ReportItem firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().isEmpty()) {
            return false;
        }
        try {
            JsonNode rows = TableDataMergeUtil.mergedRowsFromTableDataJson(firstItem.getTableData(), objectMapper);
            if (rows == null || !rows.isArray()) {
                return false;
            }
            for (JsonNode row : rows) {
                JsonNode typeNode = row.get("类型");
                String type = typeNode != null ? typeNode.asText() : null;
                if (isBoltOrNutTypeText(type)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * 从螺栓里氏类型文案提取「端部」或「腰部」（用于检测内容叙述 locationDesc）。
     */
    public static String extractBoltLeebEndWaist(String type) {
        if (type == null || type.isEmpty()) {
            return "";
        }
        if (type.contains("端部")) {
            return "端部";
        }
        if (type.contains("腰部")) {
            return "腰部";
        }
        return "";
    }

    /**
     * 材质库螺栓里氏硬度范围；缺失时回退管件/钢管/母材里氏。
     */
    public static String resolveLeebBoltRange(Map<String, String> materialProperty) {
        if (materialProperty == null || materialProperty.isEmpty()) {
            return null;
        }
        String boltRange = materialProperty.get("里氏-螺栓");
        if (boltRange != null && !boltRange.isEmpty()) {
            return boltRange;
        }
        boltRange = materialProperty.get("里氏-管件");
        if (boltRange != null && !boltRange.isEmpty()) {
            return boltRange;
        }
        boltRange = materialProperty.get("里氏-钢管");
        if (boltRange != null && !boltRange.isEmpty()) {
            return boltRange;
        }
        return materialProperty.get("里氏");
    }
}
