package com.reportweb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.util.ExportTextOverrides;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionContentNarrativeServiceTest {

    private final DetectionContentNarrativeService service = new DetectionContentNarrativeService(new ObjectMapper());

    @Test
    void extractNarrativeBodyFromWordDetectionCell_stripsCellLabels() {
        assertEquals("超声检测支架，总计数量为10个。",
                service.extractNarrativeBodyFromWordDetectionCell(
                        "检测部位:\n    超声检测支架，总计数量为10个。"));
        assertEquals("磁粉检测正文。",
                service.extractNarrativeBodyFromWordDetectionCell("检测内容：\n  磁粉检测正文。"));
        assertEquals("", service.extractNarrativeBodyFromWordDetectionCell("检测部位："));
    }

    @Test
    void normalizeExportDetectionNarrativeBody_stripsRepeatedLocationLabel() {
        String duplicated = "检测部位:\n    检测部位:\n    叙述正文。";
        assertEquals("叙述正文。", service.normalizeExportDetectionNarrativeBody(duplicated));
        assertFalse(service.normalizeExportDetectionNarrativeBody(duplicated).startsWith("检测部位"));
    }

    @Test
    void wordCellAssembly_doesNotDuplicateDetectionLocationLabel() {
        String overrideWithTitle = "检测部位:\n    氧化皮堆积检测，总计数量为1个。";
        String body = service.normalizeExportDetectionNarrativeBody(overrideWithTitle);
        String cell = body.isEmpty() ? "检测部位：" : "检测部位:\n    " + body;
        assertEquals("检测部位:\n    氧化皮堆积检测，总计数量为1个。", cell);
        assertFalse(cell.matches("(?s).*检测部位[:：]\\s*\\r?\\n\\s*检测部位[:：].*"));
    }

    @Test
    void leebHardness_boltTypeWithDetectionPart_stripsLocationQualifierOnly() {
        ExperimentType et = new ExperimentType();
        et.setCode("LHD");
        et.setName("里氏硬度检测");
        String out = service.buildDetectionDetailText(
                "里氏硬度检测", "高压螺栓", "螺栓（检测部位：端部）",
                "10", "", "A1", true, true, et, null, null);
        assertFalse(out.contains("检测部位"), "应去掉类型中的检测部位括注");
        assertFalse(out.contains("螺栓（检测部位"), "不应保留括注原文");
        assertTrue(out.contains("里氏硬度检测高压螺栓"), "部件名已含螺栓时不应再赘连类型中的螺栓");
        assertTrue(out.contains("总计数量为10"));
        assertTrue(out.contains("编号：A1"));
    }

    @Test
    void leebHardness_boltTypeWithLocation_keepsBoltWhenNotInComponentName() {
        ExperimentType et = new ExperimentType();
        et.setCode("LHD");
        et.setName("里氏硬度检测");
        String out = service.buildDetectionDetailText(
                "里氏硬度检测", "高压阀门", "螺栓（检测部位：端部）",
                "5", "", "N1", true, true, et, null, null);
        assertFalse(out.contains("检测部位"));
        assertTrue(out.contains("高压阀门螺栓"), "部件名无螺栓时应保留类型中的「螺栓」");
        assertTrue(out.contains("总计数量为5"));
    }

    @Test
    void leebHardness_plainBolt_omitsTypeFromPrefix() {
        ExperimentType et = new ExperimentType();
        et.setCode("LHD");
        et.setName("里氏硬度检测");
        String out = service.buildDetectionDetailText(
                "里氏硬度检测", "螺母", "螺栓", "1", "", "", true, true, et, null, null);
        assertFalse(out.contains("螺母螺栓"), "里氏+螺栓类型时不应在部件名后接「螺栓」");
        assertTrue(out.contains("里氏硬度检测螺母"));
    }

    @Test
    void leebHardness_pipeJoint_keepsTypeInPrefix() {
        ExperimentType et = new ExperimentType();
        et.setCode("LHD");
        et.setName("里氏硬度检测");
        String out = service.buildDetectionDetailText(
                "里氏", "法兰", "管件/对接焊缝", "3", "", "1", true, true, et, null, null);
        assertTrue(out.contains("管件/对接焊缝"));
    }

    @Test
    void overviewFallback_byExperimentTypeNameAndCode_stripsLocationOnly() {
        String out = service.buildDetectionDetailText(
                "里氏硬度检测", "M24螺栓", "螺栓（检测部位：腰部）",
                "2", "", "B", true, true, null, "里氏硬度检测", "LHD");
        assertFalse(out.contains("检测部位"));
        assertTrue(out.contains("M24螺栓"));
        assertTrue(out.contains("总计数量为2"));
    }

    @Test
    void nonLeeb_boltType_keepsTypeInPrefix() {
        ExperimentType ut = new ExperimentType();
        ut.setCode("UTM");
        ut.setName("超声检测");
        String out = service.buildDetectionDetailText(
                "超声检测", "支架", "螺栓", "1", "", "", true, true, ut, null, null);
        assertTrue(out.contains("螺栓"));
    }

    @Test
    void nonLeeb_boltTypeWithLocationQualifier_stripsParenForConcat() {
        ExperimentType ut = new ExperimentType();
        ut.setCode("UTM");
        ut.setName("超声检测");
        String out = service.buildDetectionDetailText(
                "超声检测", "阀体", "螺栓（检测部位：端部）", "1", "", "", true, true, ut, null, null);
        assertFalse(out.contains("检测部位"), "非里氏叙述拼接应去掉类型括注");
        assertFalse(out.contains("螺栓（检测部位"));
        assertTrue(out.contains("超声检测阀体螺栓"));
    }

    @Test
    void paut_typeWithSubtypeQualifier_stripsParenForConcat() {
        ExperimentType paut = new ExperimentType();
        paut.setCode("PAUT");
        paut.setName("相控阵超声波检测");
        String out = service.buildDetectionDetailText(
                "相控阵超声波检测", "高压转子", "叶根（叉形、菌形）",
                "2", "焊缝区域", "P1", true, true, paut, null, null);
        assertTrue(out.contains("叶根"));
        assertFalse(out.contains("叉形"));
        assertFalse(out.contains("菌形"));
        assertTrue(out.contains("相控阵超声波检测高压转子叶根"));
    }

    @Test
    void buildDetectionContentNarrativeSingleRow_usesPerRowOverrideOnly() {
        ExperimentType ut = new ExperimentType();
        ut.setCode("UT");
        ut.setName("超声波检测");
        Report report = new Report();
        report.setComponentName("部件A");
        report.setDetectionContent(Map.of(
                "mode", "table",
                "rows", List.of(
                        Map.of("type", "对接焊缝", "total", "1"),
                        Map.of("type", "弯头", "total", "2")
                )));
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put(ExportTextOverrides.DETECTION_NARRATIVE_BODY, "仅第二段自定义");
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(ExportTextOverrides.BY_CONTENT_ROW, Map.of("1", row1));
        report.setCustomFields(Map.of(ExportTextOverrides.CUSTOM_FIELDS_KEY, overrides));

        String row0 = service.buildDetectionContentNarrativeSingleRow(report, ut, 0);
        String row1Text = service.buildDetectionContentNarrativeSingleRow(report, ut, 1);

        assertFalse(row0.contains("仅第二段自定义"));
        assertEquals("仅第二段自定义", row1Text);
    }

    @Test
    void normalizeExportDetectionNarrativeBody_stripsFigureReference() {
        String raw = "检测内容：\n  超声检测支架，总计数量为10个。\n  详见附图";
        String out = service.normalizeExportDetectionNarrativeBody(raw);
        assertFalse(out.contains(DetectionContentNarrativeService.FIGURE_REFERENCE_PHRASE));
        assertTrue(out.contains("超声检测支架"));
    }

    @Test
    void buildDetectionDetailText_subsequentRowPrefixTypeOnly() {
        ExperimentType mt = new ExperimentType();
        mt.setCode("MT");
        mt.setName("磁粉检测");
        String out = service.buildDetectionDetailText(
                "磁粉检测", "阀体", "弯头", "2", "", "", false, false, mt, null, null);
        assertTrue(out.startsWith("弯头"));
        assertFalse(out.contains("阀体弯头"), "后续行前缀不应重复部件名");
        assertFalse(out.startsWith("磁粉检测"));
    }

    @Test
    void buildDetectionContentNarrativeBody_multiRow_omitsComponentOnSubsequentRows() {
        ExperimentType mt = new ExperimentType();
        mt.setCode("MT");
        mt.setName("磁粉检测");
        Report report = new Report();
        report.setTestMethod("磁粉检测");
        report.setComponentName("阀体");
        report.setDetectionContent(Map.of(
                "mode", "table",
                "rows", List.of(
                        Map.of("type", "对接焊缝", "total", "1"),
                        Map.of("type", "弯头", "total", "2")
                )));

        String body = service.buildDetectionContentNarrativeBody(report, mt);

        assertTrue(body.contains("磁粉检测阀体对接焊缝"));
        assertTrue(body.contains("弯头"));
        assertFalse(body.contains("阀体弯头"), "第二段不应在类型前重复部件名");
        assertTrue(body.contains("；"), "多行应以分号连接");
    }

    @Test
    void appendFigureSuffixIfAbsent_onlyWhenMissing() {
        String base = "检测部位:\n    叙述正文。";
        String once = DetectionContentNarrativeService.appendFigureSuffixIfAbsent(base, true);
        assertTrue(once.endsWith(DetectionContentNarrativeService.FIGURE_REFERENCE_PHRASE));
        String twice = DetectionContentNarrativeService.appendFigureSuffixIfAbsent(once, true);
        assertEquals(once, twice);
        assertFalse(DetectionContentNarrativeService.appendFigureSuffixIfAbsent(base, false)
                .contains(DetectionContentNarrativeService.FIGURE_REFERENCE_PHRASE));
    }
}
