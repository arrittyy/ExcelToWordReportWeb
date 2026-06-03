package com.reportweb.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UltrasonicThicknessMinRequiredRulesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void isBelowMinRequired_strictLessThan() {
        assertTrue(UltrasonicThicknessMinRequiredRules.isBelowMinRequired(4.9, 5.0));
        assertFalse(UltrasonicThicknessMinRequiredRules.isBelowMinRequired(5.0, 5.0));
        assertFalse(UltrasonicThicknessMinRequiredRules.isBelowMinRequired(5.1, 5.0));
    }

    @Test
    void parseMinRequiredFromDetectionContent_legacyTopLevel() {
        Map<String, Object> dc = Map.of(
                "mode", "table",
                UltrasonicThicknessMinRequiredRules.DETECTION_CONTENT_MIN_REQUIRED_KEY, "8.5",
                "rows", List.of());
        assertEquals(8.5, UltrasonicThicknessMinRequiredRules.parseMinRequiredFromDetectionContent(dc, mapper));
    }

    @Test
    void parseMinRequiredFromDetectionContent_perContentRow() {
        Map<String, Object> row0 = Map.of(
                "type", "直管段",
                UltrasonicThicknessMinRequiredRules.DETECTION_CONTENT_MIN_REQUIRED_KEY, "8.0");
        Map<String, Object> row1 = Map.of(
                "type", "弯头",
                UltrasonicThicknessMinRequiredRules.DETECTION_CONTENT_MIN_REQUIRED_KEY, "10.5");
        Map<String, Object> dc = Map.of("mode", "table", "rows", List.of(row0, row1));
        assertEquals(8.0, UltrasonicThicknessMinRequiredRules.parseMinRequiredFromDetectionContent(dc, mapper, 0));
        assertEquals(10.5, UltrasonicThicknessMinRequiredRules.parseMinRequiredFromDetectionContent(dc, mapper, 1));
    }

    @Test
    void evaluateRows_usesReportLevelMin() {
        ArrayNode rows = mapper.createArrayNode();
        rows.add(row("1", "9.5"));
        rows.add(row("2", "10"));
        rows.add(row("3", "7"));

        UltrasonicThicknessMinRequiredRules.EvaluationResult eval =
                UltrasonicThicknessMinRequiredRules.evaluateRows(rows, 8.0);
        assertTrue(eval.hasEvaluableRow());
        assertEquals(1, eval.failedPointNumbers().size());
        assertTrue(eval.failedPointNumbers().contains("3"));
    }

    /** 缺陷判定仍用 evaluateRows：无最小厚度时不标 evaluable、不记不合格点 */
    @Test
    void evaluateRows_noMin_returnsNotEvaluable() {
        ArrayNode rows = mapper.createArrayNode();
        rows.add(row("1", "5"));
        UltrasonicThicknessMinRequiredRules.EvaluationResult eval =
                UltrasonicThicknessMinRequiredRules.evaluateRows(rows, null);
        assertFalse(eval.hasEvaluableRow());
        assertTrue(eval.failedPointNumbers().isEmpty());
    }

    @Test
    void resolveConclusionSentence_noMin_treatsAsQualified() {
        ArrayNode rows = mapper.createArrayNode();
        rows.add(row("1", "3"));
        rows.add(row("2", "1"));
        assertEquals(UltrasonicThicknessMinRequiredRules.CONCLUSION_OK,
                UltrasonicThicknessMinRequiredRules.resolveConclusionSentence(rows, null));
        assertEquals(UltrasonicThicknessMinRequiredRules.CONCLUSION_OK,
                UltrasonicThicknessMinRequiredRules.resolveConclusionSentence(rows, 0.0));
        assertEquals(UltrasonicThicknessMinRequiredRules.CONCLUSION_OK,
                UltrasonicThicknessMinRequiredRules.resolveConclusionSentence(rows, -1.0));
    }

    @Test
    void resolveConclusionSentence_withMin_failsWhenBelowMin() {
        ArrayNode rows = mapper.createArrayNode();
        rows.add(row("1", "9.5"));
        rows.add(row("3", "7"));
        assertEquals("在上述检测条件下，3壁厚不满足DL438-2016的要求。",
                UltrasonicThicknessMinRequiredRules.resolveConclusionSentence(rows, 8.0));
    }

    @Test
    void resolveConclusionSentence_withMin_okWhenAllPass() {
        ArrayNode rows = mapper.createArrayNode();
        rows.add(row("1", "9.5"));
        rows.add(row("2", "10"));
        assertEquals(UltrasonicThicknessMinRequiredRules.CONCLUSION_OK,
                UltrasonicThicknessMinRequiredRules.resolveConclusionSentence(rows, 8.0));
    }

    @Test
    void buildConclusionSentence_okAndFail() {
        assertEquals("在上述检测条件下，未见异常。",
                UltrasonicThicknessMinRequiredRules.buildConclusionSentence(List.of()));
        assertEquals("在上述检测条件下，1、3壁厚不满足DL438-2016的要求。",
                UltrasonicThicknessMinRequiredRules.buildConclusionSentence(List.of("3", "1")));
    }

    @Test
    void parseMeasuredLegacyKey() {
        ObjectNode row = mapper.createObjectNode();
        row.put("测点编号", "A1");
        row.put("实测厚度（mm）", 11.5);
        assertEquals(11.5, UltrasonicThicknessMinRequiredRules.parseMeasuredThickness(row));
    }

    @Test
    void parseMeasuredAndMinRequired_stringValues() {
        ObjectNode row = mapper.createObjectNode();
        row.put("测点编号", "1");
        row.put("实测厚度", "7.99");
        assertEquals(7.99, UltrasonicThicknessMinRequiredRules.parseMeasuredThickness(row));

        Map<String, Object> dc = Map.of(
                "mode", "table",
                UltrasonicThicknessMinRequiredRules.DETECTION_CONTENT_MIN_REQUIRED_KEY, "8.50",
                "rows", List.of());
        assertEquals(8.5, UltrasonicThicknessMinRequiredRules.parseMinRequiredFromDetectionContent(dc, mapper));

        UltrasonicThicknessMinRequiredRules.EvaluationResult eval =
                UltrasonicThicknessMinRequiredRules.evaluateRows(List.of(row), 8.5);
        assertTrue(eval.hasEvaluableRow());
        assertEquals(1, eval.failedPointNumbers().size());
        assertTrue(eval.failedPointNumbers().contains("1"));
    }

    private static ObjectNode row(String point, String measured) {
        ObjectNode n = new ObjectMapper().createObjectNode();
        if (point != null && !point.isEmpty()) {
            n.put("测点编号", point);
        }
        if (measured != null && !measured.isEmpty()) {
            n.put("实测厚度", measured);
        }
        return n;
    }
}
