package com.reportweb.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 管径测量（PDM）外径蠕变应变：按检测内容「类型」下拉取允许应变比例阈值（与名义外径比较，不取绝对值）。
 */
public final class PdmPipeCreepRules {

    private PdmPipeCreepRules() {
    }

    /**
     * @return 小数形式阈值，如 0.025 表示 2.5%；未知类型返回 null
     */
    public static Double ratioForPipeType(String pipeType) {
        if (pipeType == null) {
            return null;
        }
        String t = pipeType.trim();
        switch (t) {
            case "低合金钢管":
                return 0.025;
            case "碳素钢管":
                return 0.035;
            case "T91/T122类管":
                return 0.012;
            case "奥氏体耐热钢管":
                return 0.045;
            default:
                return null;
        }
    }

    /**
     * 将比例（如 0.025）格式化为展示用百分数字符串（如 "2.5"），去掉无意义的小数位。
     */
    public static String formatPercentLabel(double ratio) {
        BigDecimal pct = BigDecimal.valueOf(ratio).multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
        return pct.scale() < 0 ? pct.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : pct.toPlainString();
    }

    /**
     * 全部测点合格时的结论文案；类型无法映射时返回 null。
     */
    public static String normalConclusionSentence(String pipeType) {
        Double r = ratioForPipeType(pipeType);
        if (r == null) {
            return null;
        }
        String pct = formatPercentLabel(r);
        String typeLabel = pipeType != null ? pipeType.trim() : "";
        if (typeLabel.isEmpty()) {
            return null;
        }
        return "检测结果均符合要求（" + typeLabel + "外径蠕变应变不大于" + pct + "%）。";
    }
}
