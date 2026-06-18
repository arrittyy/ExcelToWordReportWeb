package com.reportweb.service;

import com.reportweb.entity.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 总报告「报告日期」：无损/理化批准日期中的较晚者。
 */
class ResolveProjectReportDateTest {

    private Object service;
    private Method resolveProjectReportDate;

    @BeforeEach
    void setUp() throws Exception {
        service = new WordGeneratorServiceImpl(
            null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        resolveProjectReportDate = WordGeneratorServiceImpl.class.getDeclaredMethod(
            "resolveProjectReportDate", Project.class);
        resolveProjectReportDate.setAccessible(true);
    }

    private LocalDate resolve(Project project) throws Exception {
        return (LocalDate) resolveProjectReportDate.invoke(service, project);
    }

    @Test
    void bothPresent_returnsLaterDate() throws Exception {
        Project project = new Project();
        project.setApprovalDateNdt(LocalDate.of(2026, 3, 10));
        project.setApprovalDateChem(LocalDate.of(2026, 6, 15));
        assertEquals(LocalDate.of(2026, 6, 15), resolve(project));
    }

    @Test
    void bothPresent_ndtLater_returnsNdt() throws Exception {
        Project project = new Project();
        project.setApprovalDateNdt(LocalDate.of(2026, 8, 1));
        project.setApprovalDateChem(LocalDate.of(2026, 5, 1));
        assertEquals(LocalDate.of(2026, 8, 1), resolve(project));
    }

    @Test
    void onlyNdt_returnsNdt() throws Exception {
        Project project = new Project();
        project.setApprovalDateNdt(LocalDate.of(2026, 4, 20));
        assertEquals(LocalDate.of(2026, 4, 20), resolve(project));
    }

    @Test
    void onlyChem_returnsChem() throws Exception {
        Project project = new Project();
        project.setApprovalDateChem(LocalDate.of(2026, 7, 8));
        assertEquals(LocalDate.of(2026, 7, 8), resolve(project));
    }

    @Test
    void neitherPresent_returnsNull() throws Exception {
        assertNull(resolve(new Project()));
    }
}
