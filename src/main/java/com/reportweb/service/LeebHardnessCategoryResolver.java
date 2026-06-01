package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 里氏硬度表格行：管件 / 钢管 / 焊缝 分类。
 * 优先采用行内 {@link #FIELD_LEEB_CATEGORY}，否则按编号规则推断（弯头或 w/W → 管件；方位字 → 钢管；其余 → 焊缝）。
 */
public final class LeebHardnessCategoryResolver {

    private LeebHardnessCategoryResolver() {}

    /** 表格列名：用户确认后的类别，取值 {@link Category#getStoredLabel()} */
    public static final String FIELD_LEEB_CATEGORY = "里氏分类";

    public enum Category {
        PIPE_FITTING("管件", "管件硬度"),
        STEEL_PIPE("钢管", "钢管硬度"),
        WELD("焊缝", "焊缝硬度");

        private final String storedLabel;
        private final String itemName;

        Category(String storedLabel, String itemName) {
            this.storedLabel = storedLabel;
            this.itemName = itemName;
        }

        public String getStoredLabel() {
            return storedLabel;
        }

        public String getItemName() {
            return itemName;
        }
    }

    /** 仅根据编号推断（保存弹窗「建议分类」与后端比对兜底共用同一规则）。 */
    public static Category inferFromNumber(String number) {
        if (number == null || number.isEmpty()) {
            return Category.WELD;
        }
        if (number.contains("弯头") || number.contains("w") || number.contains("W")) {
            return Category.PIPE_FITTING;
        }
        if (number.matches(".*[上下左右前后].*")) {
            return Category.STEEL_PIPE;
        }
        return Category.WELD;
    }

    /** 先读行内「里氏分类」，合法则采用；否则 {@link #inferFromNumber(String)}。 */
    public static Category resolveCategory(JsonNode row, String numberField) {
        if (row == null) {
            return Category.WELD;
        }
        String stored = textField(row, FIELD_LEEB_CATEGORY);
        if ("管件".equals(stored)) {
            return Category.PIPE_FITTING;
        }
        if ("钢管".equals(stored)) {
            return Category.STEEL_PIPE;
        }
        if ("焊缝".equals(stored)) {
            return Category.WELD;
        }
        return inferFromNumber(textField(row, numberField));
    }

    private static String textField(JsonNode row, String field) {
        if (!row.has(field)) {
            return null;
        }
        JsonNode n = row.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            String s = n.asText();
            return s != null ? s.trim() : null;
        }
        if (n.isNumber()) {
            return n.asText();
        }
        return n.asText("").trim();
    }
}
