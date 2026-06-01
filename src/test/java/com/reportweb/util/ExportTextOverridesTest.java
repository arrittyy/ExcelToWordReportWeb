package com.reportweb.util;

import com.reportweb.entity.Report;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportTextOverridesTest {

    @Test
    void readStringAtContentRow_legacyTopLevelAppliesToRowZeroOnly() {
        Report report = new Report();
        report.setCustomFields(Map.of(
                ExportTextOverrides.CUSTOM_FIELDS_KEY,
                Map.of(
                        ExportTextOverrides.DETECTION_NARRATIVE_BODY, "  首段遗留  ",
                        ExportTextOverrides.CONCLUSION_PARAGRAPH, "结论0"
                )));
        assertEquals("首段遗留", ExportTextOverrides.detectionNarrativeBody(report, 0));
        assertEquals("结论0", ExportTextOverrides.conclusionParagraph(report, 0));
        assertEquals("", ExportTextOverrides.detectionNarrativeBody(report, 1));
    }

    @Test
    void mergeIntoCustomFields_withContentRowIndex_writesOverviewToByContentRow() {
        Report report = new Report();
        report.setCustomFields(new LinkedHashMap<>());
        Map<String, Object> cf = ExportTextOverrides.mergeIntoCustomFields(
                report.getCustomFields(),
                Map.of(
                        ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE, "第二段概述",
                        ExportTextOverrides.OVERVIEW_DEFECT_LINE, "第二段缺陷"
                ),
                1);
        report.setCustomFields(cf);
        assertEquals("第二段概述", ExportTextOverrides.overviewWorkContentLine(report, 1));
        assertEquals("第二段缺陷", ExportTextOverrides.overviewDefectLine(report, 1));
        assertEquals("", ExportTextOverrides.overviewWorkContentLine(report, 0));
    }

    @Test
    void mergeIntoCustomFields_withContentRowIndex_writesByContentRow() {
        Report report = new Report();
        report.setCustomFields(new LinkedHashMap<>());
        Map<String, String> patch = Map.of(
                ExportTextOverrides.DETECTION_NARRATIVE_BODY, "第二段正文",
                ExportTextOverrides.CONCLUSION_PARAGRAPH, "第二段结论"
        );
        Map<String, Object> cf = ExportTextOverrides.mergeIntoCustomFields(
                report.getCustomFields(), patch, 1);
        report.setCustomFields(cf);

        assertEquals("", ExportTextOverrides.detectionNarrativeBody(report, 0));
        assertEquals("第二段正文", ExportTextOverrides.detectionNarrativeBody(report, 1));
        assertEquals("第二段结论", ExportTextOverrides.conclusionParagraph(report, 1));

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) report.getCustomFields()
                .get(ExportTextOverrides.CUSTOM_FIELDS_KEY);
        assertTrue(root.containsKey(ExportTextOverrides.BY_CONTENT_ROW));
        assertFalse(root.containsKey(ExportTextOverrides.DETECTION_NARRATIVE_BODY));
    }

    @Test
    void mergeIntoCustomFields_clearsPerRowWithEmptyString() {
        Report report = new Report();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put(ExportTextOverrides.DETECTION_NARRATIVE_BODY, "待清除");
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(ExportTextOverrides.BY_CONTENT_ROW, Map.of("1", row1));
        report.setCustomFields(Map.of(ExportTextOverrides.CUSTOM_FIELDS_KEY, overrides));

        Map<String, Object> cf = ExportTextOverrides.mergeIntoCustomFields(
                report.getCustomFields(),
                Map.of(ExportTextOverrides.DETECTION_NARRATIVE_BODY, ""),
                1);
        report.setCustomFields(cf);
        assertEquals("", ExportTextOverrides.detectionNarrativeBody(report, 1));
    }

    @Test
    void overviewFields_doNotStackOverflowWhenReadViaContentRow() {
        Report report = new Report();
        report.setCustomFields(Map.of(
                ExportTextOverrides.CUSTOM_FIELDS_KEY,
                Map.of(ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE, "概述行")
        ));
        assertEquals("概述行", ExportTextOverrides.overviewWorkContentLine(report));
        assertEquals("概述行", ExportTextOverrides.readStringAtContentRow(
                report, 0, ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE));
        assertEquals("概述行", ExportTextOverrides.readString(report, ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE));
    }

    @Test
    void overviewMultiSegment_rowZeroIgnoresLegacyTopLevelOnly() {
        Report report = new Report();
        report.setCustomFields(Map.of(
                ExportTextOverrides.CUSTOM_FIELDS_KEY,
                Map.of(ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE, "多部件合并概述")
        ));
        assertEquals("", ExportTextOverrides.overviewWorkContentLine(report, 0, true));
        assertEquals("多部件合并概述", ExportTextOverrides.overviewWorkContentLine(report, 0, false));
    }

    @Test
    void overviewMultiSegment_defectLineRowZeroStillReadsLegacyTopLevel() {
        Report report = new Report();
        report.setCustomFields(Map.of(
                ExportTextOverrides.CUSTOM_FIELDS_KEY,
                Map.of(ExportTextOverrides.OVERVIEW_DEFECT_LINE, "顶层保存的缺陷概述")
        ));
        assertEquals("顶层保存的缺陷概述", ExportTextOverrides.overviewDefectLine(report, 0, true));
        assertEquals("顶层保存的缺陷概述", ExportTextOverrides.overviewDefectLine(report, 0, false));
    }

    @Test
    void migrateLegacyOnPerRowPatch_movesTopLevelToRowZero() {
        Report report = new Report();
        report.setCustomFields(Map.of(
                ExportTextOverrides.CUSTOM_FIELDS_KEY,
                new LinkedHashMap<>(Map.of(
                        ExportTextOverrides.DETECTION_NARRATIVE_BODY, "迁移正文"
                ))
        ));
        Map<String, Object> cf = ExportTextOverrides.mergeIntoCustomFields(
                report.getCustomFields(),
                Map.of(ExportTextOverrides.CONCLUSION_PARAGRAPH, "新结论"),
                0);
        report.setCustomFields(cf);
        assertEquals("迁移正文", ExportTextOverrides.detectionNarrativeBody(report, 0));
        assertEquals("新结论", ExportTextOverrides.conclusionParagraph(report, 0));
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) report.getCustomFields()
                .get(ExportTextOverrides.CUSTOM_FIELDS_KEY);
        assertFalse(root.containsKey(ExportTextOverrides.DETECTION_NARRATIVE_BODY));
    }
}
