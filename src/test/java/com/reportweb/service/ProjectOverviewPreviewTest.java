package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.dto.ProjectOverviewPreviewDTO;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Project;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.util.ExportTextOverrides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class ProjectOverviewPreviewTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WordGeneratorServiceImpl service;
    private com.reportweb.repository.ProjectComponentRepository projectComponentRepository;

    @BeforeEach
    void setUp() {
        ReportComponentMergeHelper mergeHelper = new ReportComponentMergeHelper();
        DetectionContentRowComponentResolver rowResolver =
                new DetectionContentRowComponentResolver(mergeHelper, MAPPER);
        DefectDetectionService defectDetectionService =
                new DefectDetectionService(MAPPER, null, null, null, rowResolver);
        DetectionContentNarrativeService narrativeService = new DetectionContentNarrativeService(MAPPER);
        projectComponentRepository = Mockito.mock(com.reportweb.repository.ProjectComponentRepository.class);
        service = new WordGeneratorServiceImpl(
                MAPPER, null, projectComponentRepository, null, null, defectDetectionService, null, null,
                mergeHelper, rowResolver, null, narrativeService, null);
    }

    private static Project projectWithReports(List<Report> reports) {
        Project project = new Project();
        project.setId(1);
        project.setProjectNumber("PRJ-001");
        project.setProjectName("测试项目");
        project.setCustomerName("测试客户");
        project.setDescription("项目概述描述正文。");
        project.setReports(reports);
        return project;
    }

    private static Report utReport(int id, String componentName, String category, boolean defect) {
        ExperimentType ut = new ExperimentType();
        ut.setId(1);
        ut.setCode("UT");
        ut.setName("超声波检测");

        ReportItem item = new ReportItem();
        item.setExperimentType(ut);
        if (defect) {
            item.setTableData(
                    "{\"rows\":[{\"位置\":\"A-1\",\"波幅\":\"-12\",\"深度\":\"5.2\","
                            + "\"长度\":\"3\",\"高度\":\"1.1\",\"级别\":\"Ⅱ\"}]}");
        } else {
            item.setTableData("{\"rows\":[]}");
        }

        Report report = new Report();
        report.setId(id);
        report.setComponentName(componentName);
        report.setReportNumber("UT-" + id);
        report.setHasDefect(defect ? "是" : "否");
        report.setReportItems(List.of(item));

        ProjectComponent comp = new ProjectComponent();
        comp.setId(id);
        comp.setProjectId(1);
        comp.setComponentName(componentName);
        comp.setCategory(category);
        report.setProjectComponentId(comp.getId());
        return report;
    }

    private void stubComponents(ProjectComponent... components) {
        when(projectComponentRepository.findByProjectId(1)).thenReturn(List.of(components));
        Map<Integer, ProjectComponent> byId = new LinkedHashMap<>();
        for (ProjectComponent c : components) {
            byId.put(c.getId(), c);
        }
        when(projectComponentRepository.findById(anyInt())).thenAnswer(inv -> {
            Integer compId = inv.getArgument(0);
            return Optional.ofNullable(byId.get(compId));
        });
    }

    @Test
    void buildProjectOverviewPreview_noDefect_hidesChapter2() {
        Report report = utReport(1, "主蒸汽管道", "四大管道", false);
        stubComponents(component(1, "主蒸汽管道", "四大管道"));
        Project project = projectWithReports(List.of(report));

        ProjectOverviewPreviewDTO preview = service.buildProjectOverviewPreview(project);

        assertFalse(preview.isShowChapter2());
        assertEquals("项目概述描述正文。", preview.getSection1Body());
        assertTrue(preview.getAbstractParagraph().contains("测试客户"));
        assertEquals(1, preview.getCategories().size());
        assertTrue(preview.getCategories().get(0).getChapter2Components().isEmpty());
        assertFalse(preview.getCategories().get(0).getChapter3Components().isEmpty());
    }

    @Test
    void buildProjectOverviewPreview_withDefect_showsChapter2Item() {
        Report report = utReport(2, "主蒸汽管道", "四大管道", true);
        stubComponents(component(2, "主蒸汽管道", "四大管道"));
        Project project = projectWithReports(List.of(report));

        ProjectOverviewPreviewDTO preview = service.buildProjectOverviewPreview(project);

        assertTrue(preview.isShowChapter2());
        var ch2Items = preview.getCategories().get(0).getChapter2Components().get(0).getItems();
        assertEquals(1, ch2Items.size());
        assertTrue(ch2Items.get(0).getText().contains("存在缺陷"));
        assertTrue(ch2Items.get(0).getNumber().startsWith("2.1.1.1"));
    }

    @Test
    void buildProjectOverviewPreview_savedDefectOverride_appearsInChapter2() {
        Report report = utReport(3, "主蒸汽管道", "四大管道", false);
        stubComponents(component(3, "主蒸汽管道", "四大管道"));
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(ExportTextOverrides.OVERVIEW_DEFECT_LINE, "用户自定义缺陷概述句。");
        report.setCustomFields(Map.of(ExportTextOverrides.CUSTOM_FIELDS_KEY, overrides));
        Project project = projectWithReports(List.of(report));

        ProjectOverviewPreviewDTO preview = service.buildProjectOverviewPreview(project);

        assertTrue(preview.isShowChapter2());
        String text = preview.getCategories().get(0).getChapter2Components().get(0).getItems().get(0).getText();
        assertEquals("用户自定义缺陷概述句。", text);
    }

    @Test
    void buildProjectOverviewPreview_categoryOrder_matchesOverviewOrder() {
        ProjectComponent c10 = component(10, "水冷壁", "锅炉本体");
        ProjectComponent c11 = component(11, "主汽门", "汽机");
        stubComponents(c10, c11);
        Report boiler = utReport(10, "水冷壁", "锅炉本体", false);
        Report turbine = utReport(11, "主汽门", "汽机", false);
        Project project = projectWithReports(List.of(boiler, turbine));

        ProjectOverviewPreviewDTO preview = service.buildProjectOverviewPreview(project);

        assertEquals(2, preview.getCategories().size());
        assertEquals("汽机", preview.getCategories().get(0).getCategory());
        assertEquals("锅炉本体", preview.getCategories().get(1).getCategory());
    }

    private static ProjectComponent component(int id, String name, String category) {
        ProjectComponent comp = new ProjectComponent();
        comp.setId(id);
        comp.setProjectId(1);
        comp.setComponentName(name);
        comp.setCategory(category);
        return comp;
    }
}
