package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DetectionContentRowComponentResolverTest {

    private DetectionContentRowComponentResolver resolver;
    private Report report;
    private List<ProjectComponent> comps;

    @BeforeEach
    void setUp() {
        resolver = new DetectionContentRowComponentResolver(
                new ReportComponentMergeHelper(),
                new ObjectMapper());
        report = new Report();
        report.setProjectComponentIds(Arrays.asList(10, 20));

        ProjectComponent c10 = new ProjectComponent();
        c10.setId(10);
        c10.setComponentName("部件A");
        ProjectComponent c20 = new ProjectComponent();
        c20.setId(20);
        c20.setComponentName("部件B");
        comps = Arrays.asList(c10, c20);

        Map<String, Object> dc = new HashMap<>();
        dc.put("mode", "table");
        dc.put("rows", Arrays.asList(
                Map.of("type", "t0"),
                Map.of("type", "t1"),
                Map.of("type", "t2", "projectComponentId", 10),
                Map.of("type", "t3")));
        report.setDetectionContent(dc);
    }

    @Test
    void resolveComponentId_usesIndexWhenNoRowOverride() {
        assertEquals(10, resolver.resolveComponentId(report, 0));
        assertEquals(20, resolver.resolveComponentId(report, 1));
    }

    @Test
    void resolveComponentId_usesRowProjectComponentIdWhenInSelectedList() {
        assertEquals(10, resolver.resolveComponentId(report, 2));
    }

    @Test
    void resolveComponentId_returnsNullWhenBeyondSelectedAndNoRowId() {
        assertNull(resolver.resolveComponentId(report, 3));
    }

    @Test
    void resolve_returnsMatchingComponentForRowOverride() {
        ProjectComponent row2 = resolver.resolve(report, comps, 2);
        assertEquals(10, row2.getId());
        assertEquals("部件A", row2.getComponentName());
    }

    @Test
    void resolve_fallsBackToFirstWhenBeyondRangeAndNoRowId() {
        ProjectComponent row3 = resolver.resolve(report, comps, 3);
        assertEquals(10, row3.getId());
    }
}
