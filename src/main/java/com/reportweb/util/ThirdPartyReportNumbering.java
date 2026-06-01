package com.reportweb.util;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 第三方报告编号策略：华图按检测类型分别递增（如 HT2025-UT001），思维奇/自定义全局递增（如 HT2025-001）。
 */
public final class ThirdPartyReportNumbering {

    public static final String HUATU_POWER_NAME = "安徽华图电力科技有限公司";
    public static final String SIWEIQI_NAME = "天津市思维奇检测技术有限公司";

    /** 华图格式末尾：-{LETTERS}{3digits}，如 -UT001 */
    private static final Pattern HUATU_TRAILING = Pattern.compile("^(.*-[A-Za-z]+)(\\d{3})$");
    /** Legacy 格式末尾：-{digits}，如 -001 */
    private static final Pattern LEGACY_TRAILING = Pattern.compile("^(.*-)(\\d+)$");

    private ThirdPartyReportNumbering() {
    }

    /** 是否启用华图按检测类型编号（第三方名称精确匹配华图全称）。 */
    public static boolean usesPerTypeNumbering(String thirdPartyName) {
        if (thirdPartyName == null) {
            return false;
        }
        return HUATU_POWER_NAME.equals(thirdPartyName.trim());
    }

    /** 华图格式：base + "-" + typeCode + seq(3位)，如 HT2025-UT001 */
    public static String formatHuatuNumber(String base, String typeCode, int seq) {
        String effectiveBase = base != null ? base.trim() : ThirdPartyPlaceholders.NOT_ENTERED;
        String code = (typeCode != null && !typeCode.isBlank()) ? typeCode.trim() : "UNK";
        return effectiveBase + "-" + code + String.format("%03d", seq);
    }

    /** 思维奇/自定义：base + "-" + seq(3位) */
    public static String formatLegacyNumber(String base, int seq) {
        String effectiveBase = base != null ? base.trim() : ThirdPartyPlaceholders.NOT_ENTERED;
        return effectiveBase + "-" + String.format("%03d", seq);
    }

    /**
     * 解析报告编号末尾序号（兼容 legacy 与华图格式，供 UT 多行偏移使用）。
     * Legacy：HT2025-003 → 3；华图：HT2025-UT003 → 3。
     */
    public static OptionalInt parseTrailingSequence(String reportNumber) {
        if (reportNumber == null || reportNumber.isBlank()) {
            return OptionalInt.empty();
        }
        String trimmed = reportNumber.trim();
        Matcher huatu = HUATU_TRAILING.matcher(trimmed);
        if (huatu.matches()) {
            return OptionalInt.of(Integer.parseInt(huatu.group(2)));
        }
        Matcher legacy = LEGACY_TRAILING.matcher(trimmed);
        if (legacy.matches()) {
            return OptionalInt.of(Integer.parseInt(legacy.group(2)));
        }
        return OptionalInt.empty();
    }

    /** 在已有编号上叠加 row 偏移，保持前缀不变。 */
    public static String withTrailingSequenceOffset(String reportNumber, int offset) {
        if (reportNumber == null || reportNumber.isBlank() || offset <= 0) {
            return reportNumber;
        }
        String trimmed = reportNumber.trim();
        OptionalInt seqOpt = parseTrailingSequence(trimmed);
        if (seqOpt.isEmpty()) {
            return reportNumber;
        }
        int newSeq = seqOpt.getAsInt() + offset;
        Matcher huatu = HUATU_TRAILING.matcher(trimmed);
        if (huatu.matches()) {
            return huatu.group(1) + String.format("%03d", newSeq);
        }
        Matcher legacy = LEGACY_TRAILING.matcher(trimmed);
        if (legacy.matches()) {
            return legacy.group(1) + String.format("%03d", newSeq);
        }
        return reportNumber;
    }
}
