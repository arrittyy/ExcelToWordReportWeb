package com.reportweb.util;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 检测内容「类型」用于叙述拼接时的展示处理（入库与模板分支仍用原值）。
 */
public final class TypeLabelUtil {

    private static final Pattern FULLWIDTH_PAREN = Pattern.compile("（[^）]*）");
    private static final Pattern HALFWIDTH_PAREN = Pattern.compile("\\([^)]*\\)");
    private static final String PAUT_ROOT_PREFIX = "叶根";
    private static final String PAUT_PROBE_FORK_MUSHROOM = "1号探头(7.5MHz 阵元数16 晶片0.5x10mm)";
    private static final String PAUT_PROBE_T_FIR = "1号探头(5MHz 阵元数10 晶片0.5x0.5mm)";

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

    /**
     * 叙述用类型：去括注后，再将「叶根-叉形」等 PAUT 细分归并为「叶根」。
     */
    public static String effectiveTypeForNarrative(String type) {
        return stripPautRootDashSubtype(stripParentheticalQualifiers(type));
    }

    /**
     * 「叶根-叉形」→ Optional「叉形」；旧括号合并类型及非叶根- 前缀返回 empty。
     */
    public static Optional<String> pautRootShapeFromType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        String t = type.trim();
        if (!t.startsWith(PAUT_ROOT_PREFIX)) {
            return Optional.empty();
        }
        if (t.length() <= PAUT_ROOT_PREFIX.length()) {
            return Optional.empty();
        }
        char sep = t.charAt(PAUT_ROOT_PREFIX.length());
        if (sep != '-' && sep != '－') {
            return Optional.empty();
        }
        String shape = t.substring(PAUT_ROOT_PREFIX.length() + 1).trim();
        return shape.isEmpty() ? Optional.empty() : Optional.of(shape);
    }

    public static boolean isPautRootForkOrMushroomShape(String shape) {
        return "叉形".equals(shape) || "菌形".equals(shape);
    }

    public static boolean isPautRootTOrFirShape(String shape) {
        return "T形".equals(shape) || "枞树形".equals(shape);
    }

    /** 新 PAUT 叶根细分类型的 1 号探头固定文案；未知形制返回 empty。 */
    public static Optional<String> pautBoltProbeParamForRootShape(String shape) {
        if (shape == null || shape.isBlank()) {
            return Optional.empty();
        }
        String s = shape.trim();
        if (isPautRootForkOrMushroomShape(s)) {
            return Optional.of(PAUT_PROBE_FORK_MUSHROOM);
        }
        if (isPautRootTOrFirShape(s)) {
            return Optional.of(PAUT_PROBE_T_FIR);
        }
        return Optional.empty();
    }

    public static boolean isPautRootDashSubtypeType(String type) {
        return pautRootShapeFromType(type).isPresent();
    }

    private static String stripPautRootDashSubtype(String type) {
        if (type == null || type.isEmpty()) {
            return type != null ? type : "";
        }
        String t = type.trim();
        if (t.startsWith(PAUT_ROOT_PREFIX) && t.length() > PAUT_ROOT_PREFIX.length()) {
            char sep = t.charAt(PAUT_ROOT_PREFIX.length());
            if (sep == '-' || sep == '－') {
                return PAUT_ROOT_PREFIX;
            }
        }
        return t;
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
