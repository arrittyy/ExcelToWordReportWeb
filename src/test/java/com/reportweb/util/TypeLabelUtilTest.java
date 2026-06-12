package com.reportweb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeLabelUtilTest {

    @Test
    void stripParentheticalQualifiers_fullwidthAndHalfwidth() {
        assertEquals("螺栓", TypeLabelUtil.stripParentheticalQualifiers("螺栓（检测部位：端部）"));
        assertEquals("叶根", TypeLabelUtil.stripParentheticalQualifiers("叶根（叉形、菌形）"));
        assertEquals("叶片", TypeLabelUtil.stripParentheticalQualifiers("叶片（表面波）"));
        assertEquals("bolt", TypeLabelUtil.stripParentheticalQualifiers("bolt(part A)"));
        assertEquals("对接焊缝", TypeLabelUtil.stripParentheticalQualifiers("对接焊缝"));
    }

    @Test
    void effectiveTypeForNarrative_pautRootDashSubtype() {
        assertEquals("叶根", TypeLabelUtil.effectiveTypeForNarrative("叶根-叉形"));
        assertEquals("叶根", TypeLabelUtil.effectiveTypeForNarrative("叶根-菌形"));
        assertEquals("叶根", TypeLabelUtil.effectiveTypeForNarrative("叶根－T形"));
        assertEquals("叶根", TypeLabelUtil.effectiveTypeForNarrative("叶根（叉形、菌形）"));
    }

    @Test
    void pautRootShapeFromType_newSubtypes() {
        assertEquals("叉形", TypeLabelUtil.pautRootShapeFromType("叶根-叉形").orElseThrow());
        assertEquals("菌形", TypeLabelUtil.pautRootShapeFromType("叶根-菌形").orElseThrow());
        assertEquals("T形", TypeLabelUtil.pautRootShapeFromType("叶根-T形").orElseThrow());
        assertEquals("枞树形", TypeLabelUtil.pautRootShapeFromType("叶根-枞树形").orElseThrow());
        assertTrue(TypeLabelUtil.pautRootShapeFromType("叶根（叉形、菌形）").isEmpty());
        assertTrue(TypeLabelUtil.pautRootShapeFromType("叶片").isEmpty());
    }

    @Test
    void pautBoltProbeParamForRootShape() {
        assertEquals(
                "1号探头(7.5MHz 阵元数16 晶片0.5x10mm)",
                TypeLabelUtil.pautBoltProbeParamForRootShape("叉形").orElseThrow());
        assertEquals(
                "1号探头(5MHz 阵元数10 晶片0.5x0.5mm)",
                TypeLabelUtil.pautBoltProbeParamForRootShape("枞树形").orElseThrow());
    }

    @Test
    void isElbowPipeDetectionType_canonicalAndLegacy() {
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯头"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯管"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯头/弯管"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯头弯管"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType(" 弯头 "));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType(" 弯管 "));
        assertFalse(TypeLabelUtil.isElbowPipeDetectionType("对接焊缝"));
        assertFalse(TypeLabelUtil.isElbowPipeDetectionType(null));
        assertFalse(TypeLabelUtil.isElbowPipeDetectionType(""));
    }
}
