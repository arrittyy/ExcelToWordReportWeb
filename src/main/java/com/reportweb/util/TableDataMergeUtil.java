package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * reportItems.tableData：perContentRow 分块与顶层 rows 合并（缺陷判断、Word 缺陷表等读合并结果）。
 */
public final class TableDataMergeUtil {

    private static final Set<String> TABLE_ROW_META_KEYS;

    static {
        Set<String> s = new HashSet<>();
        s.add("_rid");
        s.add("key");
        TABLE_ROW_META_KEYS = Collections.unmodifiableSet(s);
    }

    private TableDataMergeUtil() {
    }

    /**
     * 检测数据表末尾占位行：除 _rid、key 外，每个字段均为字符串「/」。
     * 与前端保存逻辑一致，用于 Word「总计」等不计入该行。
     */
    public static boolean isTrailingSlashPlaceholderRow(JsonNode row) {
        if (row == null || !row.isObject()) {
            return false;
        }
        boolean anyBiz = false;
        Iterator<Map.Entry<String, JsonNode>> it = row.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String name = e.getKey();
            if (TABLE_ROW_META_KEYS.contains(name)) {
                continue;
            }
            anyBiz = true;
            JsonNode v = e.getValue();
            if (v == null || !v.isTextual()) {
                return false;
            }
            if (!"/".equals(v.asText().trim())) {
                return false;
            }
        }
        return anyBiz;
    }

    /** 合并后的行列表用于「总计」展示时的条数（不含末尾全「/」占位行）。 */
    public static int effectiveRowCountExcludingTrailingPlaceholder(List<JsonNode> rows) {
        int n = rows.size();
        if (n == 0) {
            return 0;
        }
        if (isTrailingSlashPlaceholderRow(rows.get(n - 1))) {
            return n - 1;
        }
        return n;
    }

    /**
     * 从 tableData JSON 字符串解析并合并所有分块的 rows（顺序：分块 0,1,…）。
     * 若无 perContentRow 则使用顶层 rows（兼容旧数据）。
     */
    public static ArrayNode mergedRowsFromTableDataJson(String tableDataJson, ObjectMapper objectMapper) {
        if (tableDataJson == null || tableDataJson.trim().isEmpty()) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode root = objectMapper.readTree(tableDataJson);
            return mergedRowsFromTableDataNode(root, objectMapper);
        } catch (Exception e) {
            return objectMapper.createArrayNode();
        }
    }

    public static ArrayNode mergedRowsFromTableDataNode(JsonNode root, ObjectMapper objectMapper) {
        ArrayNode out = objectMapper.createArrayNode();
        if (root == null || !root.isObject()) {
            return out;
        }
        if (root.has("perContentRow") && root.get("perContentRow").isArray()) {
            for (JsonNode block : root.get("perContentRow")) {
                if (block != null && block.has("rows") && block.get("rows").isArray()) {
                    for (JsonNode row : block.get("rows")) {
                        out.add(row);
                    }
                }
            }
            if (out.size() > 0) {
                return out;
            }
        }
        if (root.has("rows") && root.get("rows").isArray()) {
            for (JsonNode row : root.get("rows")) {
                out.add(row);
            }
        }
        return out;
    }

    /**
     * 返回仅含合并后 rows 的 JSON 对象节点，供仍调用 get("rows") 的逻辑使用。
     */
    public static ObjectNode tableDataWithMergedRowsOnly(String tableDataJson, ObjectMapper objectMapper) {
        ArrayNode merged = mergedRowsFromTableDataJson(tableDataJson, objectMapper);
        ObjectNode o = objectMapper.createObjectNode();
        o.set("rows", merged);
        return o;
    }

    public static List<JsonNode> perContentRowBlocks(String tableDataJson, ObjectMapper objectMapper) {
        List<JsonNode> blocks = new ArrayList<>();
        if (tableDataJson == null || tableDataJson.trim().isEmpty()) {
            return blocks;
        }
        try {
            JsonNode root = objectMapper.readTree(tableDataJson);
            if (root != null && root.isObject() && root.has("perContentRow") && root.get("perContentRow").isArray()) {
                for (JsonNode b : root.get("perContentRow")) {
                    blocks.add(b);
                }
            }
        } catch (Exception ignored) {
        }
        if (blocks.isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(tableDataJson);
                if (root != null && root.isObject() && root.has("rows") && root.get("rows").isArray()) {
                    ObjectNode synthetic = objectMapper.createObjectNode();
                    synthetic.set("rows", root.get("rows"));
                    blocks.add(synthetic);
                }
            } catch (Exception ignored) {
            }
        }
        return blocks;
    }

    /**
     * Word 检测数据表单元格：无行→空白；末尾全「/」占位行→「/」；普通行缺字段→空白。
     */
    public static String tableDataCellDisplay(JsonNode rowData, String fieldKey) {
        if (rowData == null) {
            return "";
        }
        if (isTrailingSlashPlaceholderRow(rowData)) {
            return "/";
        }
        if (fieldKey == null || fieldKey.isEmpty() || !rowData.has(fieldKey) || rowData.get(fieldKey).isNull()) {
            return "";
        }
        JsonNode v = rowData.get(fieldKey);
        if (v.isNumber()) {
            return formatNumberForDisplay(v);
        }
        String t = v.asText().trim();
        return t.isEmpty() ? "" : t;
    }

    /** 按顺序尝试多个列名（如 UT 缺陷「位置」/「起点位置」）。 */
    public static String tableDataCellDisplayFirst(JsonNode rowData, String... fieldKeys) {
        if (rowData == null) {
            return "";
        }
        if (isTrailingSlashPlaceholderRow(rowData)) {
            return "/";
        }
        if (fieldKeys != null) {
            for (String key : fieldKeys) {
                if (key == null || key.isEmpty() || !rowData.has(key) || rowData.get(key).isNull()) {
                    continue;
                }
                JsonNode v = rowData.get(key);
                if (v.isNumber()) {
                    String num = formatNumberForDisplay(v);
                    if (!num.isEmpty()) {
                        return num;
                    }
                    continue;
                }
                String t = v.asText().trim();
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        return "";
    }

    private static String formatNumberForDisplay(JsonNode v) {
        if (v.isIntegralNumber()) {
            return String.valueOf(v.asLong());
        }
        double d = v.asDouble();
        if (d == Math.rint(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
