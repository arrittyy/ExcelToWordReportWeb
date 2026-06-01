package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UT/PAUT 多行拆分：每段应对应 perContentRow 一块；溢出附页取 subList(1, n) 而非仅在末段输出。
 */
class TableDataMergeUtilMultiRowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void perContentRowBlocks_alignWithDetectionContentRows() throws Exception {
        String tableData = """
                {
                  "perContentRow": [
                    { "rows": [
                      { "序号": "1", "位置": "A1" },
                      { "序号": "2", "位置": "A2" },
                      { "序号": "3", "位置": "A3" }
                    ]},
                    { "rows": [
                      { "序号": "1", "位置": "B1" },
                      { "序号": "2", "位置": "B2" }
                    ]}
                  ]
                }
                """;

        List<JsonNode> blocks = TableDataMergeUtil.perContentRowBlocks(tableData, objectMapper);
        assertEquals(2, blocks.size());

        int mainRows = 1;
        for (int segment = 0; segment < blocks.size(); segment++) {
            JsonNode block = blocks.get(segment);
            int rowCount = block.get("rows").size();
            int overflow = rowCount - mainRows;
            if (segment == 0) {
                assertEquals(3, rowCount);
                assertEquals(2, overflow);
            } else {
                assertEquals(2, rowCount);
                assertEquals(1, overflow);
            }
        }
    }
}
