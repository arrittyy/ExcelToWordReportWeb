package com.reportweb.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NdtDefectRowUtilTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void blockHasMeaningfulDefectRows_recordOnlyRowIsIgnoredWhenEnabled() {
        ArrayNode rows = MAPPER.createArrayNode();
        ObjectNode recordOnly = MAPPER.createObjectNode();
        recordOnly.put("编号", "1");
        recordOnly.put("长度", "12");
        recordOnly.put(NdtDefectRowUtil.RECORD_ONLY_DEFECT_KEY, "是");
        rows.add(recordOnly);

        assertFalse(NdtDefectRowUtil.blockHasMeaningfulDefectRows(rows, true));
        assertTrue(NdtDefectRowUtil.blockHasMeaningfulDefectRows(rows, false));
    }

    @Test
    void blockHasMeaningfulDefectRows_mixedRowsStillHasDefect() {
        ArrayNode rows = MAPPER.createArrayNode();
        ObjectNode recordOnly = MAPPER.createObjectNode();
        recordOnly.put("编号", "1");
        recordOnly.put("长度", "12");
        recordOnly.put(NdtDefectRowUtil.RECORD_ONLY_DEFECT_KEY, "是");
        rows.add(recordOnly);

        ObjectNode effective = MAPPER.createObjectNode();
        effective.put("编号", "2");
        effective.put("长度", "8");
        rows.add(effective);

        assertTrue(NdtDefectRowUtil.blockHasMeaningfulDefectRows(rows, true));
    }
}
