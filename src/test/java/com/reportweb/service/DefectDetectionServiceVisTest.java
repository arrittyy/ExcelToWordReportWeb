package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * 目视检测（VIS）：缺陷由用户手动选择 hasDefect，不因 resultDesc 自动判定。
 */
class DefectDetectionServiceVisTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefectDetectionService service = new DefectDetectionService(
            objectMapper,
            mock(DataComparisonService.class),
            mock(MaterialPropertyService.class),
            mock(AatDataComparisonService.class),
            mock(DetectionContentRowComponentResolver.class));

    private static ExperimentType visType() {
        ExperimentType et = new ExperimentType();
        et.setCode("VIS");
        et.setName("目视检测");
        return et;
    }

    @Test
    void vis_nonEmptyResultDesc_doesNotAutoDetect_returnsNull() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("resultDesc", "发现裂纹");
        item.put("imageIds", List.of());
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("locationDesc", "A区");
        group.put("items", List.of(item));
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("mode", "visual-groups");
        content.put("numberingRule", "");
        content.put("groups", List.of(group));

        Report report = new Report();
        report.setDetectionContent(content);
        report.setHasDefect("否");

        assertNull(service.hasDefect(report, visType(), null));
    }

    @Test
    void vis_userHasDefectYes_stillReturnsNullForAutoPath() {
        Report report = new Report();
        report.setHasDefect("是");
        assertNull(service.hasDefect(report, visType(), null));
    }
}
