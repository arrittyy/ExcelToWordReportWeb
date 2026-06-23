package com.reportweb.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableSchemaUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void formatNotificationLine_followsSchemaColumnOrder() throws Exception {
        String schema = """
                {"columns":[
                  {"key":"序号","label":"序号","type":"text"},
                  {"key":"位置","label":"位置","type":"text"},
                  {"key":"波幅","label":"波幅（dB）","type":"number"}
                ]}
                """;
        List<TableSchemaUtil.SchemaColumn> cols = TableSchemaUtil.parseColumns(schema, objectMapper);
        ObjectNode row = objectMapper.createObjectNode();
        row.put("序号", "1");
        row.put("位置", "A");
        row.put("波幅", "10");
        String line = TableSchemaUtil.formatNotificationLine(row, cols);
        assertEquals("序号为1，位置为A，波幅为10dB", line);
    }

    @Test
    void formatLabeledPhrase_depthWithUnit() {
        assertEquals("深度为2mm", TableSchemaUtil.formatLabeledPhrase("深度（mm）", "2"));
        assertEquals("深度为2mm", TableSchemaUtil.formatLabeledPhrase("深度(mm)", "2"));
    }

    @Test
    void formatLabeledPhrase_plainLabel() {
        assertEquals("级别为Ⅲ", TableSchemaUtil.formatLabeledPhrase("级别", "Ⅲ"));
    }

    @Test
    void formatLabeledPhrase_typeValue_stripsParenthetical() {
        assertEquals("类型为螺栓", TableSchemaUtil.formatLabeledPhrase("类型", "螺栓（检测部位：端部）"));
        assertEquals("类型为角焊缝", TableSchemaUtil.formatLabeledPhrase("类型", "角焊缝（联箱）"));
    }

    @Test
    void rowSortKey_prefersNumberOverSerial() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("序号", "9");
        row.put("编号", "2");
        assertEquals("2", TableSchemaUtil.rowSortKey(row));
    }

    @Test
    void rowsSortBySerialNumberAscending() {
        ObjectNode r3 = objectMapper.createObjectNode();
        r3.put("序号", "3");
        r3.put("位置", "C");
        ObjectNode r1 = objectMapper.createObjectNode();
        r1.put("序号", "1");
        r1.put("位置", "A");
        List<com.fasterxml.jackson.databind.JsonNode> list = new ArrayList<>();
        list.add(r3);
        list.add(r1);
        list.sort(Comparator.comparing(TableSchemaUtil::rowSortKey, (a, b) -> {
            Integer la = extractFirstInt(a);
            Integer lb = extractFirstInt(b);
            if (la != null && lb != null) {
                return Integer.compare(la, lb);
            }
            return a.compareTo(b);
        }));
        assertEquals("1", TableSchemaUtil.rowSortKey(list.get(0)));
        assertEquals("3", TableSchemaUtil.rowSortKey(list.get(1)));
    }

    private static Integer extractFirstInt(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        var m = java.util.regex.Pattern.compile("(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
