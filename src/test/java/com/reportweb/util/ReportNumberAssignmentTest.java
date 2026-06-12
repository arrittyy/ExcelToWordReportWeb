package com.reportweb.util;

import com.reportweb.entity.Project;
import com.reportweb.entity.Report;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportNumberAssignmentTest {

    private static Project project(String projectNumber, String thirdPartyName) {
        Project p = new Project();
        p.setProjectNumber(projectNumber);
        p.setThirdPartyProjectNumber("HT2025");
        p.setThirdPartyName(thirdPartyName);
        return p;
    }

    private static Report report(int id, String typeCode, boolean thirdParty) {
        Report r = new Report();
        r.setId(id);
        r.setReportNumber("old");
        r.setInspector(thirdParty ? "外部检测员" : "李世铭");
        return r;
    }

    private static final Function<Report, String> TYPE_BY_ID = r -> switch (r.getId()) {
        case 1, 10 -> "UT";
        case 2, 11 -> "PAUT";
        case 3 -> "UT";
        case 4, 5, 6 -> "MT";
        default -> "UNK";
    };

    @Test
    void huatu_assignsPerTypeFrom001() {
        Project p = project("PROJ-001", ThirdPartyReportNumbering.HUATU_POWER_NAME);
        Report ut1 = report(1, "UT", true);
        Report paut1 = report(2, "PAUT", true);
        Report ut2 = report(3, "UT", true);
        List<Report> ordered = List.of(ut1, paut1, ut2);

        ReportNumberAssignment.assign(p, ordered, (proj, r) -> true, TYPE_BY_ID);

        assertEquals("HT2025-UT001", ut1.getReportNumber());
        assertEquals("HT2025-PAUT001", paut1.getReportNumber());
        assertEquals("HT2025-UT002", ut2.getReportNumber());
    }

    @Test
    void siweiqi_assignsGlobalIncrement() {
        Project p = project("PROJ-001", ThirdPartyReportNumbering.SIWEIQI_NAME);
        Report r1 = report(4, "UT", true);
        Report r2 = report(5, "PAUT", true);
        Report r3 = report(6, "MT", true);
        List<Report> ordered = List.of(r1, r2, r3);

        ReportNumberAssignment.assign(p, ordered, (proj, r) -> true, TYPE_BY_ID);

        assertEquals("HT2025-001", r1.getReportNumber());
        assertEquals("HT2025-002", r2.getReportNumber());
        assertEquals("HT2025-003", r3.getReportNumber());
    }

    @Test
    void runDian_assignsProjectNumberIncrement() {
        Project p = project("RD2025", null);
        Report r1 = report(1, "UT", false);
        Report r2 = report(2, "PAUT", false);
        List<Report> ordered = List.of(r1, r2);

        ReportNumberAssignment.assign(p, ordered, (proj, r) -> false, TYPE_BY_ID);

        assertEquals("RD2025-001", r1.getReportNumber());
        assertEquals("RD2025-002", r2.getReportNumber());
    }

    @Test
    void mixed_runDianAndThirdPartyIndependentCounters() {
        Project p = project("RD2025", ThirdPartyReportNumbering.HUATU_POWER_NAME);
        Report runDian = report(1, "UT", false);
        Report tpUt = report(10, "UT", true);
        Report tpPaut = report(11, "PAUT", true);
        List<Report> ordered = List.of(runDian, tpUt, tpPaut);

        ReportNumberAssignment.assign(p, ordered,
                (proj, r) -> r.getInspector().contains("外部"), TYPE_BY_ID);

        assertEquals("RD2025-001", runDian.getReportNumber());
        assertEquals("HT2025-UT001", tpUt.getReportNumber());
        assertEquals("HT2025-PAUT001", tpPaut.getReportNumber());
    }
}
