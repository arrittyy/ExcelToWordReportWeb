package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * UT 缺陷表：录入 8 列（位置/波幅/深度等）须由 collectUTDefectRows 收集，而非 PT 式 6 列键。
 */
class UtDefectTableConsistencyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Object service;
    private Method collectUTDefectRows;
    private Method collectDefectRows;
    private Method getJsonValue;

    @BeforeEach
    void setUp() throws Exception {
        service = new WordGeneratorServiceImpl(
            null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        collectUTDefectRows = WordGeneratorServiceImpl.class.getDeclaredMethod(
            "collectUTDefectRows", com.fasterxml.jackson.databind.JsonNode.class);
        collectUTDefectRows.setAccessible(true);
        collectDefectRows = WordGeneratorServiceImpl.class.getDeclaredMethod(
            "collectDefectRows", com.fasterxml.jackson.databind.JsonNode.class);
        collectDefectRows.setAccessible(true);
        getJsonValue = WordGeneratorServiceImpl.class.getDeclaredMethod(
            "getJsonValue",
            com.fasterxml.jackson.databind.JsonNode.class,
            String.class,
            String.class);
        getJsonValue.setAccessible(true);
    }

    private ObjectNode detectionDataWithUtRow() {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode rows = MAPPER.createArrayNode();
        ObjectNode row = MAPPER.createObjectNode();
        row.put("序号", "1");
        row.put("位置", "A-1");
        row.put("波幅", "-12");
        row.put("深度", "5.2");
        row.put("长度", "3");
        row.put("高度", "1.1");
        row.put("级别", "Ⅱ");
        row.put("备注", "test");
        rows.add(row);
        root.set("rows", rows);
        return root;
    }

    @SuppressWarnings("unchecked")
    private List<com.fasterxml.jackson.databind.JsonNode> collectUt(ObjectNode data) throws Exception {
        return (List<com.fasterxml.jackson.databind.JsonNode>) collectUTDefectRows.invoke(service, data);
    }

    @SuppressWarnings("unchecked")
    private List<com.fasterxml.jackson.databind.JsonNode> collectPt(ObjectNode data) throws Exception {
        return (List<com.fasterxml.jackson.databind.JsonNode>) collectDefectRows.invoke(service, data);
    }

    private String jsonVal(com.fasterxml.jackson.databind.JsonNode row, String key, String def) throws Exception {
        return (String) getJsonValue.invoke(service, row, key, def);
    }

    @Test
    void collectUTDefectRows_readsEightColumnSchema() throws Exception {
        ObjectNode data = detectionDataWithUtRow();
        var rows = collectUt(data);
        assertEquals(1, rows.size());
        com.fasterxml.jackson.databind.JsonNode row = rows.get(0);
        assertEquals("A-1", jsonVal(row, "位置", "/"));
        assertEquals("-12", jsonVal(row, "波幅", "/"));
        assertEquals("5.2", jsonVal(row, "深度", "/"));
        assertEquals("3", jsonVal(row, "长度", "/"));
        assertEquals("1.1", jsonVal(row, "高度", "/"));
        assertEquals("Ⅱ", jsonVal(row, "级别", "/"));
    }

    @Test
    void ptStyleTableKeys_missUtColumns() throws Exception {
        ObjectNode data = detectionDataWithUtRow();
        var rows = collectPt(data);
        assertEquals(1, rows.size());
        com.fasterxml.jackson.databind.JsonNode row = rows.get(0);
        assertEquals("/", jsonVal(row, "起点位置", "/"));
        assertEquals("/", jsonVal(row, "终点位置", "/"));
        assertFalse("A-1".equals(jsonVal(row, "起点位置", "/")));
    }

    @Test
    void eightColumnWordMapping_usesPositionNotStartEnd() throws Exception {
        ObjectNode data = detectionDataWithUtRow();
        var row = collectUt(data).get(0);
        String pos = jsonVal(row, "位置", jsonVal(row, "起点位置", "/"));
        String amp = jsonVal(row, "波幅(dB)", jsonVal(row, "波幅", "/"));
        assertEquals("A-1", pos);
        assertEquals("-12", amp);
    }
}
