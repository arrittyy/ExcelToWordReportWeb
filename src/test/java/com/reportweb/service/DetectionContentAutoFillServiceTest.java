package com.reportweb.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DetectionContentAutoFillServiceTest {

    private final DetectionContentAutoFillService service = new DetectionContentAutoFillService();

    @Test
    void generateFromTableData_preservesMinRequiredThicknessForUtm() {
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("mode", "table");
        existing.put(
                "rows",
                List.of(
                        Map.of(
                                "type",
                                "直管段",
                                "locationDesc",
                                "A区",
                                "locationNumber",
                                "",
                                "total",
                                "",
                                "minRequiredThickness",
                                "4.500")));

        String tableData =
                """
                {"perContentRow":[{"rows":[{"测点编号":"1","实测厚度 (mm)":"5.0"}]}],"rows":[{"测点编号":"1","实测厚度 (mm)":"5.0"}]}
                """;

        Map<String, Object> result =
                service.generateFromTableData("UTM", "超声波测厚", tableData, existing);

        assertEquals("table", result.get("mode"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(1, rows.size());
        assertEquals("直管段", rows.get(0).get("type"));
        assertEquals("A区", rows.get(0).get("locationDesc"));
        assertEquals("1", rows.get(0).get("locationNumber"));
        assertEquals("1", rows.get(0).get("total"));
        assertEquals("4.500", rows.get(0).get("minRequiredThickness"));
    }

    @Test
    void generateFromTableData_rowCountFollowsPerContentRowBlocks_notExistingRows() {
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("mode", "table");
        existing.put(
                "rows",
                List.of(
                        Map.of("type", "A", "locationDesc", "", "locationNumber", "", "total", ""),
                        Map.of("type", "B", "locationDesc", "", "locationNumber", "", "total", ""),
                        Map.of("type", "C", "locationDesc", "", "locationNumber", "", "total", "")));

        String tableData =
                """
                {"perContentRow":[{"rows":[{"测点编号":"1"}]},{"rows":[{"测点编号":"2"}]}],"rows":[{"测点编号":"1"},{"测点编号":"2"}]}
                """;

        Map<String, Object> result =
                service.generateFromTableData("UTM", "超声波测厚", tableData, existing);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(2, rows.size());
        assertEquals("A", rows.get(0).get("type"));
        assertEquals("B", rows.get(1).get("type"));
        assertEquals("1", rows.get(0).get("locationNumber"));
        assertEquals("2", rows.get(1).get("locationNumber"));
    }
}
