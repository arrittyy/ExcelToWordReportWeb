package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonScalarStringNormalizerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizeTableDataJson_coercesNumbersAndBooleansInRows() throws Exception {
        String in = "{\"rows\":[{\"编号\":1,\"厚度\":3.5,\"ok\":true,\"note\":\"a\"}],\"extra\":1}";
        String out = JsonScalarStringNormalizer.normalizeTableDataJson(in, mapper);
        JsonNode root = mapper.readTree(out);
        JsonNode row = root.get("rows").get(0);
        assertTrue(row.get("编号").isTextual());
        assertEquals("1", row.get("编号").asText());
        assertTrue(row.get("厚度").isTextual());
        assertEquals("3.5", row.get("厚度").asText());
        assertTrue(row.get("ok").isTextual());
        assertEquals("true", row.get("ok").asText());
        assertEquals("a", row.get("note").asText());
        // 根上非 rows 字段不处理
        assertTrue(root.get("extra").isInt());
    }

    @Test
    void normalizeCustomFieldsMap_coercesScalars() {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("n", 42);
        in.put("f", 1.25);
        in.put("b", true);
        in.put("s", "x");
        Map<String, Object> out = JsonScalarStringNormalizer.normalizeCustomFieldsMap(in);
        assertEquals("42", out.get("n"));
        assertEquals("1.25", out.get("f"));
        assertEquals("true", out.get("b"));
        assertEquals("x", out.get("s"));
    }

    @Test
    void normalizeCustomFieldsMap_nullReturnsNull() {
        assertEquals(null, JsonScalarStringNormalizer.normalizeCustomFieldsMap(null));
    }

    @Test
    void normalizeTableDataJson_coercesNumbersInPerContentRow() throws Exception {
        String in = "{\"perContentRow\":[{\"rows\":[{\"测点编号\":1,\"实测厚度\":8.2}]}],\"rows\":[]}";
        String out = JsonScalarStringNormalizer.normalizeTableDataJson(in, mapper);
        JsonNode row = mapper.readTree(out).get("perContentRow").get(0).get("rows").get(0);
        assertTrue(row.get("测点编号").isTextual());
        assertEquals("1", row.get("测点编号").asText());
        assertTrue(row.get("实测厚度").isTextual());
        assertEquals("8.2", row.get("实测厚度").asText());
    }
}
