package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeebHardnessModeResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void isBoltOrNutMode_fromDetectionContent_boltEnd() throws Exception {
        Report report = reportWithDetectionType("螺栓（检测部位：端部）", null);
        assertTrue(LeebHardnessModeResolver.isBoltOrNutMode(report, objectMapper));
    }

    @Test
    void isBoltOrNutMode_fromDetectionContent_boltWaist() throws Exception {
        Report report = reportWithDetectionType("螺栓（检测部位：腰部）", null);
        assertTrue(LeebHardnessModeResolver.isBoltOrNutMode(report, objectMapper));
    }

    @Test
    void isBoltOrNutMode_fromDetectionContent_nut() throws Exception {
        Report report = reportWithDetectionType("螺帽", null);
        assertTrue(LeebHardnessModeResolver.isBoltOrNutMode(report, objectMapper));
    }

    @Test
    void isBoltOrNutMode_fromTableDataType_whenContentIsPipe() throws Exception {
        String tableData =
                """
                {"rows":[{"编号":"1","平均":"250","类型":"螺栓"}]}
                """;
        Report report = reportWithDetectionType("管件/对接焊缝", tableData);
        assertTrue(LeebHardnessModeResolver.isBoltOrNutMode(report, objectMapper));
    }

    @Test
    void isBoltOrNutMode_false_forPipeJointOnly() throws Exception {
        Report report = reportWithDetectionType("管件/对接焊缝", "{\"rows\":[{\"编号\":\"1\",\"平均\":\"250\"}]}");
        assertFalse(LeebHardnessModeResolver.isBoltOrNutMode(report, objectMapper));
    }

    @Test
    void resolveLeebBoltRange_prefersExplicitBoltKey() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("里氏-螺栓", "255～311");
        props.put("里氏-管件", "100～200");
        assertEquals("255～311", LeebHardnessModeResolver.resolveLeebBoltRange(props));
    }

    @Test
    void resolveLeebBoltRange_fallsBackToPipe() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("里氏-管件", "100～200");
        assertEquals("100～200", LeebHardnessModeResolver.resolveLeebBoltRange(props));
    }

    @Test
    void extractBoltLeebEndWaist_fromBoltEndType() {
        assertEquals("端部", LeebHardnessModeResolver.extractBoltLeebEndWaist("螺栓（检测部位：端部）"));
    }

    @Test
    void extractBoltLeebEndWaist_fromBoltWaistType() {
        assertEquals("腰部", LeebHardnessModeResolver.extractBoltLeebEndWaist("螺栓（检测部位：腰部）"));
    }

    @Test
    void extractBoltLeebEndWaist_emptyForNut() {
        assertEquals("", LeebHardnessModeResolver.extractBoltLeebEndWaist("螺帽"));
    }

    @Test
    void typeTextIsPipeJointWeld_trueForPipeJointAndLegacy() {
        assertTrue(LeebHardnessModeResolver.typeTextIsPipeJointWeld("管件/对接焊缝"));
        assertTrue(LeebHardnessModeResolver.typeTextIsPipeJointWeld("管件"));
        assertTrue(LeebHardnessModeResolver.typeTextIsPipeJointWeld("对接焊缝"));
    }

    @Test
    void typeTextIsPipeJointWeld_falseForBoltNutAndShaft() {
        assertFalse(LeebHardnessModeResolver.typeTextIsPipeJointWeld("螺栓（检测部位：端部）"));
        assertFalse(LeebHardnessModeResolver.typeTextIsPipeJointWeld("螺帽"));
        assertFalse(LeebHardnessModeResolver.typeTextIsPipeJointWeld("大轴"));
    }

    private Report reportWithDetectionType(String type, String tableData) throws Exception {
        Report report = new Report();
        Map<String, Object> detectionContent = new LinkedHashMap<>();
        detectionContent.put("mode", "table");
        detectionContent.put(
                "rows",
                List.of(Map.of("type", type, "locationNumber", "", "total", "", "locationDesc", "")));
        report.setDetectionContent(detectionContent);
        if (tableData != null) {
            ReportItem item = new ReportItem();
            item.setTableData(tableData);
            report.setReportItems(List.of(item));
        }
        return report;
    }
}
