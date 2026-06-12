package com.reportweb.util;

import com.reportweb.entity.Project;
import com.reportweb.entity.Report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 按概述顺序为报告分配编号（唯一算法入口）。
 * 润电：projectNumber-001；思维奇/自定义第三方：tpBase-001 全局递增；华图：tpBase-{TYPE}{001} 按检测类型递增。
 */
public final class ReportNumberAssignment {

    private ReportNumberAssignment() {
    }

    public static void assign(
            Project project,
            List<Report> orderedReports,
            BiPredicate<Project, Report> usesThirdPartyBranding,
            Function<Report, String> resolveTypeCode) {
        if (project == null || orderedReports == null || orderedReports.isEmpty()) {
            return;
        }
        String pn = project.getProjectNumber() != null ? project.getProjectNumber().trim() : "";

        boolean needRunDian = false;
        boolean needThirdParty = false;
        for (Report r : orderedReports) {
            if (usesThirdPartyBranding.test(project, r)) {
                needThirdParty = true;
            } else {
                needRunDian = true;
            }
        }
        if (needRunDian && pn.isEmpty() && !needThirdParty) {
            return;
        }

        String effectiveTpBase = ThirdPartyPlaceholders.effectiveThirdPartyProjectNumberBase(
                project.getThirdPartyProjectNumber());

        int runDianSeq = 1;
        int thirdPartySeq = 1;
        boolean huatuPerType = ThirdPartyReportNumbering.usesPerTypeNumbering(project.getThirdPartyName());
        Map<String, Integer> thirdPartyTypeSeq = huatuPerType ? new HashMap<>() : null;
        for (Report report : orderedReports) {
            if (usesThirdPartyBranding.test(project, report)) {
                if (huatuPerType) {
                    String typeCode = resolveTypeCode.apply(report);
                    int seq = thirdPartyTypeSeq.merge(typeCode, 1, Integer::sum);
                    report.setReportNumber(ThirdPartyReportNumbering.formatHuatuNumber(
                            effectiveTpBase, typeCode, seq));
                } else {
                    report.setReportNumber(ThirdPartyReportNumbering.formatLegacyNumber(
                            effectiveTpBase, thirdPartySeq++));
                }
            } else {
                if (pn.isEmpty()) {
                    continue;
                }
                report.setReportNumber(pn + "-" + String.format("%03d", runDianSeq++));
            }
        }
    }
}
