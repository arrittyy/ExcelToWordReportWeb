package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 检测内容行与部件对齐：行内 projectComponentId 优先，否则 projectComponentIds[rowIndex]。
 * 与前端 detectionContentRowComponent.ts 规则一致。
 */
@Component
public class DetectionContentRowComponentResolver {

    private final ReportComponentMergeHelper reportComponentMergeHelper;
    private final ObjectMapper objectMapper;

    public DetectionContentRowComponentResolver(
            ReportComponentMergeHelper reportComponentMergeHelper,
            ObjectMapper objectMapper) {
        this.reportComponentMergeHelper = reportComponentMergeHelper;
        this.objectMapper = objectMapper;
    }

    public JsonNode detectionContentRowAt(Report report, int rowIndex) {
        Map<String, Object> dc = report.getDetectionContent();
        if (dc == null || dc.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        JsonNode root = objectMapper.valueToTree(dc);
        if (!root.has("rows") || !root.get("rows").isArray()) {
            return objectMapper.createObjectNode();
        }
        JsonNode rows = root.get("rows");
        if (rowIndex >= 0 && rowIndex < rows.size()) {
            return rows.get(rowIndex);
        }
        return objectMapper.createObjectNode();
    }

    /**
     * 解析该行应对应的部件 ID；无则 null。
     */
    public Integer resolveComponentId(Report report, int rowIndex) {
        return resolveComponentId(report, rowIndex, detectionContentRowAt(report, rowIndex));
    }

    public Integer resolveComponentId(Report report, int rowIndex, JsonNode contentRow) {
        List<Integer> selected = reportComponentMergeHelper.resolveComponentIds(report);
        if (contentRow != null && contentRow.has("projectComponentId") && !contentRow.get("projectComponentId").isNull()) {
            int rowId = contentRow.get("projectComponentId").asInt();
            if (selected.contains(rowId)) {
                return rowId;
            }
        }
        if (rowIndex >= 0 && rowIndex < selected.size()) {
            return selected.get(rowIndex);
        }
        return null;
    }

    /**
     * 从已加载的 orderedComps 中取本行部件；无匹配时与历史逻辑一致回退首个部件。
     */
    public ProjectComponent resolve(Report report, List<ProjectComponent> orderedComps, int rowIndex) {
        return resolve(report, orderedComps, rowIndex, detectionContentRowAt(report, rowIndex));
    }

    public ProjectComponent resolve(Report report, List<ProjectComponent> orderedComps, int rowIndex, JsonNode contentRow) {
        if (orderedComps == null || orderedComps.isEmpty()) {
            return null;
        }
        Integer id = resolveComponentId(report, rowIndex, contentRow);
        if (id != null) {
            for (ProjectComponent c : orderedComps) {
                if (id.equals(c.getId())) {
                    return c;
                }
            }
        }
        if (rowIndex >= 0 && rowIndex < orderedComps.size()) {
            return orderedComps.get(rowIndex);
        }
        return orderedComps.get(0);
    }
}
