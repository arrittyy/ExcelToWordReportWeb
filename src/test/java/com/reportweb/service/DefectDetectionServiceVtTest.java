package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * 内窥镜检测（VT）：缺陷由用户手动选择 hasDefect，不因检测数据或 detectionContent.result 自动判定。
 */
class DefectDetectionServiceVtTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefectDetectionService service = new DefectDetectionService(
            objectMapper,
            mock(DataComparisonService.class),
            mock(MaterialPropertyService.class),
            mock(AatDataComparisonService.class),
            mock(DetectionContentRowComponentResolver.class));

    private static ExperimentType vtType() {
        ExperimentType et = new ExperimentType();
        et.setCode("VT");
        et.setName("内窥镜检测");
        return et;
    }

    @Test
    void vt_tableDataRows_doesNotAutoDetect_returnsNull() throws Exception {
        String tableData = objectMapper.writeValueAsString(Map.of(
                "rows", List.of(Map.of("备注", "异常")),
                "perContentRow", List.of(Map.of("rows", List.of(Map.of("备注", "异常"))))));

        ReportItem item = new ReportItem();
        item.setTableData(tableData);

        Report report = new Report();
        report.setReportItems(List.of(item));
        report.setHasDefect("否");

        assertNull(service.hasDefect(report, vtType(), null));
    }

    @Test
    void vt_detectionContentResult_doesNotAutoDetect_returnsNull() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", "联箱");
        row.put("locationDesc", "A侧");
        row.put("method", "内窥镜");
        row.put("result", "存在异常内窥镜显示");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("mode", "table");
        content.put("rows", List.of(row));

        Report report = new Report();
        report.setDetectionContent(content);
        report.setHasDefect("否");

        assertNull(service.hasDefect(report, vtType(), null));
    }

    @Test
    void vt_userHasDefectYes_stillReturnsNullForAutoPath() {
        Report report = new Report();
        report.setHasDefect("是");
        assertNull(service.hasDefect(report, vtType(), null));
    }
}
