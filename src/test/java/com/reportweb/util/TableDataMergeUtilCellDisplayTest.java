package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableDataMergeUtilCellDisplayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void nullRow_returnsEmpty() {
        assertEquals("", TableDataMergeUtil.tableDataCellDisplay(null, "编号"));
    }

    @Test
    void placeholderRow_returnsSlash() throws Exception {
        JsonNode row = objectMapper.readTree("""
                {"编号":"/","1":"/","2":"/","平均":"/"}
                """);
        assertEquals("/", TableDataMergeUtil.tableDataCellDisplay(row, "编号"));
        assertEquals("/", TableDataMergeUtil.tableDataCellDisplay(row, "平均"));
    }

    @Test
    void dataRow_withValue_andMissingField() throws Exception {
        JsonNode row = objectMapper.readTree("""
                {"编号":"A-1","1":120,"平均":118.5}
                """);
        assertEquals("A-1", TableDataMergeUtil.tableDataCellDisplay(row, "编号"));
        assertEquals("120", TableDataMergeUtil.tableDataCellDisplay(row, "1"));
        assertEquals("118.5", TableDataMergeUtil.tableDataCellDisplay(row, "平均"));
        assertEquals("", TableDataMergeUtil.tableDataCellDisplay(row, "2"));
    }

    @Test
    void displayFirst_triesKeysInOrder() throws Exception {
        JsonNode row = objectMapper.readTree("""
                {"起点位置":"P1"}
                """);
        assertEquals("P1", TableDataMergeUtil.tableDataCellDisplayFirst(row, "位置", "起点位置"));
    }

    @Test
    void displayFirst_placeholderRow_returnsSlash() throws Exception {
        JsonNode row = objectMapper.readTree("""
                {"位置":"/","起点位置":"/"}
                """);
        assertEquals("/", TableDataMergeUtil.tableDataCellDisplayFirst(row, "位置", "起点位置"));
    }
}
