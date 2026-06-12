package com.reportweb.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 单项 Word 报告中的日期展示格式（检测日期、编制/审核/批准日期等）。
 */
public final class ReportWordDateFormat {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private ReportWordDateFormat() {
    }

    public static String format(LocalDate date, String defaultValue) {
        return date != null ? date.format(FORMATTER) : defaultValue;
    }
}
