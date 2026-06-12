package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.util.ExportTextOverrides;
import com.reportweb.util.TypeLabelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 检测内容叙述：与单项 Word 中「检测内容」段落规则一致，供概述、API、日志等复用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionContentNarrativeService {

    private final ObjectMapper objectMapper;

    private static boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty() && !"/".equals(s.trim());
    }

    /** Word 大格标签；导出覆盖 {@link ExportTextOverrides#DETECTION_NARRATIVE_BODY} 仅存正文，勿含格标题。 */
    private static final Pattern DETECTION_LOCATION_CELL_LABEL_PREFIX =
            Pattern.compile("^检测部位\\s*[:：]\\s*(?:\\r?\\n\\s*)?");
    private static final Pattern DETECTION_CONTENT_CELL_LABEL_PREFIX =
            Pattern.compile("^检测内容\\s*[:：]\\s*(?:\\r?\\n\\s*)?");
    private static final Pattern DETECTION_POSITION_CELL_LABEL_PREFIX =
            Pattern.compile("^检测位置\\s*[:：]\\s*(?:\\r?\\n\\s*)?");
    private static final Pattern INSPECTION_CONTENT_CELL_LABEL_PREFIX =
            Pattern.compile("^检查内容\\s*(?:\\r?\\n\\s*)?");

    private static final Pattern[] WORD_DETECTION_CELL_LABEL_PREFIXES = {
            DETECTION_LOCATION_CELL_LABEL_PREFIX,
            DETECTION_CONTENT_CELL_LABEL_PREFIX,
            DETECTION_POSITION_CELL_LABEL_PREFIX,
            INSPECTION_CONTENT_CELL_LABEL_PREFIX,
    };

    /** 单项 Word / 概述：由渲染层按需追加，导出覆盖正文不应包含该短语。 */
    public static final String FIGURE_REFERENCE_PHRASE = "详见附图";

    public static boolean containsFigureReference(String text) {
        return text != null && text.contains(FIGURE_REFERENCE_PHRASE);
    }

    /**
     * 去掉正文中的「详见附图」及紧邻句号；概述「3 工作内容」覆盖与检测叙述覆盖共用。
     */
    public static String stripFigureReferencePhrases(String line) {
        if (line == null) {
            return null;
        }
        String figSentence = FIGURE_REFERENCE_PHRASE + "。";
        String s = line;
        while (s.contains(figSentence)) {
            s = s.replace(figSentence, "");
        }
        s = s.replace(FIGURE_REFERENCE_PHRASE, "");
        while (s.contains("。。")) {
            s = s.replace("。。", "。");
        }
        return s.trim();
    }

    /**
     * 有附图且正文尚未含「详见附图」时，在格末追加一次（与单项 Word 版式一致）。
     */
    public static String appendFigureSuffixIfAbsent(String cellText, boolean hasImageAttachments) {
        String base = cellText != null ? cellText : "";
        if (hasImageAttachments && !containsFigureReference(base)) {
            return base + "\n  " + FIGURE_REFERENCE_PHRASE;
        }
        return base;
    }

    /**
     * 去掉导出文字或历史数据中误带的「检测部位:」格标题（Word 生成时会再拼一次）。
     */
    public String stripDetectionLocationLabelPrefix(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return DETECTION_LOCATION_CELL_LABEL_PREFIX.matcher(text.trim()).replaceFirst("").trim();
    }

    /**
     * 从单项 Word 大格整段文本中提取叙述正文（去掉检测部位/检测内容/检测位置等格标题，可重复剥离以兼容历史双标题）。
     */
    public String extractNarrativeBodyFromWordDetectionCell(String cellText) {
        if (cellText == null || cellText.isEmpty()) {
            return "";
        }
        String t = cellText.trim();
        boolean changed;
        do {
            changed = false;
            for (Pattern prefix : WORD_DETECTION_CELL_LABEL_PREFIXES) {
                String next = prefix.matcher(t).replaceFirst("").trim();
                if (!next.equals(t)) {
                    t = next;
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return t;
    }

    /** 规范化 {@link ExportTextOverrides#DETECTION_NARRATIVE_BODY}：仅保留 Word 大格内正文（不含详见附图）。 */
    public String normalizeExportDetectionNarrativeBody(String text) {
        return stripFigureReferencePhrases(extractNarrativeBodyFromWordDetectionCell(text));
    }

    private String normalizeDetectionNarrativeOverride(String override) {
        return normalizeExportDetectionNarrativeBody(override);
    }

    private static boolean isBlankOrSlash(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        return t.isEmpty() || "/".equals(t) || "／".equals(t);
    }

    private String dualTextareaPositionText(JsonNode node) {
        if (node == null || !node.has("position") || node.get("position").isNull()) {
            return "";
        }
        String position = node.get("position").asText("");
        if (isBlankOrSlash(position)) {
            return "";
        }
        return extractNarrativeBodyFromWordDetectionCell(position);
    }

    /** 用户填入的「总计 / 位置描述 / 编号」末尾常见标点，避免与系统拼接的逗号、句号重复。 */
    private static final String FIELD_TRAILING_PUNCT = "。．，、；：,.;:!?！?";

    private static String stripTrailingFieldPunctuation(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        while (!t.isEmpty()) {
            int last = t.length() - 1;
            char c = t.charAt(last);
            if (FIELD_TRAILING_PUNCT.indexOf(c) >= 0) {
                t = t.substring(0, last).trim();
            } else {
                break;
            }
        }
        return t;
    }

    /**
     * 里氏硬度检测上下文：LHT/LHD 或名称/方法中含「里氏」。
     */
    private static boolean isLeebHardnessContext(ExperimentType experimentType,
                                                   String experimentTypeNameFallback,
                                                   String experimentTypeCodeFallback,
                                                   String methodName) {
        if (experimentType != null) {
            String code = experimentType.getCode();
            if (code != null && ("LHT".equalsIgnoreCase(code.trim()) || "LHD".equalsIgnoreCase(code.trim()))) {
                return true;
            }
            String name = experimentType.getName();
            if (name != null && name.contains("里氏")) {
                return true;
            }
        }
        if (experimentTypeCodeFallback != null
                && ("LHT".equalsIgnoreCase(experimentTypeCodeFallback.trim())
                || "LHD".equalsIgnoreCase(experimentTypeCodeFallback.trim()))) {
            return true;
        }
        if (experimentTypeNameFallback != null && experimentTypeNameFallback.contains("里氏")) {
            return true;
        }
        return methodName != null && methodName.contains("里氏");
    }

    /**
     * 叙述前缀中的类型：先去掉全角/半角括注；里氏螺栓类再按部件名去重，避免「螺母螺栓」等赘连。
     */
    private static String effectiveTypeForNarrativePrefix(ExperimentType experimentType,
                                                          String experimentTypeNameFallback,
                                                          String experimentTypeCodeFallback,
                                                          String methodName,
                                                          String componentName,
                                                          String type) {
        if (type == null || !hasValue(type)) {
            return "";
        }
        String stripped = TypeLabelUtil.effectiveTypeForNarrative(type);
        if (!hasValue(stripped)) {
            return "";
        }
        if (!stripped.contains("螺栓")) {
            return stripped;
        }
        if (!isLeebHardnessContext(experimentType, experimentTypeNameFallback, experimentTypeCodeFallback, methodName)) {
            return stripped;
        }
        String comp = componentName != null ? componentName.trim() : "";
        if ("螺栓".equals(stripped)) {
            if (comp.contains("螺栓")) {
                return "";
            }
            if ((comp.contains("螺母") || comp.contains("螺帽")) && !comp.contains("螺栓")) {
                return "";
            }
        }
        return stripped;
    }

    /**
     * 从 Report 获取字段值：实体字段优先，否则 customFields（与 WordGeneratorServiceImpl 一致）。
     */
    public String getFieldValue(Object entity, String fieldName, String defaultValue) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value != null && !value.toString().isEmpty()) {
                return value.toString();
            }
            if (entity instanceof Report report) {
                if (report.getCustomFields() != null && report.getCustomFields().containsKey(fieldName)) {
                    Object customValue = report.getCustomFields().get(fieldName);
                    if (customValue != null && !customValue.toString().isEmpty()) {
                        return customValue.toString();
                    }
                }
            }
            return defaultValue;
        } catch (NoSuchFieldException e) {
            if (entity instanceof Report report) {
                if (report.getCustomFields() != null && report.getCustomFields().containsKey(fieldName)) {
                    Object customValue = report.getCustomFields().get(fieldName);
                    if (customValue != null && !customValue.toString().isEmpty()) {
                        return customValue.toString();
                    }
                }
            }
            return defaultValue;
        } catch (Exception e) {
            log.warn("Error getting field value for {}: {}", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 按字段有无条件拼接检测内容/检测部位详细描述（与单项 Word 一致）。
     * 等价于 {@link #buildDetectionDetailText(String, String, String, String, String, String, boolean, boolean, ExperimentType, String, String)} 且
     * 方法与部件均进入前缀，无检测类型上下文回退。
     */
    public String buildDetectionDetailText(String methodName, String componentName, String type,
                                             String total, String locationDesc, String locationNumber) {
        return buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                true, true, null, null, null);
    }

    /**
     * @param includeMethodInPrefix {@code true} 时前缀含检测方法；多行叙述除首行外为 {@code false}。
     * @param includeComponentInPrefix {@code true} 时前缀含部件名称；多行叙述除首行外为 {@code false}（后续行仅类型起句）。
     */
    public String buildDetectionDetailText(String methodName, String componentName, String type,
                                             String total, String locationDesc, String locationNumber,
                                             boolean includeMethodInPrefix, boolean includeComponentInPrefix) {
        return buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                includeMethodInPrefix, includeComponentInPrefix, null, null, null);
    }

    /**
     * 与 {@link #buildDetectionDetailText(String, String, String, String, String, String, boolean, boolean)} 等价，
     * {@code includeComponentInPrefix} 与 {@code includeMethodInPrefix} 相同。
     */
    public String buildDetectionDetailText(String methodName, String componentName, String type,
                                             String total, String locationDesc, String locationNumber,
                                             boolean includeMethodInPrefix) {
        return buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                includeMethodInPrefix, includeMethodInPrefix, null, null, null);
    }

    /**
     * 总览 Word 等无 {@link ExperimentType} 实体时的回退：用检测类型名称/编码判定里氏，以对螺栓类型行省略前缀中的 type。
     */
    public String buildDetectionDetailText(String methodName, String componentName, String type,
                                           String total, String locationDesc, String locationNumber,
                                           boolean includeMethodInPrefix,
                                           String experimentTypeNameFallback, String experimentTypeCodeFallback) {
        return buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                includeMethodInPrefix, includeMethodInPrefix, null, experimentTypeNameFallback, experimentTypeCodeFallback);
    }

    /**
     * 总览 Word 等无 {@link ExperimentType} 实体时的回退（可分别控制方法与部件是否进入前缀）。
     */
    public String buildDetectionDetailText(String methodName, String componentName, String type,
                                           String total, String locationDesc, String locationNumber,
                                           boolean includeMethodInPrefix, boolean includeComponentInPrefix,
                                           String experimentTypeNameFallback, String experimentTypeCodeFallback) {
        return buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                includeMethodInPrefix, includeComponentInPrefix, null, experimentTypeNameFallback, experimentTypeCodeFallback);
    }

    /**
     * @param experimentTypeNameFallback 无实体时的检测类型名称（如总览 {@code ReportInfo.experimentTypeName}）
     * @param experimentTypeCodeFallback 无实体时的检测类型编码（如 {@code LHD}）
     */
    public String buildDetectionDetailText(String methodName, String componentName, String type,
                                             String total, String locationDesc, String locationNumber,
                                             boolean includeMethodInPrefix, boolean includeComponentInPrefix,
                                             ExperimentType experimentType,
                                             String experimentTypeNameFallback,
                                             String experimentTypeCodeFallback) {
        total = stripTrailingFieldPunctuation(total);
        locationDesc = stripTrailingFieldPunctuation(locationDesc);
        locationNumber = stripTrailingFieldPunctuation(locationNumber);
        String typeForPrefix = effectiveTypeForNarrativePrefix(experimentType, experimentTypeNameFallback,
                experimentTypeCodeFallback, methodName, componentName, type);

        List<String> segments = new ArrayList<>();
        String prefix = "";
        if (includeMethodInPrefix && hasValue(methodName)) {
            prefix = methodName;
        }
        if (includeComponentInPrefix && hasValue(componentName)) {
            prefix = prefix + componentName;
        }
        if (hasValue(typeForPrefix)) {
            prefix = prefix + typeForPrefix;
        }
        if (!prefix.isEmpty()) {
            segments.add(prefix);
        }
        if (hasValue(total)) {
            segments.add("总计数量为" + total);
        }
        if (hasValue(locationDesc) && hasValue(componentName)) {
            segments.add("具体位置：" + componentName + locationDesc);
        } else if (hasValue(locationDesc)) {
            segments.add("具体位置：" + locationDesc);
        }
        if (hasValue(locationNumber)) {
            segments.add("编号：" + locationNumber);
        }
        if (segments.isEmpty()) {
            return "";
        }
        return String.join("，", segments) + "。";
    }

    /**
     * 优先使用 {@code custom_fields._exportTextOverrides.detectionNarrativeBody}，否则与 {@link #buildDetectionContentNarrativeBody} 一致。
     */
    public String getEffectiveNarrativeBody(Report report, ExperimentType experimentType) {
        String override = ExportTextOverrides.detectionNarrativeBody(report);
        if (!override.isEmpty()) {
            return normalizeDetectionNarrativeOverride(override);
        }
        return buildDetectionContentNarrativeBody(report, experimentType);
    }

    /**
     * 单行检测部位叙述：有导出文字覆盖时返回整段覆盖，否则 {@link #buildDetectionDetailText}。
     */
    public String getEffectiveDetailText(Report report, ExperimentType experimentType,
                                       String methodName, String componentName, String type,
                                       String total, String locationDesc, String locationNumber) {
        String override = ExportTextOverrides.detectionNarrativeBody(report);
        if (!override.isEmpty()) {
            return normalizeDetectionNarrativeOverride(override);
        }
        return buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                true, true, experimentType, null, null);
    }

    /**
     * Word「检测部位」长叙述：{@code detectionContent.rows} 多于一行时用与磁粉一致的合并叙述；
     * 单行时沿用调用方解析的首行字段（保留硬度/冲击等将 {@code total} 设为测量表行数等特例）。
     */
    public String getEffectiveWordDetectionLocationNarrative(Report report, ExperimentType experimentType,
            String methodName, String componentName, String type,
            String total, String locationDesc, String locationNumber) {
        if (countDetectionContentTableRows(report) > 1) {
            return getEffectiveNarrativeBody(report, experimentType);
        }
        return getEffectiveDetailText(report, experimentType, methodName, componentName, type, total, locationDesc,
                locationNumber);
    }

    /**
     * 从 Report 解析检测内容首行等字段（首行用于回退与里氏分支等）。
     */
    public DetectionContentDetail parseDetectionContentDetail(Report report, ExperimentType experimentType) {
        return parseDetectionContentDetailAtRow(report, experimentType, 0);
    }

    /**
     * 按 detectionContent.rows 下标解析单行（超声等多模板按行分发）；下标越界时钳到末行。
     */
    public DetectionContentDetail parseDetectionContentDetailAtRow(Report report, ExperimentType experimentType, int rowIndex) {
        String methodName = report.getTestMethod();
        if (methodName == null || methodName.isEmpty()) {
            methodName = (experimentType != null && experimentType.getName() != null) ? experimentType.getName() : "";
        }
        String componentName = getFieldValue(report, "componentName", "");
        if ("/".equals(componentName)) {
            componentName = "";
        }
        String type = "";
        String total = "";
        String locationDesc = "";
        String locationNumber = "";
        if (report.getDetectionContent() != null) {
            try {
                String json = objectMapper.writeValueAsString(report.getDetectionContent());
                JsonNode node = objectMapper.readTree(json);
                if (node.has("mode") && "visual-groups".equals(node.get("mode").asText())) {
                    String methodNameVis = (experimentType != null && experimentType.getName() != null)
                            ? experimentType.getName() : "目视检测";
                    return new DetectionContentDetail(methodNameVis, componentName, type, total, locationDesc, locationNumber);
                }
                if (node.has("mode") && "dual-textarea".equals(node.get("mode").asText())) {
                    String position = dualTextareaPositionText(node);
                    locationDesc = position;
                    return new DetectionContentDetail(methodName, componentName, type, total, locationDesc, locationNumber);
                }
                if (node.has("rows") && node.get("rows").isArray() && node.get("rows").size() > 0) {
                    JsonNode rows = node.get("rows");
                    int idx = Math.min(Math.max(0, rowIndex), rows.size() - 1);
                    JsonNode row = rows.get(idx);
                    if (row.has("type")) {
                        type = row.get("type").asText();
                    }
                    if (row.has("total")) {
                        total = row.get("total").asText();
                    }
                    if (row.has("locationDesc")) {
                        locationDesc = row.get("locationDesc").asText();
                    }
                    if (row.has("locationNumber")) {
                        locationNumber = row.get("locationNumber").asText();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse detectionContent for report {}: {}", report.getId(), e.getMessage());
            }
        }
        if (experimentType != null && experimentType.getCode() != null) {
            String code = experimentType.getCode().trim();
            if (("LHD".equals(code) || "LHT".equals(code)) && LeebHardnessModeResolver.isBoltOrNutTypeText(type)) {
                if (locationDesc == null || locationDesc.trim().isEmpty()) {
                    String endWaist = LeebHardnessModeResolver.extractBoltLeebEndWaist(type);
                    if (!endWaist.isEmpty()) {
                        locationDesc = endWaist;
                    }
                }
            }
        }
        return new DetectionContentDetail(methodName, componentName, type, total, locationDesc, locationNumber);
    }

    /**
     * 检测内容 table 模式下的数据行数（目视分组等非 table 返回 1）。
     */
    public int countDetectionContentTableRows(Report report) {
        if (report.getDetectionContent() == null) {
            return 1;
        }
        try {
            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(report.getDetectionContent()));
            if (node.has("mode") && "visual-groups".equals(node.get("mode").asText())) {
                return 1;
            }
            if (node.has("rows") && node.get("rows").isArray()) {
                int n = node.get("rows").size();
                return n <= 0 ? 1 : n;
            }
        } catch (Exception e) {
            log.warn("countDetectionContentTableRows failed for report {}: {}", report.getId(), e.getMessage());
        }
        return 1;
    }

    /** 仅叙述单行（与 buildDetectionDetailText 规则一致）。 */
    public String buildDetectionContentNarrativeSingleRow(Report report, ExperimentType experimentType, int rowIndex) {
        String override = ExportTextOverrides.detectionNarrativeBody(report, rowIndex);
        if (!override.isEmpty()) {
            return normalizeDetectionNarrativeOverride(override);
        }
        DetectionContentDetail d = parseDetectionContentDetailAtRow(report, experimentType, rowIndex);
        return buildDetectionDetailText(d.methodName, d.componentName, d.type, d.total, d.locationDesc, d.locationNumber,
                true, true, experimentType, null, null);
    }

    /**
     * table 多行时：首行含检测方法+部件+类型，后续行仅类型起句，段间全角分号连接为一段话；单条或非 table 与首行规则一致。
     */
    public String buildDetectionContentNarrativeBody(Report report, ExperimentType experimentType) {
        DetectionContentDetail fallback = parseDetectionContentDetail(report, experimentType);
        String single = buildDetectionDetailText(fallback.methodName, fallback.componentName, fallback.type,
                fallback.total, fallback.locationDesc, fallback.locationNumber, true, true, experimentType, null, null);
        if (report.getDetectionContent() == null) {
            return single;
        }
        try {
            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(report.getDetectionContent()));
            if (node.has("mode") && "dual-textarea".equals(node.get("mode").asText())) {
                // dual-textarea 仍走统一叙述拼接，保证“检测部件”改动能反映到正文
                return single;
            }
            if (node.has("mode") && "visual-groups".equals(node.get("mode").asText())) {
                return single;
            }
            if (!node.has("rows") || !node.get("rows").isArray() || node.get("rows").size() <= 1) {
                return single;
            }
            String methodName = report.getTestMethod();
            if (methodName == null || methodName.isEmpty()) {
                methodName = (experimentType != null && experimentType.getName() != null) ? experimentType.getName() : "";
            }
            String componentName = getFieldValue(report, "componentName", "");
            if ("/".equals(componentName)) {
                componentName = "";
            }
            List<String> segments = new ArrayList<>();
            int rowCount = node.get("rows").size();
            for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                JsonNode rowNode = node.get("rows").get(rowIdx);
                String type = rowNode.has("type") ? rowNode.get("type").asText("") : "";
                String total = rowNode.has("total") ? rowNode.get("total").asText("") : "";
                String locationDesc = rowNode.has("locationDesc") ? rowNode.get("locationDesc").asText("") : "";
                String locationNumber = rowNode.has("locationNumber") ? rowNode.get("locationNumber").asText("") : "";
                boolean firstRow = rowIdx == 0;
                String seg = buildDetectionDetailText(methodName, componentName, type, total, locationDesc, locationNumber,
                        firstRow, firstRow, experimentType, null, null);
                if (seg != null && !seg.trim().isEmpty()) {
                    segments.add(seg);
                }
            }
            if (segments.isEmpty()) {
                return single;
            }
            List<String> partsNoTrailingPeriod = new ArrayList<>(segments.size());
            for (String seg : segments) {
                String p = seg.trim();
                while (p.endsWith("。")) {
                    p = p.substring(0, p.length() - 1).trim();
                }
                if (!p.isEmpty()) {
                    partsNoTrailingPeriod.add(p);
                }
            }
            if (partsNoTrailingPeriod.isEmpty()) {
                return single;
            }
            return String.join("；", partsNoTrailingPeriod) + "。";
        } catch (Exception e) {
            log.warn("Failed to build multi-segment detection content for report {}: {}", report.getId(), e.getMessage());
            return single;
        }
    }
}
