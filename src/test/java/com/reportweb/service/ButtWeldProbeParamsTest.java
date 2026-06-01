package com.reportweb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 对接焊缝 UT 探头参数查表（反射调用 WordGeneratorServiceImpl 私有方法）。
 */
class ButtWeldProbeParamsTest {

    private Object service;
    private Method getButtWeldProbeParams;
    private Method formatButtWeldProbeParamLine;

    @BeforeEach
    void setUp() throws Exception {
        service = new WordGeneratorServiceImpl(
            null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        getButtWeldProbeParams = WordGeneratorServiceImpl.class.getDeclaredMethod(
            "getButtWeldProbeParams", Double.class, Double.class);
        getButtWeldProbeParams.setAccessible(true);
        formatButtWeldProbeParamLine = WordGeneratorServiceImpl.class.getDeclaredMethod(
            "formatButtWeldProbeParamLine", int.class,
            Class.forName("com.reportweb.service.WordGeneratorServiceImpl$ButtWeldProbeParams"));
        formatButtWeldProbeParamLine.setAccessible(true);
    }

    private Object lookup(double pd, double wt) throws Exception {
        return getButtWeldProbeParams.invoke(service, pd, wt);
    }

    private String formatLine(int probe, Object params) throws Exception {
        return (String) formatButtWeldProbeParamLine.invoke(service, probe, params);
    }

    @Test
    void zone1_thinLargePipe() throws Exception {
        Object p = lookup(600, 20);
        assertNotNull(p);
        assertEquals("1号探头(5MHz 晶片 9×9mm 前沿 10mm k值 1.51)", formatLine(1, p));
        assertEquals("2号探头(5MHz 晶片 9×9mm 前沿 11mm k值 3.01)", formatLine(2, p));
    }

    @Test
    void zone2_mediumThickPipe() throws Exception {
        Object p = lookup(300, 45);
        assertNotNull(p);
        assertEquals("1号探头(2.5MHz 晶片 13×13mm 前沿 12mm k值 1.01)", formatLine(1, p));
        assertEquals("2号探头(2.5MHz 晶片 13×13mm 前沿 12mm k值 2.01)", formatLine(2, p));
    }

    @Test
    void zone3_smallPipe() throws Exception {
        Object p = lookup(40, 6);
        assertNotNull(p);
        assertEquals("1号探头(5MHz 晶片 6×6mm 前沿 5.1mm k值 2.51)", formatLine(1, p));
        assertEquals("2号探头(5MHz 晶片 6×6mm 前沿 5.2mm k值 3.03)", formatLine(2, p));
    }

    @Test
    void zone2_boundaryPd160() throws Exception {
        Object p = lookup(160, 10);
        assertNotNull(p);
        assertEquals("1号探头(5MHz 晶片 9×9mm 前沿 10mm k值 1.51)", formatLine(1, p));
    }

    @Test
    void zone3_wallThickness8UsesFirstBand() throws Exception {
        Object p = lookup(80, 8);
        assertNotNull(p);
        assertEquals("1号探头(5MHz 晶片 6×6mm 前沿 5.1mm k值 2.51)", formatLine(1, p));
    }

    @Test
    void zone3_wallThickness15UsesSecondBand() throws Exception {
        Object p = lookup(80, 15);
        assertNotNull(p);
        assertEquals("1号探头(5MHz 晶片 6×6mm 前沿 5.1mm k值 2.02)", formatLine(1, p));
    }

    @Test
    void outOfRangeReturnsNull() throws Exception {
        assertNull(lookup(20, 6));
        assertNull(lookup(80, 3));
    }
}
