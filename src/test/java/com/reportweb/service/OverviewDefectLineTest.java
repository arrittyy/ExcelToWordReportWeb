package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Project;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.util.ExportTextOverrides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 总报告第 2 章「发现问题及处理情况」缺陷行：自动拼接须含通知单结论，且可升级历史简化覆盖句。
 */
class OverviewDefectLineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WordGeneratorServiceImpl service;

    @BeforeEach
    void setUp() {
        ReportComponentMergeHelper mergeHelper = new ReportComponentMergeHelper();
        DetectionContentRowComponentResolver rowResolver =
                new DetectionContentRowComponentResolver(mergeHelper, MAPPER);
        DefectDetectionService defectDetectionService =
                new DefectDetectionService(MAPPER, null, null, null, rowResolver);
        DetectionContentNarrativeService narrativeService = new DetectionContentNarrativeService(MAPPER);
        service = new WordGeneratorServiceImpl(
                MAPPER, null, null, null, null, defectDetectionService, null, null,
                mergeHelper, rowResolver, null, narrativeService, null);
    }

    private static Report utReportWithDefectRow() {
        ExperimentType ut = new ExperimentType();
        ut.setId(1);
        ut.setCode("UT");
        ut.setName("超声波检测");

        Report report = new Report();
        report.setId(1);
        report.setComponentName("主蒸汽管道");
        report.setReportNumber("UT-01");
        report.setHasDefect("是");

        ReportItem item = new ReportItem();
        item.setExperimentType(ut);
        item.setTableData(
                "{\"rows\":[{\"位置\":\"A-1\",\"波幅\":\"-12\",\"深度\":\"5.2\","
                        + "\"长度\":\"3\",\"高度\":\"1.1\",\"级别\":\"Ⅱ\"}]}");
        report.setReportItems(List.of(item));
        return report;
    }

    @Test
    void isSimplifiedOverviewDefectLineWithoutDetail_detectsLegacyPattern() throws Exception {
        Method m = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "isSimplifiedOverviewDefectLineWithoutDetail", String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null,
                "超声波检测中发现主蒸汽管道存在缺陷，详情请见单项报告3。"));
        assertFalse((Boolean) m.invoke(null,
                "超声波检测中发现主蒸汽管道存在缺陷，位置：A-1，详情请见单项报告3。"));
    }

    @Test
    void buildOverviewDefectLineAutoForSegment_includesNotificationDetail() throws Exception {
        Report report = utReportWithDefectRow();
        ExperimentType ut = report.getReportItems().get(0).getExperimentType();
        Method build = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "buildOverviewDefectLineAutoForSegment",
                Report.class, ExperimentType.class, com.reportweb.entity.Project.class,
                int.class, int.class);
        build.setAccessible(true);
        String line = (String) build.invoke(service, report, ut, null, 0, 1);
        assertTrue(line.contains("存在缺陷，其中，"));
        assertTrue(line.contains("详情请见单项报告"));
        assertFalse(line.matches("(?s).*存在缺陷，详情请见单项报告.*"));
        assertTrue(line.contains("位置") || line.contains("波幅"));
    }

    private static Report ptReportWithTwoContentRowsAndDefects() {
        ExperimentType pt = new ExperimentType();
        pt.setId(2);
        pt.setCode("PT");
        pt.setName("渗透检测");

        String tableData = """
                {
                  "perContentRow": [
                    {"rows": [{"序号":"1","起点位置":"1","终点位置":"11","长度":"1","级别":"1","备注":""}]},
                    {"rows": [{"序号":"1","起点位置":"1","终点位置":"1","长度":"1","级别":"1","备注":"1"}]}
                  ]
                }
                """;
        ReportItem item = new ReportItem();
        item.setExperimentType(pt);
        item.setTableData(tableData);

        Report report = new Report();
        report.setId(2);
        report.setComponentName("高温再热蒸汽管道");
        report.setReportNumber("CL2025-JCBG0198-007");
        report.setHasDefect("是");
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "mode", "table",
                "rows", List.of(
                        Map.of("type", "对接焊缝"),
                        Map.of("type", "弯头"))));
        return report;
    }

    private static Report uttReportWithThicknessDefect() {
        ExperimentType utt = new ExperimentType();
        utt.setId(3);
        utt.setCode("UTT");
        utt.setName("超声波测厚");

        String tableData = """
                {
                  "perContentRow": [
                    {"rows": [{"测点编号":"1","实测厚度 (mm)":"5.0"}]}
                  ]
                }
                """;
        ReportItem item = new ReportItem();
        item.setExperimentType(utt);
        item.setTableData(tableData);

        Report report = new Report();
        report.setId(3);
        report.setComponentName("高温再热蒸汽管道");
        report.setReportNumber("CL2025-JCBG0198-010");
        report.setHasDefect("是");
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "rows", List.of(Map.of("minRequiredThickness", "10"))));
        return report;
    }

    @Test
    void buildOverviewDefectLineAutoForSegment_ptMultiContentRow_usesWhereinAndTypeLabels() throws Exception {
        Report report = ptReportWithTwoContentRowsAndDefects();
        ExperimentType pt = report.getReportItems().get(0).getExperimentType();
        Method build = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "buildOverviewDefectLineAutoForSegment",
                Report.class, ExperimentType.class, com.reportweb.entity.Project.class,
                int.class, int.class);
        build.setAccessible(true);
        String line = (String) build.invoke(service, report, pt, null, 0, 1);

        assertTrue(line.contains("其中，"), line);
        assertTrue(line.contains("对接焊缝："), line);
        assertTrue(line.contains("弯头："), line);
        assertEquals(1, line.split("渗透检测中发现", -1).length - 1, line);
        assertEquals(1, line.split("详情请见单项报告", -1).length - 1, line);
        assertTrue(line.contains("终点位置为11"));
        assertTrue(line.contains("备注为1"));
        assertFalse(line.contains("。，"), line);
    }

    @Test
    void buildOverviewDefectLineAutoForSegment_uttConclusion_stripsResultLabelAndFixesPunctuation() throws Exception {
        Report report = uttReportWithThicknessDefect();
        ExperimentType utt = report.getReportItems().get(0).getExperimentType();
        Method build = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "buildOverviewDefectLineAutoForSegment",
                Report.class, ExperimentType.class, com.reportweb.entity.Project.class,
                int.class, int.class);
        build.setAccessible(true);
        String line = (String) build.invoke(service, report, utt, null, 0, 1);

        assertTrue(line.contains("超声波测厚中发现高温再热蒸汽管道存在缺陷，其中，"), line);
        assertTrue(line.contains("壁厚不满足DL438-2016的要求"), line);
        assertTrue(line.contains("详情请见单项报告CL2025-JCBG0198-010"), line);
        assertFalse(line.contains("检测结果"), line);
        assertFalse(line.contains("。，"), line);
    }

    private static Report pautMultiContentRowSingleTableBlock() {
        ExperimentType paut = new ExperimentType();
        paut.setId(10);
        paut.setCode("PAUT");
        paut.setName("相控阵超声检测");

        ReportItem item = new ReportItem();
        item.setExperimentType(paut);
        item.setTableData("{\"rows\":[{\"位置\":\"焊缝1\",\"级别\":\"Ⅱ\"}]}");

        Report report = new Report();
        report.setId(10);
        report.setComponentName("主蒸汽管道");
        report.setReportNumber("PAUT-01");
        report.setHasDefect("是");
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "mode", "table",
                "rows", List.of(
                        Map.of("type", "对接焊缝"),
                        Map.of("type", "弯头"))));
        return report;
    }

    @Test
    void getDetectionRowsForNotification_pautMultiContentRowSingleBlock_rowOneEmpty() throws Exception {
        Report report = pautMultiContentRowSingleTableBlock();
        Method m = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "getDetectionRowsForNotification", Report.class, int.class);
        m.setAccessible(true);
        JsonNode row0 = (JsonNode) m.invoke(service, report, 0);
        JsonNode row1 = (JsonNode) m.invoke(service, report, 1);
        assertTrue(row0.isArray() && row0.size() > 0);
        assertTrue(row1.isArray() && row1.isEmpty(), "越界分块不应回退到第 0 块数据");
    }

    @Test
    void collectComponentReportData_savedDefectLineWhenAutoDefectFalse_setsHasDefectAndResolved() throws Exception {
        Report report = pautMultiContentRowSingleTableBlock();
        report.setHasDefect("否");
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(ExportTextOverrides.BY_CONTENT_ROW, Map.of(
                "0", Map.of(ExportTextOverrides.OVERVIEW_DEFECT_LINE, "用户保存的 PAUT 缺陷概述句。")));
        report.setCustomFields(Map.of(ExportTextOverrides.CUSTOM_FIELDS_KEY, overrides));

        Project project = new Project();
        project.setReports(List.of(report));

        Method collect = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "collectComponentReportDataByCategoryAndType", Project.class, List.class);
        collect.setAccessible(true);
        Object data = collect.invoke(service, project, List.of(report));

        Field mapField = data.getClass().getDeclaredField("categoryToComponentToReports");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, List<Object>>> categoryMap =
                (Map<String, Map<String, List<Object>>>) mapField.get(data);
        Object info = categoryMap.values().stream()
                .flatMap(m -> m.values().stream())
                .flatMap(List::stream)
                .findFirst()
                .orElseThrow();
        Class<?> infoClass = Class.forName("com.reportweb.service.WordGeneratorServiceImpl$ReportInfo");
        assertEquals(infoClass, info.getClass());
        Field hasDefect = infoClass.getDeclaredField("hasDefect");
        hasDefect.setAccessible(true);
        Field resolved = infoClass.getDeclaredField("overviewDefectLineResolved");
        resolved.setAccessible(true);
        assertTrue((Boolean) hasDefect.get(info));
        assertEquals("用户保存的 PAUT 缺陷概述句。", resolved.get(info));
    }

    @Test
    void resolveOverviewDefectLineForExport_upgradesSimplifiedSavedOverride() throws Exception {
        Report report = utReportWithDefectRow();
        ExperimentType ut = report.getReportItems().get(0).getExperimentType();
        String simplified = "超声波检测中发现主蒸汽管道存在缺陷，详情请见单项报告3。";
        Method resolve = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "resolveOverviewDefectLineForExport",
                Report.class, ExperimentType.class, com.reportweb.entity.Project.class,
                int.class, int.class, String.class);
        resolve.setAccessible(true);
        String result = (String) resolve.invoke(service, report, ut, null, 0, 1, simplified);
        assertTrue(result.length() > simplified.length());
        assertTrue(result.contains("位置") || result.contains("波幅"));
        assertTrue(result.contains("详情请见单项报告"));
    }
}
