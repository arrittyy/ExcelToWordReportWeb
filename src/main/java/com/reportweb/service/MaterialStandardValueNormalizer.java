package com.reportweb.service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 材质标准值字符串规范化：提交入库与比对前统一波浪号、比较符号等写法。
 */
public final class MaterialStandardValueNormalizer {

    private static final Set<String> SKIP_NORMALIZE_KEYS = Set.of("评定标准", "GB5310牌号", "国外牌号");

    /** 纯数值区间：如 197-241、0.40-0.70 */
    private static final Pattern NUMERIC_RANGE_ASCII_HYPHEN = Pattern.compile(
            "^\\s*([-+]?\\d+\\.?\\d*)\\s*-\\s*([-+]?\\d+\\.?\\d*)\\s*$");

    private MaterialStandardValueNormalizer() {
    }

    public static boolean shouldNormalizeKey(String propertyKey) {
        return propertyKey != null && !SKIP_NORMALIZE_KEYS.contains(propertyKey.trim());
    }

    /**
     * 将用户常见不规范写法转为系统推荐格式（全角波浪号 ～、≥、≤）。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }

        if (t.regionMatches(true, 0, ">=", 0, 2)) {
            t = "≥" + t.substring(2).trim();
        } else if (t.regionMatches(true, 0, "<=", 0, 2)) {
            t = "≤" + t.substring(2).trim();
        }

        t = t.replace('~', '～')
                .replace('〜', '～')
                .replace('—', '～')
                .replace('–', '～');

        Matcher rangeMatcher = NUMERIC_RANGE_ASCII_HYPHEN.matcher(t);
        if (rangeMatcher.matches()) {
            t = rangeMatcher.group(1).trim() + "～" + rangeMatcher.group(2).trim();
        }

        return t.trim();
    }
}
