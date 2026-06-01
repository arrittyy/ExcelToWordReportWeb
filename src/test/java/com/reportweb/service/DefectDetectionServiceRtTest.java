package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 射线（RT）缺陷判定：仅「缺陷位置、性质及数量」列有效内容计为有缺陷。
 */
class DefectDetectionServiceRtTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefectDetectionService service = new DefectDetectionService(
            objectMapper,
            mock(DataComparisonService.class),
            mock(MaterialPropertyService.class),
            mock(AatDataComparisonService.class),
            mock(DetectionContentRowComponentResolver.class));

    private static ExperimentType rtType() {
        ExperimentType et = new ExperimentType();
        et.setCode("RT");
        return et;
    }

    private static Report reportWithTableData(String tableDataJson) {
        ReportItem item = new ReportItem();
        item.setTableData(tableDataJson);
        Report report = new Report();
        report.setReportItems(List.of(item));
        return report;
    }

    @Test
    void rt_rowsWithOnlySlashOrEmptyDefectColumn_returnsNo() {
        String json = "{\"rows\":[{\"缺陷位置、性质及数量\":\"/\"},{\"缺陷位置、性质及数量\":\"  \"}]}";
        assertEquals("否", service.hasDefect(reportWithTableData(json), rtType(), null));
    }

    @Test
    void rt_fullwidthSlashDefectColumn_returnsNo() {
        String json = "{\"rows\":[{\"缺陷位置、性质及数量\":\"／\"}]}";
        assertEquals("否", service.hasDefect(reportWithTableData(json), rtType(), null));
    }

    @Test
    void rt_meaningfulDefectColumn_returnsYes() {
        String json = "{\"rows\":[{\"缺陷位置、性质及数量\":\"圆形缺陷 1 处\"}]}";
        assertEquals("是", service.hasDefect(reportWithTableData(json), rtType(), null));
    }

    @Test
    void rt_noTableData_returnsNo() {
        Report report = new Report();
        ReportItem item = new ReportItem();
        item.setTableData("");
        report.setReportItems(List.of(item));
        assertEquals("否", service.hasDefect(report, rtType(), null));
    }

    @Test
    void rt_emptyMergedRows_returnsNo() {
        String json = "{\"rows\":[]}";
        assertEquals("否", service.hasDefect(reportWithTableData(json), rtType(), null));
    }

    @Test
    void hasRadiographicDefectInBlock_jointOnlyNoDefectColumn_false() {
        String json = "{\"rows\":[{\"焊接接头编号\":\"W1\",\"底片编号\":\"F1\"}]}";
        assertFalse(service.hasRadiographicDefectInBlock(reportWithTableData(json), 0));
    }

    @Test
    void hasRadiographicDefectInBlock_meaningfulDefectColumn_true() {
        String json = "{\"rows\":[{\"缺陷位置、性质及数量\":\"圆形缺陷 1 处\"}]}";
        assertTrue(service.hasRadiographicDefectInBlock(reportWithTableData(json), 0));
    }
}
