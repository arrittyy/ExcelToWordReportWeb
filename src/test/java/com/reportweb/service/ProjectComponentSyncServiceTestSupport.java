package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link ProjectComponentSyncServiceTest} 用辅助类。 */
final class ProjectComponentSyncServiceTestSupport {

    static final String PART_A = "\u90e8\u4ef6A";
    static final String PART_B = "\u90e8\u4ef6B";
    static final String SEGMENT_1 = "\u7ba1\u6bb51";
    static final String SEGMENT_2 = "\u7ba1\u6bb52";
    static final String KEY_NAME = "\u540d\u79f0";
    static final String KEY_MATERIAL = "\u6750\u8d28";
    static final String KEY_NUMBER = "\u7f16\u53f7";

    private ProjectComponentSyncServiceTestSupport() {
    }

    static Map<String, Object> contentRow(String name, String material) {
        Map<String, Object> row = new HashMap<>();
        row.put(KEY_NAME, name);
        row.put(KEY_MATERIAL, material);
        return row;
    }

    static Answer<ProjectComponent> assignIdOnSaveAnswer() {
        return new AssignIdOnSaveAnswer();
    }

    static Answer<ProjectComponent> assignIdOnSaveForSecondOnlyAnswer() {
        return new AssignIdOnSaveForSecondOnlyAnswer();
    }

    static Report aatReportWithRows(int rowCount) {
        Report report = new Report();
        report.setId(1);
        report.setProjectId(1);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "\u710a\u7f1d");
            row.put("locationNumber", String.valueOf(i + 1));
            rows.add(row);
        }
        Map<String, Object> dc = new HashMap<>();
        dc.put("mode", "table");
        dc.put("rows", rows);
        report.setDetectionContent(dc);

        ReportItem item = new ReportItem();
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> dataRow = new HashMap<>();
            dataRow.put(KEY_NUMBER, String.valueOf(i + 1));
            dataRow.put("Mn", "0.5");
            Map<String, Object> block = new HashMap<>();
            block.put("rows", Arrays.asList(dataRow));
            blocks.add(block);
        }
        try {
            Map<String, Object> tableData = new HashMap<>();
            tableData.put("perContentRow", blocks);
            item.setTableData(new ObjectMapper().writeValueAsString(tableData));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        report.setReportItems(Arrays.asList(item));
        return report;
    }

    private static final class AssignIdOnSaveAnswer implements Answer<ProjectComponent> {
        @Override
        public ProjectComponent answer(InvocationOnMock invocation) {
            ProjectComponent comp = invocation.getArgument(0, ProjectComponent.class);
            if (PART_A.equals(comp.getComponentName())) {
                comp.setId(101);
            } else {
                comp.setId(102);
            }
            return comp;
        }
    }

    private static final class AssignIdOnSaveForSecondOnlyAnswer implements Answer<ProjectComponent> {
        @Override
        public ProjectComponent answer(InvocationOnMock invocation) {
            ProjectComponent comp = invocation.getArgument(0, ProjectComponent.class);
            if (comp.getId() == null) {
                comp.setId(51);
            }
            return comp;
        }
    }
}
