package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 射线（RT）导出文本预览：showDefectSection 与概述缺陷行拼接仅含有效缺陷列数据行。
 */
class ExportTextPreviewRtTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WordGeneratorServiceImpl wordService;

    @BeforeEach
    void setUp() {
        ReportComponentMergeHelper mergeHelper = new ReportComponentMergeHelper();
        DetectionContentRowComponentResolver rowResolver =
                new DetectionContentRowComponentResolver(mergeHelper, MAPPER);
        DefectDetectionService defectDetectionService =
                new DefectDetectionService(MAPPER, null, null, null, rowResolver);
        DetectionContentNarrativeService narrativeService = new DetectionContentNarrativeService(MAPPER);
        wordService = new WordGeneratorServiceImpl(
                MAPPER, null, null, null, null, defectDetectionService, null, null,
                mergeHelper, rowResolver, null, narrativeService, null);
    }

    private static ExperimentType rtType() {
        ExperimentType rt = new ExperimentType();
        rt.setId(10);
        rt.setCode("RT");
        rt.setName("射线检测");
        return rt;
    }

    private static Report rtReportWithTableData(String tableDataJson) {
        ExperimentType rt = rtType();
        ReportItem item = new ReportItem();
        item.setExperimentType(rt);
        item.setTableData(tableDataJson);
        Report report = new Report();
        report.setId(100);
        report.setComponentName("受热面管");
        report.setReportNumber("RT-01");
        report.setHasDefect("是");
        report.setDetectionContent(Map.of("mode", "table", "rows", List.of(Map.of("type", "受热面管"))));
        report.setReportItems(List.of(item));
        return report;
    }

    @Test
    void buildExportTextPreview_meaningfulDefectColumn_showDefectSectionTrue() {
        String json = "{\"rows\":[{\"缺陷位置、性质及数量\":\"圆形缺陷 1 处\"}]}";
        var dto = wordService.buildExportTextPreview(rtReportWithTableData(json), 0, null);
        assertTrue(dto.isShowDefectSection());
        assertFalse(dto.getOverviewDefectLineDefault().isBlank());
    }

    @Test
    void buildExportTextPreview_jointOnlyNoDefectColumn_showDefectSectionFalse() {
        String json = "{\"rows\":[{\"焊接接头编号\":\"W1\",\"底片编号\":\"F1\"}]}";
        var dto = wordService.buildExportTextPreview(rtReportWithTableData(json), 0, null);
        assertFalse(dto.isShowDefectSection());
    }

    @Test
    void buildDetectionNotificationConclusion_rt_skipsRowsWithoutMeaningfulDefectColumn() throws Exception {
        String json = "{\"rows\":["
                + "{\"焊接接头编号\":\"W1\",\"缺陷位置、性质及数量\":\"/\"},"
                + "{\"焊接接头编号\":\"W2\",\"缺陷位置、性质及数量\":\"圆形缺陷 1 处\"}"
                + "]}";
        Report report = rtReportWithTableData(json);
        ExperimentType rt = rtType();
        Method m = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "buildDetectionNotificationConclusion",
                Report.class, ExperimentType.class, int.class);
        m.setAccessible(true);
        String conclusion = (String) m.invoke(wordService, report, rt, 0);
        assertTrue(conclusion.contains("圆形缺陷"));
        assertTrue(conclusion.contains("W2"));
        assertFalse(conclusion.contains("W1"));
    }
}
