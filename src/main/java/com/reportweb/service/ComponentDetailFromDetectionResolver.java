package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 从检测内容行 + 报告部件列表解析部件明细字段（与 Word {@code normalizeComponentDetailRow} 一致）。
 */
@Component
public class ComponentDetailFromDetectionResolver {

    private final ReportComponentMergeHelper reportComponentMergeHelper;
    private final DetectionContentRowComponentResolver detectionContentRowComponentResolver;
    private final ObjectMapper objectMapper;

    public ComponentDetailFromDetectionResolver(
            ReportComponentMergeHelper reportComponentMergeHelper,
            DetectionContentRowComponentResolver detectionContentRowComponentResolver,
            ObjectMapper objectMapper) {
        this.reportComponentMergeHelper = reportComponentMergeHelper;
        this.detectionContentRowComponentResolver = detectionContentRowComponentResolver;
        this.objectMapper = objectMapper;
    }

    public JsonNode detectionContentRowsNode(Report report) {
        Map<String, Object> dc = report.getDetectionContent();
        if (dc == null || dc.isEmpty()) {
            return objectMapper.createArrayNode();
        }
        JsonNode root = objectMapper.valueToTree(dc);
        if (root.has("rows") && root.get("rows").isArray()) {
            return root.get("rows");
        }
        return objectMapper.createArrayNode();
    }

    public JsonNode rowAt(JsonNode rows, int index) {
        if (rows != null && rows.isArray() && index >= 0 && index < rows.size()) {
            return rows.get(index);
        }
        return objectMapper.createObjectNode();
    }

    private ProjectComponent componentForRow(Report report, List<ProjectComponent> comps, int rowIndex, JsonNode row) {
        return detectionContentRowComponentResolver.resolve(report, comps, rowIndex, row);
    }

    public String resolveName(JsonNode row, Report report, List<ProjectComponent> comps, int rowIndex) {
        String name = textField(row, "名称");
        if (isBlankOrSlash(name)) {
            ProjectComponent rowComponent = componentForRow(report, comps, rowIndex, row);
            if (rowComponent != null && rowComponent.getComponentName() != null
                    && !rowComponent.getComponentName().isBlank()) {
                name = rowComponent.getComponentName();
            }
        }
        if (isBlankOrSlash(name) && report.getComponentName() != null && !report.getComponentName().isBlank()) {
            name = report.getComponentName();
        }
        if (isBlankOrSlash(name)) {
            ProjectComponent first = reportComponentMergeHelper.firstOrNull(comps);
            if (first != null && first.getComponentName() != null && !first.getComponentName().isEmpty()) {
                name = first.getComponentName();
            }
        }
        return isBlankOrSlash(name) ? null : name.trim();
    }

    public String resolveMaterial(JsonNode row, Report report, List<ProjectComponent> comps, int rowIndex) {
        String material = textField(row, "材质");
        if (isBlankOrSlash(material)) {
            ProjectComponent rowComponent = componentForRow(report, comps, rowIndex, row);
            if (rowComponent != null && rowComponent.getMaterial() != null
                    && !rowComponent.getMaterial().isBlank()) {
                material = rowComponent.getMaterial();
            } else {
                String merged = reportComponentMergeHelper.mergeMaterials(comps);
                if (merged != null && !merged.isEmpty()) {
                    material = merged;
                } else if (report.getCustomFields() != null) {
                    Object m = report.getCustomFields().get("部件材质");
                    if (m != null) {
                        material = m.toString();
                    }
                }
            }
        }
        return isBlankOrSlash(material) ? null : material.trim();
    }

    public String resolveSpecDisplay(JsonNode row, Report report, List<ProjectComponent> comps, int rowIndex) {
        String spec = textField(row, "规格");
        if (isBlankOrSlash(spec)) {
            ProjectComponent rowComponent = componentForRow(report, comps, rowIndex, row);
            if (rowComponent != null) {
                String one = reportComponentMergeHelper.formatSpecUnified(rowComponent);
                if (one != null && !one.isEmpty()) {
                    spec = one;
                }
            }
        }
        return isBlankOrSlash(spec) ? null : spec.trim();
    }

    public boolean rowHasSyncableIdentity(JsonNode row, Report report, List<ProjectComponent> comps, int rowIndex) {
        return resolveName(row, report, comps, rowIndex) != null
                || resolveMaterial(row, report, comps, rowIndex) != null;
    }

    private static String textField(JsonNode row, String key) {
        if (row == null || !row.has(key) || row.get(key).isNull()) {
            return null;
        }
        String v = row.get(key).asText("").trim();
        return v.isEmpty() ? null : v;
    }

    private static boolean isBlankOrSlash(String s) {
        return s == null || s.isEmpty() || "/".equals(s);
    }
}
