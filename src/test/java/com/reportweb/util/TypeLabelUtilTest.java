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
    void isElbowPipeDetectionType_canonicalAndLegacy() {
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯头"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯头/弯管"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType("弯头弯管"));
        assertTrue(TypeLabelUtil.isElbowPipeDetectionType(" 弯头 "));
        assertFalse(TypeLabelUtil.isElbowPipeDetectionType("对接焊缝"));
        assertFalse(TypeLabelUtil.isElbowPipeDetectionType(null));
        assertFalse(TypeLabelUtil.isElbowPipeDetectionType(""));
    }
}
