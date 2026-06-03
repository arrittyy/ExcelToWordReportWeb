package com.reportweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 超声波测厚（UTM/UTT）：检测内容各行「最小需要厚度」与同下标 perContentRow 测点实测厚度比较。
 * 未填或 ≤0 的最小需要厚度不参与壁厚判定，Word 自动结论视为合格（{@link #CONCLUSION_OK}）。
 */
public final class UltrasonicThicknessMinRequiredRules {

    /** 检测内容 JSON 根字段（与前端一致） */
    public static final String DETECTION_CONTENT_MIN_REQUIRED_KEY = "minRequiredThickness";

    private static final Pattern FIRST_INTEGER = Pattern.compile("(\\d+)");

    /** 合格自动结论（与全部测点满足最小需要厚度时一致） */
    public static final String CONCLUSION_OK =
            "在上述检测条件下，未见异常。";
    private static final String CONCLUSION_FAIL_PREFIX =
            "在上述检测条件下，";
    private static final String CONCLUSION_FAIL_SUFFIX = "壁厚不满足DL438-2016的要求。";

    private UltrasonicThicknessMinRequiredRules() {
    }

    public record EvaluationResult(
            List<String> failedPointNumbers,
            boolean hasEvaluableRow) {
    }

    public static boolean isBelowMinRequired(double measured, double minRequired) {
        return measured < minRequired;
    }

    public static String parsePointNumber(JsonNode row) {
        if (row == null) {
            return "";
        }
        for (String key : new String[]{"测点编号", "编号"}) {
            if (!row.has(key)) {
                continue;
            }
            JsonNode n = row.get(key);
            if (n == null || n.isNull()) {
                continue;
            }
            String s = n.isNumber() ? n.asText() : n.asText().trim();
            if (!s.isEmpty() && !"/".equals(s)) {
                return s;
            }
        }
        return "";
    }

    /**
     * 从检测内容第 0 行读取最小需要厚度（兼容仅顶层字段的旧数据）。
     */
    public static Double parseMinRequiredFromDetectionContent(Object detectionContent, ObjectMapper mapper) {
        return parseMinRequiredFromDetectionContent(detectionContent, mapper, 0);
    }

    /**
     * 从检测内容指定行读取最小需要厚度：优先 {@code rows[contentRowIndex].minRequiredThickness}，
     * 否则在 index==0 时回退顶层字段（旧数据）。
     */
    public static Double parseMinRequiredFromDetectionContent(
            Object detectionContent, ObjectMapper mapper, int contentRowIndex) {
        if (detectionContent == null || mapper == null) {
            return null;
        }
        try {
            JsonNode root = mapper.valueToTree(detectionContent);
            return parseMinRequiredFromDetectionContentRoot(root, contentRowIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static Double parseMinRequiredFromContentRow(JsonNode contentRow) {
        if (contentRow == null || !contentRow.isObject()) {
            return null;
        }
        if (!contentRow.has(DETECTION_CONTENT_MIN_REQUIRED_KEY)) {
            return null;
        }
        return parseNumericNode(contentRow.get(DETECTION_CONTENT_MIN_REQUIRED_KEY));
    }

    private static Double parseMinRequiredFromDetectionContentRoot(JsonNode root, int contentRowIndex) {
        if (root == null || !root.isObject()) {
            return null;
        }
        int idx = Math.max(0, contentRowIndex);
        if (root.has("rows") && root.get("rows").isArray()) {
            JsonNode rows = root.get("rows");
            if (idx < rows.size()) {
                Double perRow = parseMinRequiredFromContentRow(rows.get(idx));
                if (perRow != null) {
                    return perRow;
                }
            }
        }
        if (idx == 0 && root.has(DETECTION_CONTENT_MIN_REQUIRED_KEY)) {
            return parseNumericNode(root.get(DETECTION_CONTENT_MIN_REQUIRED_KEY));
        }
        return null;
    }

    public static Double parseMeasuredThickness(JsonNode row) {
        if (row == null) {
            return null;
        }
        String[] keys = {"实测厚度 (mm)", "实测厚度（mm）", "实测厚度", "厚度"};
        for (String key : keys) {
            Double v = parseNumericCell(row, key);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Double parseNumericCell(JsonNode row, String key) {
        if (!row.has(key)) {
            return null;
        }
        return parseNumericNode(row.get(key));
    }

    private static Double parseNumericNode(JsonNode t) {
        if (t == null || t.isNull()) {
            return null;
        }
        if (t.isNumber()) {
            return t.asDouble();
        }
        String text = t.asText().trim();
        if (text.isEmpty() || "/".equals(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @param minRequired 整份报告最小需要厚度；null 或 ≤0 时不判定
     */
    public static EvaluationResult evaluateRows(Iterable<JsonNode> rows, Double minRequired) {
        List<String> failed = new ArrayList<>();
        if (minRequired == null || minRequired <= 0 || rows == null) {
            return new EvaluationResult(failed, false);
        }
        boolean hasEvaluable = false;
        for (JsonNode row : rows) {
            if (row == null || TableDataMergeUtil.isTrailingSlashPlaceholderRow(row)) {
                continue;
            }
            String point = parsePointNumber(row);
            Double measured = parseMeasuredThickness(row);
            if (point.isEmpty() || measured == null || measured <= 0) {
                continue;
            }
            hasEvaluable = true;
            if (isBelowMinRequired(measured, minRequired)) {
                failed.add(point);
            }
        }
        return new EvaluationResult(failed, hasEvaluable);
    }

    /**
     * Word/预览用自动检测结论：未填最小需要厚度时视为合格；已填则按实测与最小厚度比较。
     */
    public static String resolveConclusionSentence(Iterable<JsonNode> rows, Double minRequired) {
        if (minRequired == null || minRequired <= 0) {
            return buildConclusionSentence(List.of());
        }
        EvaluationResult eval = evaluateRows(rows, minRequired);
        return buildConclusionSentence(eval.failedPointNumbers());
    }

    public static String buildConclusionSentence(List<String> failedPointNumbers) {
        if (failedPointNumbers == null || failedPointNumbers.isEmpty()) {
            return CONCLUSION_OK;
        }
        return CONCLUSION_FAIL_PREFIX + sortAndJoinPointNumbers(failedPointNumbers) + CONCLUSION_FAIL_SUFFIX;
    }

    public static String sortAndJoinPointNumbers(List<String> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return "";
        }
        return numbers.stream()
                .filter(s -> s != null)
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"/".equals(s))
                .distinct()
                .sorted(UltrasonicThicknessMinRequiredRules::comparePointNumberText)
                .collect(Collectors.joining("、"));
    }

    private static int comparePointNumberText(String left, String right) {
        Integer leftInt = extractFirstInteger(left);
        Integer rightInt = extractFirstInteger(right);
        if (leftInt != null && rightInt != null) {
            int compare = Integer.compare(leftInt, rightInt);
            if (compare != 0) {
                return compare;
            }
        } else if (leftInt != null) {
            return -1;
        } else if (rightInt != null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static Integer extractFirstInteger(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = FIRST_INTEGER.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static String formatMinRequiredForDisplay(double value) {
        return String.format(Locale.ROOT, "%.3f mm", value);
    }

    public static String formatMeasuredForDisplay(double value) {
        return String.format(Locale.ROOT, "%.3f mm", value);
    }
}
