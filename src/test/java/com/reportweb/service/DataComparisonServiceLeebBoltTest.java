package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataComparisonServiceLeebBoltTest {

    private final DataComparisonService service = new DataComparisonService(new ObjectMapper());

    @Test
    void compareLeebBoltAndNut_usesDetectionContentType_whenTableHasNoTypeColumn() {
        String tableData =
                """
                {"rows":[{"编号":"B1","平均":"350"}]}
                """;
        Map<String, Object> detectionContent = new LinkedHashMap<>();
        detectionContent.put("mode", "table");
        detectionContent.put(
                "rows",
                List.of(Map.of("type", "螺栓（检测部位：端部）", "locationNumber", "", "total", "")));

        var records = service.compareLeebBoltAndNutRanges(
                tableData, detectionContent, "255～311", "编号", "平均", "类型");

        assertEquals(1, records.size());
        assertEquals("螺栓硬度", records.get(0).getItemName());
        assertEquals("255～311", records.get(0).getStandardValue());
    }

    @Test
    void compareLeebBoltAndNut_nutUsesSameRangeAsBolt() {
        String tableData =
                """
                {"rows":[{"编号":"N1","平均":"350","类型":"螺帽"}]}
                """;
        var records = service.compareLeebBoltAndNutRanges(
                tableData, null, "255～311", "编号", "平均", "类型");

        assertEquals(1, records.size());
        assertEquals("螺帽硬度", records.get(0).getItemName());
        assertEquals("255～311", records.get(0).getStandardValue());
    }

    @Test
    void compareLeebBoltAndNut_inRangeProducesNoRecords() {
        String tableData =
                """
                {"rows":[{"编号":"B1","平均":"280"}]}
                """;
        Map<String, Object> detectionContent = Map.of(
                "mode", "table",
                "rows", List.of(Map.of("type", "螺栓（检测部位：腰部）")));

        var records = service.compareLeebBoltAndNutRanges(
                tableData, detectionContent, "255～311", "编号", "平均", "类型");

        assertTrue(records.isEmpty());
    }

    @Test
    void compareLeebBoltAndNut_perContentRowUsesBlockType() {
        String tableData =
                """
                {"perContentRow":[
                  {"rows":[{"编号":"B1","平均":"350"}]},
                  {"rows":[{"编号":"N1","平均":"350"}]}
                ],"rows":[{"编号":"B1","平均":"350"},{"编号":"N1","平均":"350"}]}
                """;
        Map<String, Object> detectionContent = new LinkedHashMap<>();
        detectionContent.put("mode", "table");
        detectionContent.put(
                "rows",
                List.of(
                        Map.of("type", "螺栓（检测部位：端部）"),
                        Map.of("type", "螺帽")));

        var records = service.compareLeebBoltAndNutRanges(
                tableData, detectionContent, "255～311", "编号", "平均", "类型");

        assertEquals(2, records.size());
        assertEquals("螺栓硬度", records.get(0).getItemName());
        assertEquals("螺帽硬度", records.get(1).getItemName());
        assertEquals("255～311", records.get(0).getStandardValue());
        assertEquals("255～311", records.get(1).getStandardValue());
    }
}
