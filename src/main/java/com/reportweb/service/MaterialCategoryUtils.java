package com.reportweb.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 材质库按检测应用场景分类的字段定义。
 */
public final class MaterialCategoryUtils {

    public static final String CATEGORY_ALLOY = "alloy";
    public static final String CATEGORY_LEEB = "leeb";
    public static final String CATEGORY_BOLT = "bolt";
    public static final String CATEGORY_MECHANICAL = "mechanical";
    public static final String CATEGORY_HARDNESS = "hardness";

    public static final List<String> META_KEYS = List.of("GB5310牌号", "国外牌号");

    /** 用户不可编辑、更新时需保留的系统字段 */
    public static final List<String> PRESERVE_ON_USER_UPDATE_KEYS = List.of("评定标准");

    private static final Map<String, List<String>> CATEGORY_FIELDS = new LinkedHashMap<>();

    static {
        CATEGORY_FIELDS.put(CATEGORY_ALLOY, List.of(
                "Mn", "Cr", "Mo", "V", "Ti", "Ni", "Al", "Cu", "Nb", "W", "Co", "Mg", "Zr"));
        CATEGORY_FIELDS.put(CATEGORY_LEEB, List.of("里氏-管件", "里氏-钢管", "里氏-焊缝", "里氏"));
        CATEGORY_FIELDS.put(CATEGORY_BOLT, List.of("里氏-螺栓"));
        CATEGORY_FIELDS.put(CATEGORY_MECHANICAL, List.of("抗拉强度", "下屈服强度", "断后伸长率"));
        CATEGORY_FIELDS.put(CATEGORY_HARDNESS, List.of("布氏", "维氏", "洛氏"));
    }

    private MaterialCategoryUtils() {
    }

    public static Set<String> allCategories() {
        return CATEGORY_FIELDS.keySet();
    }

    public static List<String> fieldsForCategory(String category) {
        return CATEGORY_FIELDS.getOrDefault(category, List.of());
    }

    public static boolean isValidCategory(String category) {
        return category != null && CATEGORY_FIELDS.containsKey(category);
    }

    public static boolean matchesCategory(String category, Map<String, String> properties) {
        if (properties == null || !isValidCategory(category)) {
            return false;
        }
        for (String field : fieldsForCategory(category)) {
            String value = properties.get(field);
            if (value != null && !value.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesKeyword(String keyword, String materialKey, Map<String, String> properties) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        if (materialKey != null && materialKey.toLowerCase().contains(kw)) {
            return true;
        }
        if (properties == null) {
            return false;
        }
        for (Map.Entry<String, String> e : properties.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase().contains(kw)) {
                return true;
            }
            if (e.getValue() != null && e.getValue().toLowerCase().contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldNormalizeStandardValue(String propertyKey) {
        return MaterialStandardValueNormalizer.shouldNormalizeKey(propertyKey);
    }

    public static String inferPrimaryCategory(Map<String, String> properties) {
        if (properties == null) {
            return CATEGORY_ALLOY;
        }
        if (matchesCategory(CATEGORY_BOLT, properties) && !matchesCategory(CATEGORY_LEEB, properties)) {
            return CATEGORY_BOLT;
        }
        if (matchesCategory(CATEGORY_LEEB, properties)) {
            return CATEGORY_LEEB;
        }
        if (matchesCategory(CATEGORY_ALLOY, properties)) {
            return CATEGORY_ALLOY;
        }
        if (matchesCategory(CATEGORY_MECHANICAL, properties)) {
            return CATEGORY_MECHANICAL;
        }
        if (matchesCategory(CATEGORY_HARDNESS, properties)) {
            return CATEGORY_HARDNESS;
        }
        return CATEGORY_ALLOY;
    }

    public static Map<String, String> copyProperties(Map<String, String> source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(source);
    }

    public static Map<String, String> sanitizeProperties(Map<String, String> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null) {
            return result;
        }
        for (Map.Entry<String, String> e : raw.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String key = e.getKey().trim();
            if (key.isEmpty()) {
                continue;
            }
            String value = e.getValue() == null ? "" : e.getValue().trim();
            if (!value.isEmpty()) {
                if (shouldNormalizeStandardValue(key)) {
                    value = MaterialStandardValueNormalizer.normalize(value);
                }
                result.put(key, value);
            }
        }
        return result;
    }
}
