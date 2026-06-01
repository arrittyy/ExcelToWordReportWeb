package com.reportweb.util;

import com.reportweb.entity.Report;

import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 单项报告「记录编号」：报告编号 BG→JL；润电内部检测人员在末段序号前插入人员代码（保留序号前导零）。
 */
public final class RecordNumberFormatter {

  private static final Pattern TRAILING_SEQ = Pattern.compile("^(.*-)(\\d+)$");

  private RecordNumberFormatter() {
  }

  public static String format(String reportNumber, Report report) {
    if (reportNumber == null || reportNumber.trim().isEmpty()) {
      return "/";
    }
    String jl = reportNumber.replace("BG", "JL");
    if (report == null || InternalInspectorWhitelist.reportUsesThirdPartyBranding(report)) {
      return jl;
    }
    OptionalInt code = firstInspectorCode(report);
    if (code.isEmpty()) {
      return jl;
    }
    Matcher m = TRAILING_SEQ.matcher(jl);
    if (!m.matches()) {
      return jl;
    }
    String prefix = m.group(1);
    String seq = m.group(2);
    return prefix + code.getAsInt() + seq;
  }

  private static OptionalInt firstInspectorCode(Report report) {
    List<String> tokens = InternalInspectorWhitelist.splitInspectorTokens(report.getInspector());
    for (String name : tokens) {
      OptionalInt code = RunDianPersonnelRegistry.codeOf(name);
      if (code.isPresent()) {
        return code;
      }
    }
    return OptionalInt.empty();
  }
}
