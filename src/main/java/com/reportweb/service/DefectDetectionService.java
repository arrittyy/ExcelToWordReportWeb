package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.util.NdtDefectRowUtil;
import com.reportweb.util.RtDefectRowUtil;
import com.reportweb.util.PdmPipeCreepRules;
import com.reportweb.util.TableDataMergeUtil;
import com.reportweb.util.UltrasonicThicknessMinRequiredRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * 缺陷检测服务
 * 根据不同的检测类型判断报告是否存在缺陷
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefectDetectionService {

    private final ObjectMapper objectMapper;
    private final DataComparisonService dataComparisonService;
    private final MaterialPropertyService materialPropertyService;
    private final AatDataComparisonService aatDataComparisonService;
    private final DetectionContentRowComponentResolver rowComponentResolver;

    /** 合并 perContentRow 各块后的 rows 数组（兼容仅顶层 rows 的旧数据） */
    private JsonNode mergedRowsFromFirstItemTableData(Report report) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return objectMapper.createArrayNode();
        }
        String td = report.getReportItems().get(0).getTableData();
        if (td == null || td.trim().isEmpty()) {
            return objectMapper.createArrayNode();
        }
        return TableDataMergeUtil.mergedRowsFromTableDataJson(td, objectMapper);
    }

    private List<JsonNode> uttTableDataBlocks(Report report) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return Collections.emptyList();
        }
        String td = report.getReportItems().get(0).getTableData();
        if (td == null || td.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return TableDataMergeUtil.perContentRowBlocks(td, objectMapper);
    }

    private int uttContentAndDataBlockCount(Report report) {
        List<JsonNode> blocks = uttTableDataBlocks(report);
        int contentRows = 0;
        try {
            JsonNode dc = objectMapper.valueToTree(report.getDetectionContent());
            if (dc != null && dc.isObject() && dc.has("rows") && dc.get("rows").isArray()) {
                contentRows = dc.get("rows").size();
            }
        } catch (Exception ignored) {
        }
        return Math.max(Math.max(blocks.size(), contentRows), 1);
    }

    private JsonNode uttMeasurementRowsForBlock(Report report, int blockIndex, List<JsonNode> blocks) {
        if (blocks != null && !blocks.isEmpty()) {
            if (blockIndex >= 0 && blockIndex < blocks.size()) {
                JsonNode block = blocks.get(blockIndex);
                if (block != null && block.has("rows") && block.get("rows").isArray()) {
                    return block.get("rows");
                }
            }
            return objectMapper.createArrayNode();
        }
        return mergedRowsFromFirstItemTableData(report);
    }

    /** 供理化对比：仅含合并后 rows 的 tableData JSON */
    private String mergedTableDataJsonForCompare(Report report) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return null;
        }
        String td = report.getReportItems().get(0).getTableData();
        if (td == null || td.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(TableDataMergeUtil.tableDataWithMergedRowsOnly(td, objectMapper));
        } catch (Exception e) {
            return td;
        }
    }

    /**
     * 判断报告是否存在缺陷
     * @param report 报告对象
     * @param experimentType 检测类型
     * @param component 部件对象（可选）
     * @return "是"表示存在缺陷，"否"表示不存在缺陷，null表示无法判断
     */
    public String hasDefect(Report report, ExperimentType experimentType, ProjectComponent component) {
        if (report == null || experimentType == null) {
            return null;
        }

        String code = experimentType.getCode();
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        code = code.trim().toUpperCase();

        // 磁粉、渗透、超声检测（MT、PT、LP、UT）：按 perContentRow 分块判定有效缺陷行
        if ("MT".equals(code) || "PT".equals(code) || "LP".equals(code) || "UT".equals(code)) {
            boolean hasDefect = hasDefectForNdt(report, component, code);
            return hasDefect ? "是" : "否";
        }

        // 射线检测（RT）：仅当「缺陷位置、性质及数量」列有有效内容时判为有缺陷
        if ("RT".equals(code)) {
            boolean hasDefect = hasDefectForRadiographic(report, component);
            return hasDefect ? "是" : "否";
        }

        // 内窥镜检测 (VT)：由用户在 hasDefect 字段选择，不因检测数据行或 detectionContent.result 自动判定
        if ("VT".equals(code)) {
            return null;
        }

        // 目视检测 (VIS)：由用户在 hasDefect 字段选择，与 detectionContent 中 resultDesc 无关
        if ("VIS".equals(code)) {
            return null;
        }

        // 涡流检测 (ET)
        if ("ET".equals(code)) {
            boolean hasDefect = hasDefectForET(report);
            return hasDefect ? "是" : "否";
        }

        // 相控阵超声波检测 (PAUT)
        if ("PAUT".equals(code)) {
            boolean hasDefect = hasDefectForNdt(report, component, code);
            return hasDefect ? "是" : "否";
        }

        // 圆度测量 (RDM)
        if ("RDM".equals(code)) {
            boolean hasDefect = hasDefectForRoundness(report);
            return hasDefect ? "是" : "否";
        }

        // 超声波测厚 (UTM，兼容 UTT)
        if ("UTM".equals(code) || "UTT".equals(code)) {
            boolean hasDefect = hasDefectForUltrasonicThickness(report, component);
            return hasDefect ? "是" : "否";
        }

        // 管径测量 (PDM)
        if ("PDM".equals(code)) {
            boolean hasDefect = hasDefectForPDM(report, component);
            if (hasDefect) {
                return "是";
            } else {
                // 如果能够判断（有数据），返回"否"；否则返回null让用户手动选择
                return hasPDMData(report) ? "否" : null;
            }
        }

        // 氧化皮堆积检测 (SOD) - 用户手动选择
        if ("SOD".equals(code)) {
            return null;
        }

        // 金相检测 (MET) - 用户手动选择
        if ("MET".equals(code)) {
            return null;
        }

        // 化学成分等理化检测（使用DataComparisonService，包括合金分析、里氏硬度等）
        if (hasFieldMapping(code)) {
            boolean hasDefect = hasDefectForChemical(report, code, component);
            return hasDefect ? "是" : "否";
        }

        // 其他未实现的检测类型，使用Report.hasDefect字段
        return hasDefectForOther(report);
    }

    /**
     * 多部件：对每个部件分别调用 {@link #hasDefect(Report, ExperimentType, ProjectComponent)}，
     * 任一判定为「是」则返回「是」；若存在无法判断（null）且从未出现「是」，则返回 null；否则返回「否」。
     */
    public String hasDefectForComponents(Report report, ExperimentType experimentType,
                                         List<ProjectComponent> components) {
        if (report == null || experimentType == null) {
            return null;
        }
        if (experimentType.getCode() != null
                && ("PMI".equalsIgnoreCase(experimentType.getCode().trim())
                || "AAT".equalsIgnoreCase(experimentType.getCode().trim()))) {
            List<DataComparisonService.NonComplianceRecord> records =
                    aatDataComparisonService.computeNonComplianceRecords(report);
            return records.isEmpty() ? "否" : "是";
        }
        if (components == null || components.isEmpty()) {
            return hasDefect(report, experimentType, (ProjectComponent) null);
        }
        String code = experimentType.getCode();
        if (code != null && isPerContentRowBlockBasedType(code.trim().toUpperCase(Locale.ROOT))
                && ndtContentAndDataBlockCount(report) > 1) {
            return hasDefect(report, experimentType, (ProjectComponent) null);
        }
        boolean anyUnknown = false;
        for (ProjectComponent c : components) {
            String r = hasDefect(report, experimentType, c);
            if ("是".equals(r)) {
                return "是";
            }
            if (r == null) {
                anyUnknown = true;
            }
        }
        if (anyUnknown) {
            return null;
        }
        return "否";
    }

    /**
     * 射线（RT）指定 perContentRow 分块是否存在「缺陷位置、性质及数量」有效行。
     */
    public boolean hasRadiographicDefectInBlock(Report report, int blockIndex) {
        return hasDefectForRadiographicAtBlock(report, blockIndex);
    }

    /**
     * 指定 perContentRow 分块是否存在有效 NDT 缺陷行（与 Word {@code hasDefectInTableDataBlock} 一致）。
     */
    public boolean hasNdtDefectInTableDataBlock(Report report, int blockIndex) {
        return hasNdtDefectInTableDataBlock(report, blockIndex, false);
    }

    private boolean hasNdtDefectInTableDataBlock(Report report, int blockIndex, boolean supportsRecordOnlyFlag) {
        return NdtDefectRowUtil.blockHasMeaningfulDefectRows(
                ndtRowsForBlock(report, blockIndex),
                supportsRecordOnlyFlag
        );
    }

    private int ndtContentAndDataBlockCount(Report report) {
        List<JsonNode> blocks = ndtTableDataBlocks(report);
        int contentRows = 0;
        try {
            JsonNode dc = objectMapper.valueToTree(report.getDetectionContent());
            if (dc != null && dc.isObject() && dc.has("rows") && dc.get("rows").isArray()) {
                contentRows = dc.get("rows").size();
            }
        } catch (Exception ignored) {
        }
        return Math.max(Math.max(blocks.size(), contentRows), 1);
    }

    private List<JsonNode> ndtTableDataBlocks(Report report) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return Collections.emptyList();
        }
        String td = report.getReportItems().get(0).getTableData();
        if (td == null || td.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return TableDataMergeUtil.perContentRowBlocks(td, objectMapper);
    }

    private JsonNode ndtRowsForBlock(Report report, int blockIndex) {
        List<JsonNode> blocks = ndtTableDataBlocks(report);
        if (!blocks.isEmpty()) {
            if (blockIndex >= 0 && blockIndex < blocks.size()) {
                JsonNode block = blocks.get(blockIndex);
                if (block != null && block.has("rows") && block.get("rows").isArray()) {
                    return block.get("rows");
                }
            }
            return objectMapper.createArrayNode();
        }
        return mergedRowsFromFirstItemTableData(report);
    }

    private boolean isPerContentRowBlockBasedType(String code) {
        return "MT".equals(code) || "PT".equals(code) || "LP".equals(code)
                || "UT".equals(code) || "PAUT".equals(code) || "ET".equals(code) || "RT".equals(code);
    }

    private boolean componentHasDefectInAnyMatchingBlock(Report report, ProjectComponent component,
            int blockCount, IntPredicate blockHasDefect) {
        if (component == null || component.getId() == null) {
            return false;
        }
        for (int i = 0; i < blockCount; i++) {
            Integer rowCompId = rowComponentResolver.resolveComponentId(report, i);
            if (component.getId().equals(rowCompId) && blockHasDefect.test(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * NDT/PAUT/ET：多部件时按检测内容行分块；指定部件时仅判对应块，未指定时任一块有缺陷即为有缺陷。
     */
    private boolean hasDefectForNdt(Report report, ProjectComponent component, String code) {
        boolean supportsRecordOnlyFlag = supportsRecordOnlyFlag(code);
        int blockCount = ndtContentAndDataBlockCount(report);
        if (blockCount <= 1) {
            return hasNdtDefectInTableDataBlock(report, 0, supportsRecordOnlyFlag);
        }
        if (component != null) {
            return componentHasDefectInAnyMatchingBlock(
                    report, component, blockCount,
                    i -> hasNdtDefectInTableDataBlock(report, i, supportsRecordOnlyFlag));
        }
        for (int i = 0; i < blockCount; i++) {
            if (hasNdtDefectInTableDataBlock(report, i, supportsRecordOnlyFlag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 射线（RT）：按分块判定「缺陷位置、性质及数量」列有效内容。
     */
    private boolean hasDefectForRadiographic(Report report, ProjectComponent component) {
        int blockCount = ndtContentAndDataBlockCount(report);
        if (blockCount <= 1) {
            return hasDefectForRadiographicAtBlock(report, 0);
        }
        if (component != null) {
            return componentHasDefectInAnyMatchingBlock(
                    report, component, blockCount,
                    i -> hasDefectForRadiographicAtBlock(report, i));
        }
        for (int i = 0; i < blockCount; i++) {
            if (hasDefectForRadiographicAtBlock(report, i)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDefectForRadiographicAtBlock(Report report, int blockIndex) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return false;
        }
        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return false;
        }
        try {
            JsonNode rows = ndtRowsForBlock(report, blockIndex);
            for (JsonNode row : rows) {
                if (RtDefectRowUtil.hasEffectiveDefectColumn(row, true)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("解析射线 ReportItem tableData 失败，报告ID: {}", report.getId(), e);
            return false;
        }
    }

    /**
     * 圆度测量的缺陷判断
     * 比较"测量圆度值"和"允许圆度值"，如果测量值 > 允许值，则存在缺陷
     */
    private boolean hasDefectForRoundness(Report report) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return false;
        }

        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return false;
        }

        try {
            JsonNode rows = mergedRowsFromFirstItemTableData(report);
            for (JsonNode row : rows) {
                Double measuredValue = null;
                Double allowedValue = null;

                if (row.has("测量圆度值")) {
                    JsonNode r = row.get("测量圆度值");
                    if (r.isNumber()) {
                        measuredValue = r.asDouble();
                    }
                }

                if (row.has("允许圆度值")) {
                    JsonNode r = row.get("允许圆度值");
                    if (r.isNumber()) {
                        allowedValue = r.asDouble();
                    }
                }

                // 如果测量值 > 允许值，则存在缺陷
                if (measuredValue != null && allowedValue != null && measuredValue > allowedValue) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("解析圆度测量数据失败，报告ID: {}", report.getId(), e);
        }

        return false;
    }

    private String uttPdmPointNumberFromRow(JsonNode row) {
        return UltrasonicThicknessMinRequiredRules.parsePointNumber(row);
    }

    /**
     * 超声波测厚 (UTT/UTM) 不符合记录，供 API 高亮；规则与 {@link #hasDefectForUltrasonicThickness} 一致。
     */
    public List<DataComparisonService.NonComplianceRecord> listNonComplianceForUltrasonicThickness(
            Report report, ProjectComponent component) {
        List<DataComparisonService.NonComplianceRecord> out = new ArrayList<>();
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return out;
        }
        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return out;
        }
        try {
            List<JsonNode> blocks = uttTableDataBlocks(report);
            int blockCount = uttContentAndDataBlockCount(report);
            for (int i = 0; i < blockCount; i++) {
                Double minRequired = UltrasonicThicknessMinRequiredRules.parseMinRequiredFromDetectionContent(
                        report.getDetectionContent(), objectMapper, i);
                if (minRequired == null || minRequired <= 0) {
                    continue;
                }
                JsonNode rows = uttMeasurementRowsForBlock(report, i, blocks);
                String standardHint = UltrasonicThicknessMinRequiredRules.formatMinRequiredForDisplay(minRequired);
                for (JsonNode row : rows) {
                    if (row == null || TableDataMergeUtil.isTrailingSlashPlaceholderRow(row)) {
                        continue;
                    }
                    String pt = UltrasonicThicknessMinRequiredRules.parsePointNumber(row);
                    Double measured = UltrasonicThicknessMinRequiredRules.parseMeasuredThickness(row);
                    if (pt.isEmpty() || measured == null || measured <= 0) {
                        continue;
                    }
                    if (!UltrasonicThicknessMinRequiredRules.isBelowMinRequired(measured, minRequired)) {
                        continue;
                    }
                    DataComparisonService.NonComplianceRecord rec = new DataComparisonService.NonComplianceRecord();
                    rec.setNumber(pt);
                    rec.setItemName("实测厚度");
                    rec.setStandardValue(standardHint);
                    rec.setActualValue(UltrasonicThicknessMinRequiredRules.formatMeasuredForDisplay(measured));
                    rec.setResult("壁厚不满足DL438-2016的要求");
                    out.add(rec);
                }
            }
        } catch (Exception e) {
            log.warn("解析超声波测厚数据失败，报告ID: {}", report.getId(), e);
        }
        return out;
    }

    /**
     * 超声波测厚缺陷判断：任一分块内实测厚度低于同下标检测内容行的「最小需要厚度」。
     */
    private boolean hasDefectForUltrasonicThickness(Report report, ProjectComponent component) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return false;
        }
        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return false;
        }
        try {
            List<JsonNode> blocks = uttTableDataBlocks(report);
            int blockCount = uttContentAndDataBlockCount(report);
            for (int i = 0; i < blockCount; i++) {
                Double minRequired = UltrasonicThicknessMinRequiredRules.parseMinRequiredFromDetectionContent(
                        report.getDetectionContent(), objectMapper, i);
                JsonNode rows = uttMeasurementRowsForBlock(report, i, blocks);
                UltrasonicThicknessMinRequiredRules.EvaluationResult eval =
                        UltrasonicThicknessMinRequiredRules.evaluateRows(rows, minRequired);
                if (!eval.failedPointNumbers().isEmpty()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("解析超声波测厚数据失败，报告ID: {}", report.getId(), e);
        }
        return false;
    }

    /**
     * 与 hasDefect 中理化分支一致：对配置了字段映射的检测类型，计算与材质标准的对比不符合记录（供 API 高亮等）。
     * 另含 UTT/UTM（最小需要厚度）、PDM（蠕变应变）等几何类判定。
     */
    public List<DataComparisonService.NonComplianceRecord> listNonComplianceForMaterialComparison(
            Report report, ExperimentType experimentType, ProjectComponent component) {
        if (report == null || experimentType == null) {
            return Collections.emptyList();
        }
        String code = experimentType.getCode();
        if (code == null || code.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String upper = code.trim().toUpperCase();
        if ("UTM".equals(upper) || "UTT".equals(upper)) {
            return listNonComplianceForUltrasonicThickness(report, component);
        }
        if ("PDM".equals(upper)) {
            return listNonComplianceForPdm(report, component);
        }
        if (!hasFieldMapping(upper)) {
            return Collections.emptyList();
        }
        return computeChemicalNonComplianceRecords(report, upper, component);
    }

    /**
     * 化学成分等理化检测的缺陷判断
     * 使用DataComparisonService比较实验值与标准值
     * @param experimentTypeCode 检测类型代码（如 AAT、LHD 等）
     */
    private boolean hasDefectForChemical(Report report, String experimentTypeCode, ProjectComponent component) {
        List<DataComparisonService.NonComplianceRecord> nonComplianceRecords =
                computeChemicalNonComplianceRecords(report, experimentTypeCode, component);
        boolean hasDefect = nonComplianceRecords != null && !nonComplianceRecords.isEmpty();
        if (experimentTypeCode != null) {
            String code = experimentTypeCode.trim().toUpperCase();
            if (hasDefect) {
                log.info("报告 {} 检测类型 {} 存在 {} 条不符合标准记录，将标记为有缺陷",
                        report.getId(), code, nonComplianceRecords.size());
            } else {
                log.debug("报告 {} 检测类型 {} 未发现不符合标准记录", report.getId(), code);
            }
        }
        return hasDefect;
    }

    private List<DataComparisonService.NonComplianceRecord> computeChemicalNonComplianceRecords(
            Report report, String experimentTypeCode, ProjectComponent component) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return Collections.emptyList();
        }

        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> materialProperty = null;
        String materialKey = null;
        if (component != null && component.getMaterial() != null && !component.getMaterial().isEmpty()) {
            materialKey = component.getMaterial();
        } else if (report.getCustomFields() != null) {
            Object m = report.getCustomFields().get("部件材质");
            if (m != null) {
                materialKey = m.toString();
            }
        }
        if (materialKey != null && !materialKey.isEmpty() && !"/".equals(materialKey)) {
            materialProperty = materialPropertyService.getMaterialProperty(materialKey);
        }

        if (materialProperty == null || materialProperty.isEmpty()) {
            log.debug("化学/理化检测缺陷判断时未找到材质标准，reportId={}", report.getId());
            return Collections.emptyList();
        }

        try {
            if (experimentTypeCode == null || experimentTypeCode.trim().isEmpty()) {
                return Collections.emptyList();
            }

            String code = experimentTypeCode.trim().toUpperCase();

            if ("PMI".equals(code) || "AAT".equals(code)) {
                return aatDataComparisonService.computeNonComplianceRecords(report);
            }

            if ("LHT".equals(code) || "LHD".equals(code)) {
                String tableDataJson = firstItem.getTableData();

                if (LeebHardnessModeResolver.isBoltOrNutMode(report, objectMapper)) {
                    String boltRange = LeebHardnessModeResolver.resolveLeebBoltRange(materialProperty);
                    return dataComparisonService.compareLeebBoltAndNutRanges(
                            tableDataJson,
                            report.getDetectionContent(),
                            boltRange,
                            "编号",
                            "平均",
                            "类型"
                    );
                }
                String mergedJson = mergedTableDataJsonForCompare(report);
                if (mergedJson == null) {
                    return Collections.emptyList();
                }
                String pipeRange = materialProperty.getOrDefault("里氏-管件", materialProperty.get("里氏"));
                String weldRange = materialPropertyService.resolveLeebWeldRangeForComparison(materialProperty);
                String steelPipeRange = materialProperty.get("里氏-钢管");
                return dataComparisonService.compareLeebWithPipeAndWeldRanges(
                        mergedJson,
                        pipeRange,
                        weldRange,
                        steelPipeRange,
                        "编号",
                        "平均"
                );
            }

            var fieldMapping = DataComparisonService.getFieldMapping(code);
            if (fieldMapping == null) {
                return Collections.emptyList();
            }

            String mergedJson = mergedTableDataJsonForCompare(report);
            if (mergedJson == null) {
                return Collections.emptyList();
            }
            return dataComparisonService.compareData(mergedJson, materialProperty, fieldMapping);
        } catch (Exception e) {
            log.warn("化学/理化检测数据对比失败，报告ID: {}", report.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 其他未实现检测类型的缺陷判断
     * 使用Report.hasDefect字段
     */
    private String hasDefectForOther(Report report) {
        String hasDefect = report.getHasDefect();
        if (hasDefect == null || hasDefect.trim().isEmpty()) {
            return null;
        }
        return hasDefect.trim();
    }

    /**
     * 检查检测类型是否有字段映射配置
     */
    private boolean hasFieldMapping(String code) {
        var fieldMapping = DataComparisonService.getFieldMapping(code);
        return fieldMapping != null;
    }

    /**
     * 涡流检测 (ET) 的缺陷判断
     * 如果检测数据中有数据条，则该条报告存在缺陷
     */
    private boolean hasDefectForET(Report report) {
        return hasDefectForNdt(report, null, "ET");
    }

    private boolean supportsRecordOnlyFlag(String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return "MT".equals(normalized)
                || "PT".equals(normalized)
                || "LP".equals(normalized)
                || "UT".equals(normalized)
                || "RT".equals(normalized);
    }

    private Double parseMeasuredDiameterFromRow(JsonNode row) {
        if (row == null) {
            return null;
        }
        String[] keys = {"实测管径（mm）", "实测管径 (mm)", "实测管径"};
        for (String key : keys) {
            if (!row.has(key)) {
                continue;
            }
            JsonNode d = row.get(key);
            if (d == null || d.isNull()) {
                continue;
            }
            if (d.isNumber()) {
                return d.asDouble();
            }
            try {
                return Double.parseDouble(d.asText().trim());
            } catch (NumberFormatException ignored) {
                // try next key
            }
        }
        return null;
    }

    private boolean isPdmRowNonCompliant(double measuredDiameter, double componentDiameter, double thresholdRatio) {
        if (measuredDiameter <= 0 || componentDiameter <= 0) {
            return false;
        }
        double strain = (measuredDiameter - componentDiameter) / componentDiameter;
        return strain > thresholdRatio;
    }

    /**
     * 管径测量 (PDM) 不符合记录，供 API 高亮；规则与 {@link #hasDefectForPDM} 一致。
     */
    public List<DataComparisonService.NonComplianceRecord> listNonComplianceForPdm(
            Report report, ProjectComponent component) {
        List<DataComparisonService.NonComplianceRecord> out = new ArrayList<>();
        if (component == null || component.getPipeDiameter() == null || component.getPipeDiameter().isEmpty()) {
            return out;
        }
        Double componentDiameter = parseDiameter(component.getPipeDiameter());
        if (componentDiameter == null || componentDiameter <= 0) {
            return out;
        }
        String pipeType = parsePdmPipeTypeFromReport(report);
        Double thresholdRatio = PdmPipeCreepRules.ratioForPipeType(pipeType);
        if (thresholdRatio == null) {
            return out;
        }
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return out;
        }
        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return out;
        }
        String typeLabel = pipeType != null ? pipeType : "未知类型";
        String standardHint = String.format(Locale.ROOT,
                "名义外径 %.3f mm（%s）；蠕变应变>%.2f%% 视为不符合",
                componentDiameter, typeLabel, thresholdRatio * 100.0);
        try {
            JsonNode rows = mergedRowsFromFirstItemTableData(report);
            for (JsonNode row : rows) {
                Double measuredDiameter = parseMeasuredDiameterFromRow(row);
                if (measuredDiameter == null) {
                    continue;
                }
                if (!isPdmRowNonCompliant(measuredDiameter, componentDiameter, thresholdRatio)) {
                    continue;
                }
                double strain = (measuredDiameter - componentDiameter) / componentDiameter;
                DataComparisonService.NonComplianceRecord rec = new DataComparisonService.NonComplianceRecord();
                String pt = uttPdmPointNumberFromRow(row);
                rec.setNumber(pt.isEmpty() ? "/" : pt);
                rec.setItemName("实测管径");
                rec.setStandardValue(standardHint);
                rec.setActualValue(String.format(Locale.ROOT, "%.3f mm", measuredDiameter));
                rec.setResult(String.format(Locale.ROOT,
                        "不符合：蠕变应变 %.2f%% 超过允许 %.2f%%",
                        strain * 100.0, thresholdRatio * 100.0));
                out.add(rec);
            }
        } catch (Exception e) {
            log.warn("解析管径测量数据失败，报告ID: {}", report.getId(), e);
        }
        return out;
    }

    /**
     * 管径测量 (PDM) 的缺陷判断：按检测内容「类型」与名义外径计算蠕变应变，大于允许比例则缺陷。
     */
    private boolean hasDefectForPDM(Report report, ProjectComponent component) {
        if (component == null || component.getPipeDiameter() == null || component.getPipeDiameter().isEmpty()) {
            return false;
        }

        Double componentDiameter = parseDiameter(component.getPipeDiameter());
        if (componentDiameter == null || componentDiameter <= 0) {
            return false;
        }

        String pipeType = parsePdmPipeTypeFromReport(report);
        Double thresholdRatio = PdmPipeCreepRules.ratioForPipeType(pipeType);
        if (thresholdRatio == null) {
            return false;
        }

        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return false;
        }

        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return false;
        }

        try {
            JsonNode rows = mergedRowsFromFirstItemTableData(report);
            for (JsonNode row : rows) {
                Double measuredDiameter = parseMeasuredDiameterFromRow(row);
                if (measuredDiameter != null && isPdmRowNonCompliant(measuredDiameter, componentDiameter, thresholdRatio)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("解析管径测量数据失败，报告ID: {}", report.getId(), e);
        }

        return false;
    }

    /** 检测内容 rows[0].type，与前端「管径测量」类型下拉一致 */
    private String parsePdmPipeTypeFromReport(Report report) {
        if (report.getDetectionContent() == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(report.getDetectionContent()));
            if (node.has("rows") && node.get("rows").isArray() && node.get("rows").size() > 0) {
                JsonNode first = node.get("rows").get(0);
                if (first.has("type")) {
                    String t = first.get("type").asText();
                    if (t != null && !t.trim().isEmpty()) {
                        return t.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析管径测量检测内容类型失败，报告ID: {}", report.getId(), e);
        }
        return null;
    }

    /**
     * 检查PDM是否有数据
     */
    private boolean hasPDMData(Report report) {
        if (report.getReportItems() == null || report.getReportItems().isEmpty()) {
            return false;
        }

        var firstItem = report.getReportItems().get(0);
        if (firstItem.getTableData() == null || firstItem.getTableData().trim().isEmpty()) {
            return false;
        }

        try {
            JsonNode rows = mergedRowsFromFirstItemTableData(report);
            return rows.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析管径字符串，提取数值
     * 例如："100" -> 100.0, "100mm" -> 100.0, "Φ100" -> 100.0
     */
    private Double parseDiameter(String diameterStr) {
        if (diameterStr == null || diameterStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 移除所有非数字和小数点的字符
            String cleaned = diameterStr.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse diameter: {}", diameterStr);
            return null;
        }
    }

    /**
     * 解析壁厚字符串，提取数值
     * 例如："12" -> 12.0, "12mm" -> 12.0, "Φ10×12" -> 12.0
     */
    private Double parseWallThickness(String wallThicknessStr) {
        if (wallThicknessStr == null || wallThicknessStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 移除所有非数字和小数点的字符
            String cleaned = wallThicknessStr.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse wall thickness: {}", wallThicknessStr);
            return null;
        }
    }

}

