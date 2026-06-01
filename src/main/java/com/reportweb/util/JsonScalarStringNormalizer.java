package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将录入 JSON 中的标量（数字、布尔）规范为字符串，便于持久化类型一致；比对与 Word 生成端再按需解析数值。
 */
public final class JsonScalarStringNormalizer {

    private JsonScalarStringNormalizer() {
    }

    /**
     * 规范化 {@code tableData} JSON：对 {@code rows} 数组中每个对象的第一层字段，
     * 将 {@link JsonNode#isNumber()} 与 {@link JsonNode#isBoolean()} 转为 JSON 字符串节点。
     *
     * @param tableDataJson 原始 JSON 字符串，可为 null 或空白
     * @param mapper        Jackson ObjectMapper
     * @return 规范化后的 JSON 字符串；解析失败时返回原字符串
     */
    public static String normalizeTableDataJson(String tableDataJson, ObjectMapper mapper) {
        if (tableDataJson == null || tableDataJson.isBlank()) {
            return tableDataJson;
        }
        try {
            JsonNode root = mapper.readTree(tableDataJson);
            if (!root.isObject()) {
                return tableDataJson;
            }
            ObjectNode obj = (ObjectNode) root;
            ObjectNode copy = obj.deepCopy();
            JsonNode rows = obj.get("rows");
            if (rows != null && rows.isArray()) {
                copy.set("rows", normalizeRowArray(mapper, rows));
            }
            JsonNode perContentRow = obj.get("perContentRow");
            if (perContentRow != null && perContentRow.isArray()) {
                ArrayNode newBlocks = mapper.createArrayNode();
                for (JsonNode block : perContentRow) {
                    if (block != null && block.isObject()) {
                        ObjectNode newBlock = ((ObjectNode) block).deepCopy();
                        JsonNode blockRows = block.get("rows");
                        if (blockRows != null && blockRows.isArray()) {
                            newBlock.set("rows", normalizeRowArray(mapper, blockRows));
                        }
                        newBlocks.add(newBlock);
                    } else {
                        newBlocks.add(block);
                    }
                }
                copy.set("perContentRow", newBlocks);
            }
            return mapper.writeValueAsString(copy);
        } catch (Exception e) {
            return tableDataJson;
        }
    }

    private static ArrayNode normalizeRowArray(ObjectMapper mapper, JsonNode rows) {
        ArrayNode newRows = mapper.createArrayNode();
        for (JsonNode row : rows) {
            if (row != null && row.isObject()) {
                newRows.add(normalizeRowObject(mapper, (ObjectNode) row));
            } else {
                newRows.add(row);
            }
        }
        return newRows;
    }

    private static ObjectNode normalizeRowObject(ObjectMapper mapper, ObjectNode row) {
        ObjectNode out = mapper.createObjectNode();
        row.fields().forEachRemaining(e -> {
            String key = e.getKey();
            JsonNode v = e.getValue();
            if (v != null && (v.isNumber() || v.isBoolean())) {
                out.put(key, v.asText());
            } else {
                out.set(key, v);
            }
        });
        return out;
    }

    /**
     * 将 customFields 中值为 {@link Number}、{@link Boolean} 的条目转为字符串；其它类型原样保留。
     */
    public static Map<String, Object> normalizeCustomFieldsMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                out.put(e.getKey(), String.valueOf(v));
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }
}
