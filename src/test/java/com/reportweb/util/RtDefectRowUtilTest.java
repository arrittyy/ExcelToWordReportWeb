package com.reportweb.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtDefectRowUtilTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void hasMeaningfulDefectColumn_validAndInvalid() throws Exception {
        ObjectNode withDefect = MAPPER.createObjectNode();
        withDefect.put("焊接接头编号", "W1");
        withDefect.put(RtDefectRowUtil.RT_DEFECT_COLUMN_KEY, "圆形缺陷 1 处");
        assertTrue(RtDefectRowUtil.hasMeaningfulDefectColumn(withDefect));

        ObjectNode slashOnly = MAPPER.createObjectNode();
        slashOnly.put("焊接接头编号", "W1");
        slashOnly.put(RtDefectRowUtil.RT_DEFECT_COLUMN_KEY, "/");
        assertFalse(RtDefectRowUtil.hasMeaningfulDefectColumn(slashOnly));

        ObjectNode jointOnly = MAPPER.createObjectNode();
        jointOnly.put("焊接接头编号", "W1");
        assertFalse(RtDefectRowUtil.hasMeaningfulDefectColumn(jointOnly));
    }

    @Test
    void hasEffectiveDefectColumn_skipRecordOnlyWhenEnabled() {
        ObjectNode row = MAPPER.createObjectNode();
        row.put(RtDefectRowUtil.RT_DEFECT_COLUMN_KEY, "夹渣 1 处");
        row.put(NdtDefectRowUtil.RECORD_ONLY_DEFECT_KEY, "是");

        assertFalse(RtDefectRowUtil.hasEffectiveDefectColumn(row, true));
        assertTrue(RtDefectRowUtil.hasEffectiveDefectColumn(row, false));
    }
}
