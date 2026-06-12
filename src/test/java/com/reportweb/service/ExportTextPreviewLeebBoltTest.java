package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 里氏螺栓/螺帽：检测内容预览完整叙述与结论按类型单显。
 */
class ExportTextPreviewLeebBoltTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WordGeneratorServiceImpl wordService;
    private MaterialPropertyService materialPropertyService;
    private com.reportweb.repository.ProjectComponentRepository projectComponentRepository;
    private DataComparisonService dataComparisonService;

    @BeforeEach
    void setUp() {
        ReportComponentMergeHelper mergeHelper = new ReportComponentMergeHelper();
        DetectionContentRowComponentResolver rowResolver =
                new DetectionContentRowComponentResolver(mergeHelper, MAPPER);
        materialPropertyService = mock(MaterialPropertyService.class);
        projectComponentRepository = mock(com.reportweb.repository.ProjectComponentRepository.class);
        dataComparisonService = new DataComparisonService(MAPPER);
        DefectDetectionService defectDetectionService =
                new DefectDetectionService(MAPPER, dataComparisonService, materialPropertyService, null, rowResolver);
        DetectionContentNarrativeService narrativeService = new DetectionContentNarrativeService(MAPPER);
        wordService = new WordGeneratorServiceImpl(
                MAPPER, null, projectComponentRepository, materialPropertyService, dataComparisonService,
                defectDetectionService, null, null, mergeHelper, rowResolver, null, narrativeService, null);
    }

    private static ExperimentType lhdType() {
        ExperimentType lhd = new ExperimentType();
        lhd.setId(20);
        lhd.setCode("LHD");
        lhd.setName("里氏硬度检测");
        return lhd;
    }

    private Report leebBoltReport(String type, String locationNumber, String total) {
        return leebReportWithRows(List.of(row(type, locationNumber, total)));
    }

    private Report leebReportWithRows(List<Map<String, String>> rows) {
        ExperimentType lhd = lhdType();
        ReportItem item = new ReportItem();
        item.setExperimentType(lhd);
        item.setTableData("{\"rows\":[{\"编号\":\"1\",\"平均\":\"280\"}]}");
        Report report = new Report();
        report.setId(200);
        report.setComponentName("高压螺栓");
        report.setTestMethod("里氏硬度检测");
        report.setProjectComponentId(5);
        Map<String, Object> detectionContent = new LinkedHashMap<>();
        detectionContent.put("mode", "table");
        detectionContent.put("rows", rows);
        report.setDetectionContent(detectionContent);
        report.setReportItems(List.of(item));
        return report;
    }

    private static Map<String, String> row(String type, String locationNumber, String total) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("locationNumber", locationNumber);
        m.put("total", total);
        m.put("locationDesc", "");
        return m;
    }

    private void stubMaterial() {
        stubMaterialWithRange("35CrMo", "255～311");
    }

    private void stubMaterialWithRange(String material, String boltRange) {
        ProjectComponent comp = new ProjectComponent();
        comp.setMaterial(material);
        when(projectComponentRepository.findById(anyInt())).thenReturn(Optional.of(comp));
        when(materialPropertyService.getMaterialProperty(material))
                .thenReturn(Map.of("里氏-螺栓", boltRange));
    }

    private Report leebBoltReportWithTableData(String type, String tableDataJson) {
        ExperimentType lhd = lhdType();
        ReportItem item = new ReportItem();
        item.setExperimentType(lhd);
        item.setTableData(tableDataJson);
        Report report = new Report();
        report.setId(200);
        report.setComponentName("高压螺栓");
        report.setTestMethod("里氏硬度检测");
        report.setProjectComponentId(5);
        Map<String, Object> detectionContent = new LinkedHashMap<>();
        detectionContent.put("mode", "table");
        detectionContent.put("rows", List.of(row(type, "A1", "10")));
        report.setDetectionContent(detectionContent);
        report.setReportItems(List.of(item));
        return report;
    }

    @Test
    void exportPreview_boltEndType_fullNarrativeNotOnlyEndWaist() {
        Report report = leebBoltReport("螺栓（检测部位：端部）", "A1", "10");
        var dto = wordService.buildExportTextPreview(report, 0, null);
        String body = dto.getDetectionNarrativeBodyDefault();
        assertNotEquals("端部", body.trim(), "预览不应仅为「端部」");
        assertTrue(body.contains("里氏硬度检测"), "应含完整检测方法叙述");
        assertTrue(body.contains("总计数量为10"));
        assertTrue(body.contains("端部"), "端部应作为具体位置出现在叙述中");
        assertFalse(body.contains("编号：A1"), "单行里氏预览与 Word 一致，叙述内不含编号段");
        assertFalse(body.contains("硬度测点编号方法"), "螺栓类型不应追加测点编号说明");
    }

    @Test
    void exportPreview_pipeJointType_includesMeasurementPointNote() {
        Report report = leebBoltReport("管件/对接焊缝", "P1", "4");
        var dto = wordService.buildExportTextPreview(report, 0, null);
        String body = dto.getDetectionNarrativeBodyDefault();
        assertTrue(body.contains("硬度测点编号方法"));
        assertTrue(body.contains("顺汽流方向顺时针间隔90°"));
    }

    @Test
    void exportPreview_boltType_conclusionOnlyBoltStandard() {
        stubMaterial();
        Report report = leebBoltReport("螺栓（检测部位：端部）", "A1", "10");
        var dto = wordService.buildExportTextPreview(report, 0, null);
        String conclusion = dto.getConclusionParagraphDefault();
        assertTrue(conclusion.contains("螺栓里氏硬度为255～311"));
        assertFalse(conclusion.contains("螺帽里氏硬度"));
        assertFalse(conclusion.contains("0.9"));
        assertTrue(conclusion.contains("所检测部件硬度符合标准要求"));
        assertFalse(conclusion.contains("检测结果符合标准要求"));
    }

    @Test
    void exportPreview_nutType_conclusionOnlyNutStandard() {
        stubMaterial();
        Report report = leebBoltReport("螺帽", "N1", "5");
        var dto = wordService.buildExportTextPreview(report, 0, null);
        String conclusion = dto.getConclusionParagraphDefault();
        assertTrue(conclusion.contains("螺帽里氏硬度为255～311"));
        assertFalse(conclusion.contains("螺栓里氏硬度"));
        assertTrue(conclusion.contains("所检测部件硬度符合标准要求"));
    }

    @Test
    void exportPreview_boltType_nonCompliance_usesDl438StyleTail() {
        stubMaterialWithRange("20CrMo", "197～241");
        String tableData = "{\"rows\":[{\"编号\":\"1\",\"平均\":\"150\"}]}";
        Report report = leebBoltReportWithTableData("螺栓（检测部位：端部）", tableData);
        var dto = wordService.buildExportTextPreview(report, 0, null);
        String conclusion = dto.getConclusionParagraphDefault();
        assertTrue(conclusion.contains("依据DL/T439"));
        assertTrue(conclusion.contains("螺栓里氏硬度为197～241"));
        assertTrue(conclusion.contains("1低于标准下限"));
        assertTrue(conclusion.contains("其余所检测部件硬度符合标准要求"));
        assertFalse(conclusion.contains("197～241低于下限"));
        assertFalse(conclusion.contains("低于下限编号"));
    }

    @Test
    void exportPreview_multiRowMixed_conclusionBothStandards() {
        stubMaterial();
        Report report = leebReportWithRows(List.of(
                row("螺栓（检测部位：端部）", "A1", "10"),
                row("螺帽", "N1", "5")));
        var dto = wordService.buildExportTextPreview(report, 0, null);
        String conclusion = dto.getConclusionParagraphDefault();
        assertTrue(conclusion.contains("螺栓里氏硬度为255～311"));
        assertTrue(conclusion.contains("螺帽里氏硬度为255～311"));
    }

    @Test
    void resolveBoltLeebDetectionLocation_stillReturnsEndWaistForMainTable() throws Exception {
        Report report = leebBoltReport("螺栓（检测部位：端部）", "A1", "10");
        Method m = WordGeneratorServiceImpl.class.getDeclaredMethod("resolveBoltLeebDetectionLocation", Report.class);
        m.setAccessible(true);
        assertEquals("端部", m.invoke(wordService, report));
    }
}
