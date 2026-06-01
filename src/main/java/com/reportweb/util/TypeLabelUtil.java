package com.reportweb.util;

import java.util.regex.Pattern;

/**
 * 检测内容「类型」用于叙述拼接时的展示处理（入库与模板分支仍用原值）。
 */
public final class TypeLabelUtil {

    private static final Pattern FULLWIDTH_PAREN = Pattern.compile("（[^）]*）");
    private static final Pattern HALFWIDTH_PAREN = Pattern.compile("\\([^)]*\\)");

    private TypeLabelUtil() {
    }

    /**
     * 去掉全角、半角括号及其中的内容，供方法+部件+类型等叙述拼接使用。
     */
    public static String stripParentheticalQualifiers(String type) {
        if (type == null) {
            return "";
        }
        String t = FULLWIDTH_PAREN.matcher(type).replaceAll("");
        t = HALFWIDTH_PAREN.matcher(t).replaceAll("");
        return t.trim();
    }

    /** 超声等模板分支：新值「弯头」与历史入库值「弯头/弯管」「弯头弯管」等价。 */
    public static boolean isElbowPipeDetectionType(String type) {
        if (type == null) {
            return false;
        }
        String t = type.trim();
        return "弯头".equals(t) || "弯头/弯管".equals(t) || "弯头弯管".equals(t);
    }
}
