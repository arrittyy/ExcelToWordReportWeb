package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.util.TableDataMergeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 合金分析（PMI，兼容 AAT）：按 perContentRow 分块、按块材质分别与标准比对。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AatDataComparisonService {

    private final ObjectMapper objectMapper;
    private final DataComparisonService dataComparisonService;
    private final MaterialPropertyService materialPropertyService;
    private final ReportComponentMergeHelper reportComponentMergeHelper;
    private final ProjectComponentRepository projectComponentRepository;
    private final ComponentDetailFromDetectionResolver detailResolver;

    public List<DataComparisonService.NonComplianceRecord> computeNonComplianceRecords(Report report) {
        List<DataComparisonService.NonComplianceRecord> all = new ArrayList<>();
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return all;
        }
        ReportItem firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().isBlank()) {
            return all;
        }

        DataComparisonService.FieldMapping mapping = DataComparisonService.getFieldMapping("PMI");
        if (mapping == null) {
            return all;
        }

        List<ProjectComponent> comps = reportComponentMergeHelper.loadOrdered(
                projectComponentRepository, reportComponentMergeHelper.resolveComponentIds(report));

        List<JsonNode> blocks = TableDataMergeUtil.perContentRowBlocks(firstItem.getTableData(), objectMapper);
        JsonNode dcRows = detailResolver.detectionContentRowsNode(report);

        if (blocks.isEmpty()) {
            return compareSingleBlock(
                    firstItem.getTableData(),
                    report,
                    comps,
                    detailResolver.rowAt(dcRows, 0),
                    0,
                    mapping);
        }

        for (int i = 0; i < blocks.size(); i++) {
            JsonNode block = blocks.get(i);
            if (block == null || !block.has("rows") || !block.get("rows").isArray()) {
                continue;
            }
            JsonNode contentRow = detailResolver.rowAt(dcRows, i);
            String material = detailResolver.resolveMaterial(contentRow, report, comps, i);
            if (material == null || material.isEmpty() || "/".equals(material)) {
                log.debug("AAT 块 {} 无有效材质，跳过比对", i);
                continue;
            }
            Map<String, String> materialProperty = materialPropertyService.getMaterialProperty(material);
            if (materialProperty == null || materialProperty.isEmpty()) {
                log.debug("AAT 块 {} 材质 {} 无标准数据", i, material);
                continue;
            }
            ObjectNode blockOnly = objectMapper.createObjectNode();
            blockOnly.set("rows", block.get("rows"));
            try {
                all.addAll(dataComparisonService.compareData(
                        objectMapper.writeValueAsString(blockOnly),
                        materialProperty,
                        mapping));
            } catch (Exception e) {
                log.warn("AAT 块 {} 比对失败: {}", i, e.getMessage());
            }
        }
        return all;
    }

    /** 各块材质（保持顺序、去重） */
    public List<String> distinctMaterialsForReport(Report report, List<ProjectComponent> comps) {
        Set<String> materials = new LinkedHashSet<>();
        JsonNode dcRows = detailResolver.detectionContentRowsNode(report);
        int n = Math.max(dcRows.size(), 1);
        if (report.getReportItems() != null && !report.getReportItems().isEmpty()) {
            ReportItem item = report.getReportItems().get(0);
            if (item.getTableData() != null) {
                int blocks = TableDataMergeUtil.perContentRowBlocks(item.getTableData(), objectMapper).size();
                if (blocks > 0) {
                    n = Math.max(n, blocks);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            String m = detailResolver.resolveMaterial(detailResolver.rowAt(dcRows, i), report, comps, i);
            if (m != null && !m.isEmpty() && !"/".equals(m)) {
                materials.add(m);
            }
        }
        if (materials.isEmpty() && comps != null) {
            for (ProjectComponent c : comps) {
                if (c.getMaterial() != null && !c.getMaterial().isBlank()) {
                    materials.add(c.getMaterial().trim());
                }
            }
        }
        return new ArrayList<>(materials);
    }

    private List<DataComparisonService.NonComplianceRecord> compareSingleBlock(
            String tableDataJson,
            Report report,
            List<ProjectComponent> comps,
            JsonNode contentRow,
            int rowIndex,
            DataComparisonService.FieldMapping mapping) {
        String material = detailResolver.resolveMaterial(contentRow, report, comps, rowIndex);
        if (material == null || material.isEmpty()) {
            return List.of();
        }
        Map<String, String> materialProperty = materialPropertyService.getMaterialProperty(material);
        if (materialProperty == null || materialProperty.isEmpty()) {
            return List.of();
        }
        try {
            ObjectNode merged = TableDataMergeUtil.tableDataWithMergedRowsOnly(tableDataJson, objectMapper);
            return dataComparisonService.compareData(
                    objectMapper.writeValueAsString(merged),
                    materialProperty,
                    mapping);
        } catch (Exception e) {
            log.warn("AAT 单块比对失败: {}", e.getMessage());
            return List.of();
        }
    }
}
