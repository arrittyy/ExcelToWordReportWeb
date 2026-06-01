package com.reportweb.util;

import com.reportweb.entity.Report;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordNumberFormatterTest {

    @Test
    void rundianInspector_insertsCodeBeforeSeq_preservingLeadingZeros() {
        Report report = new Report();
        report.setInspector("郭文");
        assertEquals(
                "CL2025-JCJL0198-48013",
                RecordNumberFormatter.format("CL2025-JCBG0198-013", report));
    }

    @Test
    void thirdPartyInspector_onlyBgToJl() {
        Report report = new Report();
        report.setInspector("外部检测员");
        assertEquals(
                "CL2025-JCJL0198-013",
                RecordNumberFormatter.format("CL2025-JCBG0198-013", report));
    }

    @Test
    void multipleInspectors_usesFirstWithCode() {
        Report report = new Report();
        report.setInspector("李世涛、郭文");
        assertEquals(
                "CL2025-JCJL0198-12013",
                RecordNumberFormatter.format("CL2025-JCBG0198-013", report));
    }

    @Test
    void seq001_withCode10() {
        Report report = new Report();
        report.setInspector("蔡红生");
        assertEquals(
                "CL2025-JCJL0198-10001",
                RecordNumberFormatter.format("CL2025-JCBG0198-001", report));
    }

    @Test
    void noTrailingSeq_onlyBgToJl() {
        Report report = new Report();
        report.setInspector("郭文");
        assertEquals(
                "CL2025-JCJL0198",
                RecordNumberFormatter.format("CL2025-JCBG0198", report));
    }

    @Test
    void aliasZhangBowei_mapsTo76() {
        Report report = new Report();
        report.setInspector("张博玮");
        assertEquals(
                "CL2025-JCJL0198-76013",
                RecordNumberFormatter.format("CL2025-JCBG0198-013", report));
    }
}
