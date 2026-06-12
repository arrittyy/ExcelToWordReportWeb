package com.reportweb.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportWordDateFormatTest {

    @Test
    void format_outputsDotSeparatedDate() {
        assertEquals("2025.10.10", ReportWordDateFormat.format(LocalDate.of(2025, 10, 10), "/"));
    }

    @Test
    void format_nullReturnsDefault() {
        assertEquals("/", ReportWordDateFormat.format(null, "/"));
        assertEquals("", ReportWordDateFormat.format(null, ""));
    }
}
