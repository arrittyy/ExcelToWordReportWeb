package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.util.LocationNumberCompressor;
import com.reportweb.util.TableDataMergeUtil;
import com.reportweb.util.UltrasonicThicknessMinRequiredRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 根据检测数据（tableData）自动生成「检测内容」中的类型、附图中位置编号、总计。
 * 支持 perContentRow 分块：每一块只填充对应 detectionContent.rows[i] 的编号类字段。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionContentAutoFillService {

    private static final Set<String> AUTO_FILL_TYPE_CODES = new HashSet<>(Arrays.asList(
            "SOD", "BHT", "LHT", "RDM", "VHT", "RHT", "PDM", "HTC", "CHD", "HTN", "IMP", "RTN", "PMI", "UTM",
            // 兼容历史编码
            "BHD", "LHD", "VHN", "RHN", "AAT", "UTT"
    ));

    private static final Map<String, String> LOCATION_NUMBER_COLUMN_BY_CODE = new HashMap<>();
    static {
        LOCATION_NUMBER_COLUMN_BY_CODE.put("SOD", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("BHT", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("LHT", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("RDM", "弯头编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("VHT", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("RHT", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("PDM", "测点编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("HTC", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("CHD", "至边缘距离");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("HTN", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("IMP", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("RTN", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("PMI", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("UTM", "测点编号");
        // 兼容历史编码
        LOCATION_NUMBER_COLUMN_BY_CODE.put("BHD", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("LHD", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("VHN", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("RHN", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("AAT", "编号");
        LOCATION_NUMBER_COLUMN_BY_CODE.put("UTT", "测点编号");
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isAutoFillType(String experimentTypeCode) {
        return experimentTypeCode != null && AUTO_FILL_TYPE_CODES.contains(experimentTypeCode.trim().toUpperCase());
    }

    /**
     * 根据 tableData JSON 更新 detectionContent（mode=table, 多行与 perContentRow 对齐）。
     * 保留每行已有 type、locationDesc；从各分块检测数据填充 locationNumber、total。
     */
    public Map<String, Object> generateFromTableData(
            String experimentTypeCode,
            String experimentTypeName,
            String tableDataJson,
            Map<String, Object> existingDetectionContent) {
        if (experimentTypeCode == null || experimentTypeName == null) {
            return null;
        }
        String code = experimentTypeCode.trim().toUpperCase();
        if (!AUTO_FILL_TYPE_CODES.contains(code)) {
            return null;
        }
        String locationColumnKey = LOCATION_NUMBER_COLUMN_BY_CODE.get(code);
        if (locationColumnKey == null) {
            return null;
        }

        List<JsonNode> blocks = TableDataMergeUtil.perContentRowBlocks(tableDataJson, objectMapper);
        if (blocks.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> existingRows = readExistingTableRows(existingDetectionContent);
        String existingConclusion = readTopString(existingDetectionContent, "conclusion");
        String existingPosition = readTopString(existingDetectionContent, "position");
        String existingValue = readTopString(existingDetectionContent, "value");

        int n = blocks.size();
        if (n == 0) {
            return null;
        }

        List<Map<String, Object>> outRows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            JsonNode rowsNode = i < blocks.size() ? blocks.get(i).get("rows") : null;
            Map<String, Object> prev = i < existingRows.size() ? existingRows.get(i) : Collections.emptyMap();
            String type = stringVal(prev.get("type"));
            String locationDesc = stringVal(prev.get("locationDesc"));

            List<String> locationNumbers = new ArrayList<>();
            List<JsonNode> rowNodesForCount = new ArrayList<>();
            if (rowsNode != null && rowsNode.isArray()) {
                rowsNode.forEach(rowNodesForCount::add);
                for (JsonNode rowNode : rowsNode) {
                    if (TableDataMergeUtil.isTrailingSlashPlaceholderRow(rowNode)) {
                        continue;
                    }
                    if (rowNode != null && rowNode.has(locationColumnKey)) {
                        String val = rowNode.get(locationColumnKey).asText("");
                        if (!val.trim().isEmpty()) {
                            locationNumbers.add(val.trim());
                        }
                    }
                }
            }
            String locationNumberStr = LocationNumberCompressor.compressJoined(locationNumbers);
            String totalStr = String.valueOf(TableDataMergeUtil.effectiveRowCountExcludingTrailingPlaceholder(rowNodesForCount));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("locationNumber", locationNumberStr);
            row.put("total", totalStr);
            row.put("locationDesc", locationDesc);
            String rowMin = stringVal(prev.get("minRequiredThickness"));
            if (rowMin == null || rowMin.isBlank()) {
                String legacyTop = readTopString(
                        existingDetectionContent,
                        UltrasonicThicknessMinRequiredRules.DETECTION_CONTENT_MIN_REQUIRED_KEY);
                if (legacyTop != null && !legacyTop.isBlank() && i == 0) {
                    rowMin = legacyTop;
                }
            }
            if (rowMin != null && !rowMin.isBlank()) {
                row.put(UltrasonicThicknessMinRequiredRules.DETECTION_CONTENT_MIN_REQUIRED_KEY, rowMin);
            }
            outRows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        // 氧化皮堆积检测（SOD）：前端使用 mode=sod，含探头规格/管样/检测灵敏度标定；自动填充仅更新 rows，必须保留上述字段
        if ("SOD".equals(code)) {
            result.put("mode", "sod");
            result.put("probeSpec", stringVal(existingDetectionContent != null ? existingDetectionContent.get("probeSpec") : null));
            result.put("tubeSample", stringVal(existingDetectionContent != null ? existingDetectionContent.get("tubeSample") : null));
            result.put("sensitivityCalibration", stringVal(existingDetectionContent != null ? existingDetectionContent.get("sensitivityCalibration") : null));
        } else {
            result.put("mode", "table");
        }
        result.put("rows", outRows);
        if (existingConclusion != null) {
            result.put("conclusion", existingConclusion);
        }
        if (existingPosition != null) {
            result.put("position", existingPosition);
        }
        if (existingValue != null) {
            result.put("value", existingValue);
        }
        return result;
    }

    private static String readTopString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readExistingTableRows(Map<String, Object> existingDetectionContent) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (existingDetectionContent == null) {
            return list;
        }
        try {
            Object rowsObj = existingDetectionContent.get("rows");
            if (!(rowsObj instanceof List)) {
                return list;
            }
            for (Object o : (List<?>) rowsObj) {
                if (o instanceof Map) {
                    list.add(new LinkedHashMap<>((Map<String, Object>) o));
                }
            }
        } catch (Exception e) {
            log.debug("readExistingTableRows: {}", e.getMessage());
        }
        return list;
    }

    private static String stringVal(Object o) {
        return o != null ? String.valueOf(o) : "";
    }
}
