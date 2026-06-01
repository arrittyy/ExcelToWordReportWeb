package com.reportweb.util;

import com.reportweb.entity.Project;
import com.reportweb.entity.Report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 内部检测人员白名单（与 {@link RunDianPersonnelRegistry} 保持同步）。
 */
public final class InternalInspectorWhitelist {

    private InternalInspectorWhitelist() {
    }

    public static boolean contains(String name) {
        return RunDianPersonnelRegistry.contains(name);
    }

    /**
     * 解析检测人员字符串为姓名列表（忽略空串）。
     */
    public static List<String> splitInspectorTokens(String inspector) {
        if (inspector == null) {
            return Collections.emptyList();
        }
        String s = inspector.trim();
        if (s.isEmpty() || "/".equals(s)) {
            return Collections.emptyList();
        }
        String[] parts = s.split("[|、,，\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p != null ? p.trim() : "";
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * 该条报告的单项 Word 是否使用第三方样式（公司名、第三方项目编号）。
     * 无有效姓名 token（空或仅 /）→ 润电样式（false）。
     * 全部在名单内 → false；任一不在名单 → true。
     */
    public static boolean reportUsesThirdPartyBranding(Report report) {
        if (report == null) {
            return false;
        }
        List<String> tokens = splitInspectorTokens(report.getInspector());
        if (tokens.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            if (!contains(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 项目中是否存在至少一条需要使用第三方样式的报告（用于校验是否填写第三方编号/名称）。
     */
    public static boolean needsThirdPartyAppendix(Project project) {
        if (project == null || project.getReports() == null) {
            return false;
        }
        for (Report r : project.getReports()) {
            if (reportUsesThirdPartyBranding(r)) {
                return true;
            }
        }
        return false;
    }

    /** 调试或文档用：列出某项目下所有解析到的姓名（去重） */
    public static List<String> allInspectorTokens(Project project) {
        if (project == null || project.getReports() == null) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Report r : project.getReports()) {
            set.addAll(splitInspectorTokens(r.getInspector()));
        }
        return new ArrayList<>(set);
    }
}
