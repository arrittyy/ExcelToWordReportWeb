package com.reportweb.service.ndt;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NdtQualificationRegistry {

    private static final String LEVEL_II = "Ⅱ";
    private static final String LEVEL_III = "Ⅲ";

    private static final Map<String, String> METHOD_LABELS = Map.of(
            "MT", "磁粉检测",
            "PT", "渗透检测",
            "UT", "超声检测",
            "RT", "射线检测",
            "ET", "涡流检测"
    );

    private static final Map<String, Map<String, Set<String>>> QUALIFICATIONS = buildQualifications();

    public boolean isQualified(String methodCode, String levelRoman, String fullName) {
        String normalizedCode = normalizeMethodCode(methodCode);
        String normalizedLevel = normalizeLevel(levelRoman);
        String normalizedName = normalizeName(fullName);
        if (normalizedCode == null || normalizedLevel == null || normalizedName == null) {
            return false;
        }
        Map<String, Set<String>> byLevel = QUALIFICATIONS.get(normalizedCode);
        if (byLevel == null) {
            return false;
        }
        Set<String> names = byLevel.get(normalizedLevel);
        return names != null && names.contains(normalizedName);
    }

    /**
     * 按方法和姓名解析其最高资质级别（优先Ⅲ级，其次Ⅱ级；否则返回 null）。
     */
    public String resolveQualifiedLevel(String methodCode, String fullName) {
        String normalizedCode = normalizeMethodCode(methodCode);
        String normalizedName = normalizeName(fullName);
        if (normalizedCode == null || normalizedName == null) {
            return null;
        }
        Map<String, Set<String>> byLevel = QUALIFICATIONS.get(normalizedCode);
        if (byLevel == null) {
            return null;
        }
        Set<String> levelIIISet = byLevel.get(LEVEL_III);
        if (levelIIISet != null && levelIIISet.contains(normalizedName)) {
            return LEVEL_III;
        }
        Set<String> levelIISet = byLevel.get(LEVEL_II);
        if (levelIISet != null && levelIISet.contains(normalizedName)) {
            return LEVEL_II;
        }
        return null;
    }

    public boolean supportsMethod(String methodCode) {
        String normalizedCode = normalizeMethodCode(methodCode);
        return normalizedCode != null && QUALIFICATIONS.containsKey(normalizedCode);
    }

    public String normalizeMethodCode(String methodCode) {
        if (methodCode == null) {
            return null;
        }
        String code = methodCode.trim().toUpperCase(Locale.ROOT);
        if ("LP".equals(code)) {
            return "PT";
        }
        return code;
    }

    public String normalizeLevel(String levelRoman) {
        if (levelRoman == null) {
            return null;
        }
        String level = levelRoman.trim().replace("级", "");
        if ("II".equalsIgnoreCase(level) || "2".equals(level)) {
            return LEVEL_II;
        }
        if ("III".equalsIgnoreCase(level) || "3".equals(level)) {
            return LEVEL_III;
        }
        if (LEVEL_II.equals(level) || LEVEL_III.equals(level)) {
            return level;
        }
        return null;
    }

    public String methodLabel(String methodCode) {
        String normalizedCode = normalizeMethodCode(methodCode);
        if (normalizedCode == null) {
            return "未知方法";
        }
        return METHOD_LABELS.getOrDefault(normalizedCode, normalizedCode);
    }

    public Set<String> supportedLevels(String methodCode) {
        String normalizedCode = normalizeMethodCode(methodCode);
        if (normalizedCode == null) {
            return Collections.emptySet();
        }
        Map<String, Set<String>> byLevel = QUALIFICATIONS.get(normalizedCode);
        if (byLevel == null) {
            return Collections.emptySet();
        }
        return byLevel.keySet();
    }

    private String normalizeName(String fullName) {
        if (fullName == null) {
            return null;
        }
        String normalized = fullName.replace('\u3000', ' ').trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private static Map<String, Map<String, Set<String>>> buildQualifications() {
        Map<String, Map<String, Set<String>>> map = new HashMap<>();

        map.put("UT", levels(
                names("李世涛", "靳峰", "王志永"),
                names("牛保献", "李世铭", "周书康", "高建忠", "郭文", "侯家绪", "闫宁", "符勇", "魏烁",
                        "张书浩", "蒋豹", "张庆巍", "肖乐园", "张博炜", "宋可可", "卢申", "王佳朋", "孙赞")
        ));

        Map<String, Set<String>> mtLevels = levels(
                names("郭文", "王志永", "牛保献"),
                names("李世涛", "魏泉泉", "靳峰", "胡锋涛", "马东方", "李世铭", "周书康", "高建忠", "李艳军", "高秀娜",
                        "侯家绪", "徐亮", "符勇", "魏烁", "张书浩", "蒋豹", "杨希锐", "张庆巍", "马泽军", "肖乐园",
                        "张博炜", "宋可可", "卢申", "王佳朋", "孙赞", "王志明")
        );
        map.put("MT", mtLevels);
        map.put("PT", mtLevels);

        map.put("RT", levels(
                names("李世铭", "郭文", "王志永"),
                names("李世涛", "牛保献", "高建忠", "张庆巍", "马泽军", "肖乐园", "宋可可", "孙赞")
        ));

        map.put("ET", levelIIOnly(
                names("符勇", "魏烁", "王志永", "李世涛", "靳峰")
        ));

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Set<String>> levels(Set<String> levelIII, Set<String> levelII) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        map.put(LEVEL_III, levelIII);
        map.put(LEVEL_II, levelII);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Set<String>> levelIIOnly(Set<String> levelII) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        map.put(LEVEL_II, levelII);
        return Collections.unmodifiableMap(map);
    }

    private static Set<String> names(String... names) {
        Set<String> set = new LinkedHashSet<>();
        for (String name : names) {
            set.add(name);
        }
        return Collections.unmodifiableSet(set);
    }
}
