package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.repository.ProjectComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.PART_A;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.PART_B;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.SEGMENT_1;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.SEGMENT_2;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.aatReportWithRows;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.assignIdOnSaveAnswer;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.assignIdOnSaveForSecondOnlyAnswer;
import static com.reportweb.service.ProjectComponentSyncServiceTestSupport.contentRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectComponentSyncServiceTest {

    @Mock
    private ProjectComponentRepository projectComponentRepository;

    private final ReportComponentMergeHelper mergeHelper = new ReportComponentMergeHelper();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DetectionContentRowComponentResolver rowComponentResolver =
            new DetectionContentRowComponentResolver(mergeHelper, objectMapper);
    private final ComponentDetailFromDetectionResolver detailResolver =
            new ComponentDetailFromDetectionResolver(mergeHelper, rowComponentResolver, objectMapper);

    private ProjectComponentSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new ProjectComponentSyncService(
                projectComponentRepository,
                mergeHelper,
                detailResolver,
                objectMapper);
    }

    @Test
    void sync_skipsSingleRowReport() {
        Report report = aatReportWithRows(1);
        assertFalse(syncService.syncFromAatReportIfNeeded(report));
        verify(projectComponentRepository, never()).save(any(ProjectComponent.class));
    }

    @Test
    void sync_createsTwoComponentsAndSetsIds() {
        Report report = aatReportWithRows(2);
        List<Map<String, Object>> contentRows = new ArrayList<>();
        contentRows.add(contentRow(PART_A, "20G"));
        contentRows.add(contentRow(PART_B, "12Cr1MoVG"));
        report.getDetectionContent().put("rows", contentRows);

        when(projectComponentRepository.findByProjectId(1)).thenReturn(new ArrayList<>());
        when(projectComponentRepository.save(any(ProjectComponent.class))).thenAnswer(assignIdOnSaveAnswer());

        assertTrue(syncService.syncFromAatReportIfNeeded(report));
        assertEquals(Integer.valueOf(101), report.getProjectComponentId());
        assertNotNull(report.getProjectComponentIds());
        assertEquals(Arrays.asList(101, 102), report.getProjectComponentIds());

        ArgumentCaptor<ProjectComponent> captor = ArgumentCaptor.forClass(ProjectComponent.class);
        verify(projectComponentRepository, times(2)).save(captor.capture());
        assertEquals(PART_A, captor.getAllValues().get(0).getComponentName());
        assertEquals("20G", captor.getAllValues().get(0).getMaterial());
    }

    @Test
    void sync_reusesExistingComponentByMatch() {
        Report report = aatReportWithRows(2);
        List<Map<String, Object>> contentRows = new ArrayList<>();
        contentRows.add(contentRow(SEGMENT_1, "20G"));
        contentRows.add(contentRow(SEGMENT_2, "12Cr1MoVG"));
        report.getDetectionContent().put("rows", contentRows);

        ProjectComponent existing = new ProjectComponent();
        existing.setId(50);
        existing.setProjectId(1);
        existing.setComponentName(SEGMENT_1);
        existing.setMaterial("20G");

        when(projectComponentRepository.findByProjectId(1))
                .thenReturn(new ArrayList<>(Arrays.asList(existing)));
        when(projectComponentRepository.save(any(ProjectComponent.class)))
                .thenAnswer(assignIdOnSaveForSecondOnlyAnswer());

        assertTrue(syncService.syncFromAatReportIfNeeded(report));
        assertEquals(Arrays.asList(50, 51), report.getProjectComponentIds());
    }
}
