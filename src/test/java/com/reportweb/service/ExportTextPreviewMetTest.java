package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportTextPreviewMetTest {

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

    @Test
    void buildExportTextPreview_metUsesSamePrefixForDetectionAndOverviewWork() {
        ExperimentType met = new ExperimentType();
        met.setId(20);
        met.setCode("MET");
        met.setName("金相检测");

        ReportItem item = new ReportItem();
        item.setExperimentType(met);
        item.setTableData("{\"rows\":[]}");

        Report report = new Report();
        report.setId(200);
        report.setReportNumber("MET-01");
        report.setHasDefect("否");
        report.setComponentName("再热器出口联箱");
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "mode", "dual-textarea",
                "position", "高温再热器出口联箱母材区域",
                "conclusion", "组织正常"));

        var dto = wordService.buildExportTextPreview(report, 0, null);
        assertTrue(dto.getDetectionNarrativeBodyDefault().contains("金相检测再热器出口联箱"));
        assertTrue(dto.getDetectionNarrativeBodyDefault().contains("具体位置：再热器出口联箱高温再热器出口联箱母材区域"));
        assertTrue(dto.getOverviewWorkContentLineDefault().startsWith(dto.getDetectionNarrativeBodyDefault()));
        assertTrue(dto.getOverviewWorkContentLineDefault().contains("详见后附单项报告编号MET-01。"));
    }
}
