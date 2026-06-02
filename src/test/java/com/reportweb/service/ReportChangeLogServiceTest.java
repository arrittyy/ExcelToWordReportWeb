package com.reportweb.service;

import com.reportweb.entity.Report;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportChangeLogServiceTest {

    @Test
    void metadataSnapshot_diffsMetadataFieldsOnly() {
        Report before = new Report();
        before.setExperimentTypeId(28);
        before.setReportNumber("CL2026-001");
        before.setStatus("Draft");
        before.setInspector("张三");
        before.setTestDate(LocalDate.of(2026, 6, 1));

        Report after = new Report();
        after.setExperimentTypeId(28);
        after.setReportNumber("CL2026-001");
        after.setStatus("Completed");
        after.setInspector("张三");
        after.setTestDate(LocalDate.of(2026, 6, 1));

        Map<String, Object> summary = ReportChangeLogService.ReportMetadataSnapshot.from(before).diffTo(after);
        assertNotNull(summary);
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) summary.get("fields");
        assertEquals(1, fields.size());
        assertEquals("status", fields.get(0));
    }

    @Test
    void metadataSnapshot_emptyDiffWhenOnlyLogicalEquality() {
        Report report = new Report();
        report.setReportNumber("A-001");
        report.setStatus("Draft");

        Map<String, Object> summary = ReportChangeLogService.ReportMetadataSnapshot.from(report).diffTo(report);
        assertNotNull(summary);
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) summary.get("fields");
        assertTrue(fields.isEmpty());
    }
}
