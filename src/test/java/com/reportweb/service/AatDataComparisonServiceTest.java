package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.repository.ProjectComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AatDataComparisonServiceTest {

    @Mock
    private ProjectComponentRepository projectComponentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataComparisonService dataComparisonService = new DataComparisonService(objectMapper);
    private final MaterialPropertyService materialPropertyService = new MaterialPropertyService(null);
    private final ReportComponentMergeHelper mergeHelper = new ReportComponentMergeHelper();

    private AatDataComparisonService aatService;

    @BeforeEach
    void setUp() {
        DetectionContentRowComponentResolver rowComponentResolver =
                new DetectionContentRowComponentResolver(mergeHelper, objectMapper);
        ComponentDetailFromDetectionResolver detailResolver =
                new ComponentDetailFromDetectionResolver(mergeHelper, rowComponentResolver, objectMapper);
        aatService = new AatDataComparisonService(
                objectMapper,
                dataComparisonService,
                materialPropertyService,
                mergeHelper,
                projectComponentRepository,
                detailResolver);
    }

    @Test
    void perBlock_onlyOutOfRangeBlockProducesNonCompliance() throws Exception {
        String tableData = objectMapper.writeValueAsString(Map.of(
                "perContentRow", List.of(
                        Map.of("rows", List.of(Map.of("编号", "1", "Mn", "0.50"))),
                        Map.of("rows", List.of(Map.of("编号", "2", "Mn", "9.99")))
                )
        ));

        Map<String, Object> dc = new HashMap<>();
        dc.put("mode", "table");
        dc.put("rows", List.of(
                Map.of("材质", "20G"),
                Map.of("材质", "12Cr1MoVG")
        ));

        Report report = new Report();
        report.setDetectionContent(dc);
        ReportItem item = new ReportItem();
        item.setTableData(tableData);
        report.setReportItems(List.of(item));

        List<DataComparisonService.NonComplianceRecord> records =
                aatService.computeNonComplianceRecords(report);

        assertFalse(records.isEmpty());
        assertTrue(records.stream().anyMatch(r -> "2".equals(r.getNumber()) && "Mn".equals(r.getItemName())));
        assertFalse(records.stream().anyMatch(r -> "1".equals(r.getNumber())));
    }

    @Test
    void distinctMaterials_returnsTwoEntries() {
        Report report = new Report();
        Map<String, Object> dc = new HashMap<>();
        dc.put("mode", "table");
        dc.put("rows", List.of(Map.of("材质", "20G"), Map.of("材质", "12Cr1MoVG")));
        report.setDetectionContent(dc);

        List<String> materials = aatService.distinctMaterialsForReport(report, List.of());
        assertEquals(2, materials.size());
        assertTrue(materials.contains("20G"));
        assertTrue(materials.contains("12Cr1MoVG"));
    }

}
