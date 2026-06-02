package com.reportweb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.service.DetectionContentNarrativeService;
import com.reportweb.util.ExportTextOverrides;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportsControllerMetOverrideTest {

    @Test
    void forceRefreshMetExportOverrides_overwritesLegacySavedText() throws Exception {
        DetectionContentNarrativeService narrativeService = new DetectionContentNarrativeService(new ObjectMapper());
        ReportsController controller = new ReportsController(
                null, null, null, null, null, null, null, null, null, null, null, null, narrativeService, null);

        Report report = new Report();
        report.setReportNumber("MET-99");
        report.setComponentName("再热器出口联箱");
        report.setDetectionContent(Map.of(
                "mode", "dual-textarea",
                "position", "高温再热器出口联箱母材区域",
                "conclusion", "组织正常"));

        Map<String, Object> oldOverrides = new LinkedHashMap<>();
        oldOverrides.put(ExportTextOverrides.DETECTION_NARRATIVE_BODY, "旧检测内容覆盖");
        oldOverrides.put(ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE, "旧工作内容覆盖详见后附单项报告编号OLD。");
        report.setCustomFields(Map.of(ExportTextOverrides.CUSTOM_FIELDS_KEY, oldOverrides));

        ExperimentType met = new ExperimentType();
        met.setCode("MET");
        met.setName("金相检测");

        Method m = ReportsController.class.getDeclaredMethod(
                "forceRefreshMetExportOverrides", Report.class, ExperimentType.class);
        m.setAccessible(true);
        m.invoke(controller, report, met);

        String detectionSaved = ExportTextOverrides.detectionNarrativeBody(report, 0);
        String workSaved = ExportTextOverrides.overviewWorkContentLine(report, 0, true);

        assertTrue(detectionSaved.contains("金相检测再热器出口联箱"));
        assertTrue(detectionSaved.contains("具体位置：再热器出口联箱高温再热器出口联箱母材区域"));
        assertTrue(workSaved.startsWith(detectionSaved));
        assertTrue(workSaved.contains("详见后附单项报告编号MET-99。"));
    }
}
