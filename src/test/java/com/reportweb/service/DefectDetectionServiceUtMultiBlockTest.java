package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 超声多部件：仅对应 perContentRow 分块有有效缺陷行时，该部件判为有缺陷。
 */
class DefectDetectionServiceUtMultiBlockTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DefectDetectionService service;
    private ReportComponentMergeHelper mergeHelper;

    @BeforeEach
    void setUp() {
        mergeHelper = mock(ReportComponentMergeHelper.class);
        DetectionContentRowComponentResolver rowResolver =
                new DetectionContentRowComponentResolver(mergeHelper, MAPPER);
        service = new DefectDetectionService(
                MAPPER,
                mock(DataComparisonService.class),
                mock(MaterialPropertyService.class),
                mock(AatDataComparisonService.class),
                rowResolver);
    }

    private static ExperimentType utType() {
        ExperimentType et = new ExperimentType();
        et.setCode("UT");
        return et;
    }

    private Report reportWithTwoBlocks() {
        String tableData = """
                {
                  "perContentRow": [
                    {"rows": [{"序号":"1","位置":"A-1","波幅":"-12","深度":"5","长度":"3","高度":"1","级别":"Ⅱ","备注":""}]},
                    {"rows": [{"序号":"/","位置":"/","波幅":"/","深度":"/","长度":"/","高度":"/","级别":"/","备注":"/"}]}
                  ]
                }
                """;
        ReportItem item = new ReportItem();
        item.setTableData(tableData);
        Report report = new Report();
        report.setId(1);
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "rows", List.of(
                        Map.of("type", "对接焊缝"),
                        Map.of("type", "对接焊缝"))));
        when(mergeHelper.resolveComponentIds(report)).thenReturn(List.of(101, 102));
        return report;
    }

    @Test
    void multiBlock_defectOnlyInFirstBlock_secondComponentNoDefect() {
        Report report = reportWithTwoBlocks();
        ProjectComponent compA = new ProjectComponent();
        compA.setId(101);
        ProjectComponent compB = new ProjectComponent();
        compB.setId(102);

        assertEquals("是", service.hasDefect(report, utType(), compA));
        assertEquals("否", service.hasDefect(report, utType(), compB));
        assertEquals("是", service.hasDefect(report, utType(), null));
        assertEquals("是", service.hasDefectForComponents(report, utType(), List.of(compA, compB)));
    }

    @Test
    void multiBlock_emptyDefectFieldsInBlock_returnsNo() {
        String tableData = """
                {
                  "perContentRow": [
                    {"rows": [{"序号":"","位置":"","波幅":"","深度":"","长度":"","高度":"","级别":"","备注":""}]},
                    {"rows": []}
                  ]
                }
                """;
        ReportItem item = new ReportItem();
        item.setTableData(tableData);
        Report report = new Report();
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of("rows", List.of(Map.of(), Map.of())));
        when(mergeHelper.resolveComponentIds(report)).thenReturn(List.of(101, 102));

        assertEquals("否", service.hasDefect(report, utType(), null));
    }

    @Test
    void singleComponent_twoContentRows_defectOnlyInSecondBlock_reportLevelYes() {
        String tableData = """
                {
                  "perContentRow": [
                    {"rows": [{"序号":"","位置":"","波幅":"","深度":"","长度":"","高度":"","级别":"","备注":""}]},
                    {"rows": [{"序号":"1","位置":"B-2","波幅":"-10","深度":"4","长度":"2","高度":"1","级别":"Ⅱ","备注":""}]}
                  ]
                }
                """;
        ReportItem item = new ReportItem();
        item.setTableData(tableData);
        Report report = new Report();
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "rows", List.of(
                        Map.of("type", "对接焊缝"),
                        Map.of("type", "弯头"))));
        when(mergeHelper.resolveComponentIds(report)).thenReturn(List.of(101));

        ProjectComponent comp = new ProjectComponent();
        comp.setId(101);

        assertEquals("是", service.hasDefect(report, utType(), null));
        assertEquals("是", service.hasDefectForComponents(report, utType(), List.of(comp)));
    }

    @Test
    void blockIndexOutOfRange_doesNotTreatBlockZeroAsBlockOne() {
        String tableData = """
                {
                  "perContentRow": [
                    {"rows": [{"序号":"1","位置":"A-1","波幅":"-12","深度":"5","长度":"3","高度":"1","级别":"Ⅱ","备注":""}]}
                  ]
                }
                """;
        ReportItem item = new ReportItem();
        item.setTableData(tableData);
        Report report = new Report();
        report.setReportItems(List.of(item));
        report.setDetectionContent(Map.of(
                "rows", List.of(Map.of("type", "对接焊缝"), Map.of("type", "弯头"))));
        when(mergeHelper.resolveComponentIds(report)).thenReturn(List.of(101));

        assertFalse(service.hasNdtDefectInTableDataBlock(report, 1));
        assertTrue(service.hasNdtDefectInTableDataBlock(report, 0));
    }
}
