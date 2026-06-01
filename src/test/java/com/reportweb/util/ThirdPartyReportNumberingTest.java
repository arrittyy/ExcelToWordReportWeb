package com.reportweb.util;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartyReportNumberingTest {

    @Test
    void usesPerTypeNumbering_onlyHuatuExactMatch() {
        assertTrue(ThirdPartyReportNumbering.usesPerTypeNumbering("安徽华图电力科技有限公司"));
        assertTrue(ThirdPartyReportNumbering.usesPerTypeNumbering("  安徽华图电力科技有限公司  "));
        assertFalse(ThirdPartyReportNumbering.usesPerTypeNumbering(ThirdPartyReportNumbering.SIWEIQI_NAME));
        assertFalse(ThirdPartyReportNumbering.usesPerTypeNumbering("安徽华图"));
        assertFalse(ThirdPartyReportNumbering.usesPerTypeNumbering("自定义第三方"));
        assertFalse(ThirdPartyReportNumbering.usesPerTypeNumbering(null));
        assertFalse(ThirdPartyReportNumbering.usesPerTypeNumbering(""));
    }

    @Test
    void formatHuatuNumber_appendsTypeCodeAndSeq() {
        assertEquals("HT2025-UT001", ThirdPartyReportNumbering.formatHuatuNumber("HT2025", "UT", 1));
        assertEquals("HT2025-PAUT002", ThirdPartyReportNumbering.formatHuatuNumber("HT2025", "PAUT", 2));
        assertEquals("未输入-UNK001", ThirdPartyReportNumbering.formatHuatuNumber(null, null, 1));
    }

    @Test
    void formatLegacyNumber_globalIncrement() {
        assertEquals("HT2025-001", ThirdPartyReportNumbering.formatLegacyNumber("HT2025", 1));
        assertEquals("HT2025-012", ThirdPartyReportNumbering.formatLegacyNumber("HT2025", 12));
    }

    @Test
    void parseTrailingSequence_legacyFormat() {
        assertEquals(OptionalInt.of(3), ThirdPartyReportNumbering.parseTrailingSequence("HT2025-003"));
        assertEquals(OptionalInt.of(12), ThirdPartyReportNumbering.parseTrailingSequence("CL2025-JCBG0198-012"));
    }

    @Test
    void parseTrailingSequence_huatuFormat() {
        assertEquals(OptionalInt.of(1), ThirdPartyReportNumbering.parseTrailingSequence("HT2025-UT001"));
        assertEquals(OptionalInt.of(2), ThirdPartyReportNumbering.parseTrailingSequence("HT2025-PAUT002"));
        assertEquals(OptionalInt.of(5), ThirdPartyReportNumbering.parseTrailingSequence("HT2025-UT005"));
    }

    @Test
    void withTrailingSequenceOffset_utMultiRow() {
        assertEquals("HT2025-UT002",
                ThirdPartyReportNumbering.withTrailingSequenceOffset("HT2025-UT001", 1));
        assertEquals("HT2025-004",
                ThirdPartyReportNumbering.withTrailingSequenceOffset("HT2025-003", 1));
        assertEquals("HT2025-UT001",
                ThirdPartyReportNumbering.withTrailingSequenceOffset("HT2025-UT001", 0));
        assertEquals("invalid",
                ThirdPartyReportNumbering.withTrailingSequenceOffset("invalid", 1));
    }
}
