package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.repository.ExperimentTypeRepository;
import com.reportweb.repository.ImageRepository;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.repository.ReportRepository;
import com.reportweb.service.ndt.NdtQualificationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WordGeneratorServiceImpl#reportWordSegmentCount}：仅 UT/PAUT 多行拆段，多部件单报告不拆段。
 */
@ExtendWith(MockitoExtension.class)
class ReportWordSegmentCountTest {

    @Mock
    private DetectionContentNarrativeService detectionContentNarrativeService;

    private WordGeneratorServiceImpl wordGeneratorService;

    @BeforeEach
    void setUp() {
        wordGeneratorService = new WordGeneratorServiceImpl(
                new ObjectMapper(),
                mock(ImageRepository.class),
                mock(ProjectComponentRepository.class),
                mock(MaterialPropertyService.class),
                mock(DataComparisonService.class),
                mock(DefectDetectionService.class),
                mock(ExperimentTypeRepository.class),
                mock(ReportRepository.class),
                mock(ReportComponentMergeHelper.class),
                mock(DetectionContentRowComponentResolver.class),
                mock(NdtQualificationRegistry.class),
                detectionContentNarrativeService,
                mock(AatDataComparisonService.class));
    }

    @Test
    void reportWordSegmentCount_mtMultiComponent_returnsOne() throws Exception {
        Report report = multiRowTableReport(List.of(10, 20));
        ExperimentType mt = typeWithCode("MT");
        when(detectionContentNarrativeService.countDetectionContentTableRows(report)).thenReturn(2);

        assertEquals(1, invokeReportWordSegmentCount(report, mt));
    }

    @Test
    void reportWordSegmentCount_utMultiRow_returnsRowCount() throws Exception {
        Report report = multiRowTableReport(List.of(10, 20));
        ExperimentType ut = typeWithCode("UT");
        when(detectionContentNarrativeService.countDetectionContentTableRows(report)).thenReturn(2);

        assertEquals(2, invokeReportWordSegmentCount(report, ut));
    }

    @Test
    void reportWordSegmentCount_pautMultiRow_returnsRowCount() throws Exception {
        Report report = multiRowTableReport(List.of(10, 20));
        ExperimentType paut = typeWithCode("PAUT");
        when(detectionContentNarrativeService.countDetectionContentTableRows(report)).thenReturn(3);

        assertEquals(3, invokeReportWordSegmentCount(report, paut));
    }

    @Test
    void joinOverviewNarrativeSegments_mergesWithSemicolon() throws Exception {
        Method m = WordGeneratorServiceImpl.class.getDeclaredMethod("joinOverviewNarrativeSegments", List.class);
        m.setAccessible(true);
        String combined = (String) m.invoke(null, List.of("段一。", "段二。"));
        assertEquals("段一；段二。", combined);
    }

    private int invokeReportWordSegmentCount(Report report, ExperimentType experimentType) throws Exception {
        Method m = WordGeneratorServiceImpl.class.getDeclaredMethod(
                "reportWordSegmentCount", Report.class, ExperimentType.class);
        m.setAccessible(true);
        return (int) m.invoke(wordGeneratorService, report, experimentType);
    }

    private static Report multiRowTableReport(List<Integer> componentIds) {
        Report report = new Report();
        report.setId(1);
        report.setProjectComponentIds(componentIds);
        report.setProjectComponentId(componentIds.get(0));
        report.setDetectionContent(Map.of(
                "mode", "table",
                "rows", List.of(
                        Map.of("type", "对接焊缝"),
                        Map.of("type", "角焊缝"))));
        return report;
    }

    private static ExperimentType typeWithCode(String code) {
        ExperimentType et = new ExperimentType();
        et.setCode(code);
        return et;
    }
}
