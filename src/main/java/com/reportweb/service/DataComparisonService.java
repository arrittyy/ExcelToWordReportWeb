package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.util.TableDataMergeUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 通用的数据对比服务
 * 用于将实验数据与标准数据进行对比，找出不符合标准的记录
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataComparisonService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 不符合标准的记录
     */
    @Data
    public static class NonComplianceRecord {
        private String number;           // 编号
        private String itemName;         // 检测项目名称
        private String standardValue;   // 标准值
        private String actualValue;     // 实际值
        private String result;           // 判定结果（符合/不符合）
    }
    
    /**
     * 字段映射配置
     * key: 实验数据字段名，value: 标准数据字段名
     */
    public static class FieldMapping {
        private final Map<String, String> mapping;
        private final String numberField; // 编号字段名
        
        public FieldMapping(String numberField, Map<String, String> mapping) {
            this.numberField = numberField;
            this.mapping = mapping;
        }
        
        public String getNumberField() {
            return numberField;
        }
        
        public Map<String, String> getMapping() {
            return mapping;
        }
    }
    
    /**
     * 对比实验数据与标准数据
     * @param tableDataJson 实验数据 JSON 字符串（包含 rows 数组）
     * @param standardData 标准数据 Map（从 MaterialPropertyService 获取）
     * @param fieldMapping 字段映射配置
     * @return 不符合标准的记录列表
     */
    public List<NonComplianceRecord> compareData(
            String tableDataJson,
            Map<String, String> standardData,
            FieldMapping fieldMapping) {
        
        List<NonComplianceRecord> nonComplianceRecords = new ArrayList<>();
        
        if (tableDataJson == null || tableDataJson.trim().isEmpty()) {
            log.warn("实验数据为空，无法进行对比");
            return nonComplianceRecords;
        }
        
        if (standardData == null || standardData.isEmpty()) {
            log.warn("标准数据为空，无法进行对比");
            return nonComplianceRecords;
        }
        
        try {
            JsonNode tableData = objectMapper.readTree(tableDataJson);
            if (!tableData.has("rows") || !tableData.get("rows").isArray()) {
                log.warn("实验数据格式错误，缺少 rows 数组");
                return nonComplianceRecords;
            }
            
            // 遍历每一行实验数据
            for (JsonNode row : tableData.get("rows")) {
                String number = getFieldValue(row, fieldMapping.getNumberField());
                
                // 对比每个字段
                for (Map.Entry<String, String> entry : fieldMapping.getMapping().entrySet()) {
                    String experimentField = entry.getKey();      // 实验数据字段名
                    String standardField = entry.getValue();     // 标准数据字段名
                    
                    String actualValue = getFieldValue(row, experimentField);
                    String standardValue = standardData.get(standardField);
                    
                    // 跳过空值
                    if (actualValue == null || actualValue.isEmpty() || actualValue.equals("/")) {
                        continue;
                    }
                    
                    if (standardValue == null || standardValue.isEmpty()) {
                        continue;
                    }
                    
                    // 判断是否符合标准
                    boolean meetsStandard = meetsStandard(standardValue, actualValue);
                    
                    if (!meetsStandard) {
                        NonComplianceRecord record = new NonComplianceRecord();
                        record.setNumber(number != null ? number : "/");
                        record.setItemName(experimentField);
                        record.setStandardValue(standardValue);
                        record.setActualValue(actualValue);
                        record.setResult("不符合标准要求");
                        nonComplianceRecords.add(record);
                        
                        log.debug("发现不符合标准记录: 编号={}, 项目={}, 标准值={}, 实际值={}",
                                number, experimentField, standardValue, actualValue);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("对比数据时发生错误", e);
        }
        
        return nonComplianceRecords;
    }
    
    /**
     * 从 JSON 节点获取字段值
     */
    private String getFieldValue(JsonNode row, String fieldName) {
        if (row == null || fieldName == null) {
            return null;
        }
        
        if (row.has(fieldName)) {
            JsonNode valueNode = row.get(fieldName);
            if (valueNode.isTextual()) {
                return valueNode.asText();
            } else if (valueNode.isNumber()) {
                return String.valueOf(valueNode.asDouble());
            } else if (!valueNode.isNull()) {
                return valueNode.asText();
            }
        }
        
        return null;
    }
    
    /**
     * 根据检测类型代码获取字段映射配置
     * 统一管理所有检测类型的字段映射关系，提高复用性
     * 
     * @param experimentTypeCode 检测类型代码（如 "RTN"、"HTN" 等）
     * @return 字段映射配置，如果未找到则返回 null
     */
    public static FieldMapping getFieldMapping(String experimentTypeCode) {
       
        if (experimentTypeCode == null || experimentTypeCode.trim().isEmpty()) {
            return null;
        }
        
        String code = experimentTypeCode.trim().toUpperCase();
        Map<String, String> mapping = new HashMap<>();
        /**
          * 无损检测（7种）
            超声检测 (UT)：8个字段
            渗透检测 (PT)：6个字段
            磁粉检测 (MT)：6个字段
            射线检测 (RT)：9个字段
            内窥镜检测 (VT)：1个字段
            涡流检测 (ET)：5个字段
            相控阵超声波检测 (PAUT)：8个字段
            理化检测（9种）
            -室温拉伸检测 (RTN)：4个字段
            -高温拉伸检测 (HTN)：4个字段
            -高温持久强度检测 (HTC)：3个字段
            -布氏硬度检测 (BHD)：5个字段（编号、1、2、3、平均）
            -维氏硬度检测 (VHN)：5个字段
            -洛氏硬度检测 (RHN)：7个字段
            -里氏硬度检测 (LHD)：比对仅「平均」列（专用 Leeb 方法）；表里另有 1～5 等测点列不参与通用映射
            冲击吸收能量检测 (IMP)：4个字段
            -金相检测 (MET)：1个字段
            合金分析检测 (AAT)：9个字段
            其他检测（6种）
            超声波测厚 (UTT)：测点编号、实测厚度（最小需要厚度在检测内容中填写）
            管径测量 (PDM)：2个字段
            氧化皮堆积检测 (SOD)：2个字段
            圆度测量 (RDM)：6个字段
            有效硬化层深度检测 (CHD)：2个字段
         */
        switch (code) {
            case "RTN": // 室温拉伸检测
                // 实验数据字段名 -> 标准数据字段名
                mapping.put("抗拉强度Rm", "抗拉强度");
                mapping.put("下屈服强度或规定塑性延伸强度ReL或RP0.2", "下屈服强度");
                mapping.put("断后伸长率A", "断后伸长率");
                return new FieldMapping("编号", mapping);
                
            case "HTN": // 高温拉伸检测
                mapping.put("抗拉强度", "抗拉强度");
                mapping.put("高温规定塑性延伸强度", "下屈服强度"); // 映射到标准值字段
                mapping.put("断后伸长率", "断后伸长率");
                return new FieldMapping("编号", mapping);

            case "BHT":
            case "BHD": // 布氏硬度检测（含各次测量列 1、2、3…，与「平均」同一标准）
                mapping.put("平均", "布氏");
                addHardnessPointColumnMappings(mapping, "布氏");
                return new FieldMapping("编号", mapping);

            case "VHT":
            case "VHN": // 维氏硬度检测
                mapping.put("平均", "维氏");
                addHardnessPointColumnMappings(mapping, "维氏");
                return new FieldMapping("编号", mapping);

            case "RHT":
            case "RHN": // 洛氏硬度检测
                mapping.put("平均", "洛氏");
                addHardnessPointColumnMappings(mapping, "洛氏");
                return new FieldMapping("编号", mapping);

            case "LHT":
            case "LHD": // 里氏：业务上仅「平均」参与判定；通用 compareData 勿映射测点列 1、2、3…
                mapping.put("平均", "里氏");
                return new FieldMapping("编号", mapping);

            case "HTC": // 高温持久强度检测
                mapping.put("断后伸长率A", "断后伸长率");
                return new FieldMapping("编号", mapping);
                
            case "PMI":
            case "AAT": // 合金分析检测
                // 元素名称直接映射到标准值字段名
                mapping.put("Mn", "Mn");
                mapping.put("Cr", "Cr");
                mapping.put("Mo", "Mo");
                mapping.put("V", "V");
                mapping.put("Ti", "Ti");
                mapping.put("Ni", "Ni");
                mapping.put("Al", "Al");
                mapping.put("Cu", "Cu");
                mapping.put("Nb", "Nb");
                mapping.put("W", "W");
                mapping.put("Co", "Co");
                mapping.put("Mg", "Mg");
                mapping.put("Zr", "Zr");
                return new FieldMapping("编号", mapping);
                
            default:
                // 如果未找到对应配置，返回 null
                log.debug("未找到检测类型 {} 的字段映射配置", code);
                return null;
        }
    }

    /** 硬度表多次测量列名常为 1、2、3…，与「平均」共用同一标准字段 */
    private static void addHardnessPointColumnMappings(Map<String, String> mapping, String standardKey) {
        for (int d = 1; d <= 20; d++) {
            mapping.put(String.valueOf(d), standardKey);
        }
    }

    // ========== 标准值解析和比较方法 ==========

    private boolean meetsStandard(String standardValue, String actualValue) {
        if (standardValue == null || standardValue.trim().isEmpty()) {
            log.warn("标准值为空，无法比较");
            return true; // 标准值为空时，默认认为符合
        }
        
        if (actualValue == null || actualValue.trim().isEmpty() || actualValue.equals("/")) {
            log.warn("实际值为空，无法比较");
            return true; // 实际值为空时，默认认为符合（不进行判定）
        }
        
        try {
            double actual = parseNumber(actualValue);
            return meetsStandard(standardValue.trim(), actual);
        } catch (NumberFormatException e) {
            log.warn("无法解析实际值: {}, 标准值: {}", actualValue, standardValue);
            return true; // 无法解析时，默认认为符合
        }
    }

    /**
     * 里氏硬度专用比较：按行选用管件 / 钢管 / 焊缝硬度范围。
     * 仅对 {@code valueField}（一般为「平均」）判定；测点列 1、2、3… 不参与。
     * 行内 {@link LeebHardnessCategoryResolver#FIELD_LEEB_CATEGORY} 优先，否则按编号推断（弯头或 w/W → 管件；方位 → 钢管；其余 → 焊缝）。
     *
     * @param steelPipeRange 钢管硬度范围（可空）
     */
    public List<NonComplianceRecord> compareLeebWithPipeAndWeldRanges(
            String tableDataJson,
            String pipeRange,
            String weldRange,
            String steelPipeRange,
            String numberField,
            String valueField) {

        List<NonComplianceRecord> result = new ArrayList<>();

        boolean noPipe = pipeRange == null || pipeRange.trim().isEmpty();
        boolean noWeld = weldRange == null || weldRange.trim().isEmpty();
        boolean noSteel = steelPipeRange == null || steelPipeRange.trim().isEmpty();
        if (noPipe && noWeld && noSteel) {
            return result;
        }

        if (tableDataJson == null || tableDataJson.trim().isEmpty()) {
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(tableDataJson);
            JsonNode rows = root.get("rows");
            if (rows == null || !rows.isArray()) {
                return result;
            }

            for (JsonNode row : rows) {
                String number = getFieldValue(row, numberField);
                String actualValue = getFieldValue(row, valueField);

                LeebHardnessCategoryResolver.Category cat =
                        LeebHardnessCategoryResolver.resolveCategory(row, numberField);
                String standard;
                switch (cat) {
                    case PIPE_FITTING:
                        standard = pipeRange;
                        break;
                    case STEEL_PIPE:
                        standard = steelPipeRange;
                        break;
                    default:
                        standard = weldRange;
                }

                if (standard == null || standard.trim().isEmpty()) {
                    continue;
                }

                if (actualValue != null && !actualValue.trim().isEmpty() && !"/".equals(actualValue.trim())) {
                    boolean ok = meetsStandard(standard, actualValue);
                    if (!ok) {
                        NonComplianceRecord record = new NonComplianceRecord();
                        record.setNumber(number != null ? number : "");
                        record.setItemName(cat.getItemName());
                        record.setStandardValue(standard);
                        record.setActualValue(actualValue);
                        record.setResult("不符合标准要求");
                        result.add(record);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("里氏硬度管件/焊缝/钢管比较时出错: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 里氏硬度专用比较：螺栓/螺帽均使用同一 {@code boltRange}（材质库「里氏-螺栓」）。
     * 行内 {@code typeField} 优先；缺失时按 {@code perContentRow} 块下标回退到 detectionContent.rows[i].type。
     * 仅对 {@code valueField}（一般为「平均」）判定；测点列不参与。
     */
    public List<NonComplianceRecord> compareLeebBoltAndNutRanges(
            String tableDataJson,
            Object detectionContent,
            String boltRange,
            String numberField,
            String valueField,
            String typeField) {

        List<NonComplianceRecord> result = new ArrayList<>();

        if (boltRange == null || boltRange.trim().isEmpty()) {
            return result;
        }

        if (tableDataJson == null || tableDataJson.trim().isEmpty()) {
            return result;
        }

        List<String> contentTypes = readDetectionContentRowTypes(detectionContent);

        try {
            List<JsonNode> blocks = TableDataMergeUtil.perContentRowBlocks(tableDataJson, objectMapper);
            if (!blocks.isEmpty()) {
                for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
                    JsonNode block = blocks.get(blockIndex);
                    String blockType = blockIndex < contentTypes.size() ? contentTypes.get(blockIndex) : "";
                    JsonNode rows = block != null ? block.get("rows") : null;
                    if (rows == null || !rows.isArray()) {
                        continue;
                    }
                    for (JsonNode row : rows) {
                        if (TableDataMergeUtil.isTrailingSlashPlaceholderRow(row)) {
                            continue;
                        }
                        compareLeebBoltOrNutRow(
                                row, blockType, boltRange, numberField, valueField, typeField, result);
                    }
                }
                return result;
            }

            JsonNode root = objectMapper.readTree(tableDataJson);
            JsonNode rows = root.get("rows");
            if (rows == null || !rows.isArray()) {
                return result;
            }
            String fallbackType = contentTypes.isEmpty() ? "" : contentTypes.get(0);
            for (JsonNode row : rows) {
                if (TableDataMergeUtil.isTrailingSlashPlaceholderRow(row)) {
                    continue;
                }
                compareLeebBoltOrNutRow(row, fallbackType, boltRange, numberField, valueField, typeField, result);
            }
        } catch (Exception e) {
            log.warn("里氏硬度螺栓/螺帽比较时出错: {}", e.getMessage());
        }

        return result;
    }

    private void compareLeebBoltOrNutRow(
            JsonNode row,
            String blockTypeFallback,
            String boltRange,
            String numberField,
            String valueField,
            String typeField,
            List<NonComplianceRecord> result) {

        String number = getFieldValue(row, numberField);
        String actualValue = getFieldValue(row, valueField);
        String type = getFieldValue(row, typeField);
        if (type == null || type.trim().isEmpty()) {
            type = blockTypeFallback;
        }

        boolean isNut = LeebHardnessModeResolver.typeTextIsNut(type);
        String itemName = isNut ? "螺帽硬度" : "螺栓硬度";

        if (actualValue == null || actualValue.trim().isEmpty() || "/".equals(actualValue.trim())) {
            return;
        }

        boolean ok = meetsStandard(boltRange, actualValue);
        if (!ok) {
            NonComplianceRecord record = new NonComplianceRecord();
            record.setNumber(number != null ? number : "");
            record.setItemName(itemName);
            record.setStandardValue(boltRange);
            record.setActualValue(actualValue);
            record.setResult("不符合标准要求");
            result.add(record);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readDetectionContentRowTypes(Object detectionContent) {
        List<String> types = new ArrayList<>();
        if (detectionContent == null) {
            return types;
        }
        try {
            JsonNode node = objectMapper.valueToTree(detectionContent);
            if (node != null && node.isObject() && node.has("rows") && node.get("rows").isArray()) {
                for (JsonNode row : node.get("rows")) {
                    types.add(row.has("type") ? row.get("type").asText("") : "");
                }
            }
        } catch (Exception e) {
            log.debug("readDetectionContentRowTypes: {}", e.getMessage());
        }
        return types;
    }

    /**
     * 将形如 \"197～241\" 或 \"197-241\" 的范围整体按系数缩放
     */
    private String scaleRange(String rangeStr, double factor) {
        if (rangeStr == null || rangeStr.trim().isEmpty()) {
            return rangeStr;
        }
        String trimmed = rangeStr.trim();
        String separator = trimmed.contains("～") ? "～" : (trimmed.contains("-") ? "-" : null);
        if (separator == null) {
            try {
                double value = parseNumber(trimmed);
                double scaled = value * factor;
                return formatNumber(scaled);
            } catch (NumberFormatException e) {
                log.warn("无法缩放范围值: {}", rangeStr);
                return rangeStr;
            }
        }

        String[] parts = trimmed.split(separator);
        if (parts.length != 2) {
            return rangeStr;
        }

        try {
            double min = parseNumber(parts[0].trim());
            double max = parseNumber(parts[1].trim());
            double scaledMin = min * factor;
            double scaledMax = max * factor;
            return formatNumber(scaledMin) + "～" + formatNumber(scaledMax);
        } catch (NumberFormatException e) {
            log.warn("无法缩放范围值: {}", rangeStr);
            return rangeStr;
        }
    }

    /**
     * 将数值格式化为字符串，这里统一保留 1 位小数，去掉末尾无意义的 .0
     */
    private String formatNumber(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
        if (bd.scale() > 0 && bd.stripTrailingZeros().scale() <= 0) {
            bd = bd.stripTrailingZeros();
        }
        return bd.toPlainString();
    }
    
    /**
     * 判断实验值是否符合标准（数值版本）
     * @param standardValue 标准值字符串
     * @param actualValue 实验实际值（数值）
     * @return true 表示符合标准，false 表示不符合标准
     */
    private boolean meetsStandard(String standardValue, double actualValue) {
        if (standardValue == null || standardValue.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = MaterialStandardValueNormalizer.normalize(standardValue.trim());
        
        // 1. 处理范围格式：410～550、410-550
        if (trimmed.contains("～") || trimmed.contains("-")) {
            return checkRange(trimmed, actualValue);
        }
        
        // 2. 处理大于等于：≥585、>=585
        if (trimmed.startsWith("≥") || trimmed.startsWith(">=")) {
            String numStr = trimmed.replaceFirst("≥|>=", "").trim();
            double threshold = parseNumber(numStr);
            return actualValue >= threshold;
        }
        
        // 3. 处理小于等于：≤179、<=179
        if (trimmed.startsWith("≤") || trimmed.startsWith("<=")) {
            String numStr = trimmed.replaceFirst("≤|<=", "").trim();
            double threshold = parseNumber(numStr);
            return actualValue <= threshold;
        }
        
        // 4. 处理大于：>585
        if (trimmed.startsWith(">")) {
            String numStr = trimmed.replaceFirst(">", "").trim();
            double threshold = parseNumber(numStr);
            return actualValue > threshold;
        }
        
        // 5. 处理小于：<179
        if (trimmed.startsWith("<")) {
            String numStr = trimmed.replaceFirst("<", "").trim();
            double threshold = parseNumber(numStr);
            return actualValue < threshold;
        }
        
        // 6. 处理纯数字：精确匹配（允许小误差）
        try {
            double standard = parseNumber(trimmed);
            return Math.abs(actualValue - standard) < 0.001;
        } catch (NumberFormatException e) {
            log.warn("无法解析标准值格式: {}", standardValue);
            return true; // 无法解析时，默认认为符合
        }
    }
    
    /**
     * 检查范围格式：410～550
     * @param rangeStr 范围字符串
     * @param actualValue 实际值
     * @return 是否符合范围
     */
    private boolean checkRange(String rangeStr, double actualValue) {
        // 支持 ～ 和 - 作为分隔符
        String separator = rangeStr.contains("～") ? "～" : "-";
        String[] parts = rangeStr.split(separator);
        
        if (parts.length != 2) {
            log.warn("范围格式错误: {}", rangeStr);
            return true;
        }
        
        try {
            double min = parseNumber(parts[0].trim());
            double max = parseNumber(parts[1].trim());
            return actualValue >= min && actualValue <= max;
        } catch (NumberFormatException e) {
            log.warn("无法解析范围值: {}", rangeStr);
            return true;
        }
    }
    
    /**
     * 解析数字字符串，支持去除单位等
     * @param numStr 数字字符串，可能包含单位如 "585MPa"、"20%"
     * @return 解析后的数值
     */
    private double parseNumber(String numStr) {
        if (numStr == null || numStr.trim().isEmpty()) {
            throw new NumberFormatException("空字符串");
        }
        
        // 使用正则表达式提取数字部分（包括小数）
        Pattern pattern = Pattern.compile("([-+]?\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(numStr.trim());
        
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        
        throw new NumberFormatException("无法从字符串中提取数字: " + numStr);
    }
}

