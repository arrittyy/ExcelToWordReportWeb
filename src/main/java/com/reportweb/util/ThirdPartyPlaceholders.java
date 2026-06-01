package com.reportweb.util;

/**
 * 第三方项目编号、第三方名称在未填写或仅空格时的占位，与创建/更新项目及 Word 展示保持一致。
 */
public final class ThirdPartyPlaceholders {

    public static final String NOT_ENTERED = "未输入";

    private ThirdPartyPlaceholders() {
    }

    /** null、空串或仅空格 → {@link #NOT_ENTERED}，否则返回 trim 后的原值 */
    public static String blankToDefault(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NOT_ENTERED;
        }
        return value.trim();
    }

    /** 用于报告编号前缀：库内为空时与占位一致，参与「未输入-001」拼号 */
    public static String effectiveThirdPartyProjectNumberBase(String stored) {
        if (stored == null) {
            return NOT_ENTERED;
        }
        String t = stored.trim();
        return t.isEmpty() ? NOT_ENTERED : t;
    }
}
