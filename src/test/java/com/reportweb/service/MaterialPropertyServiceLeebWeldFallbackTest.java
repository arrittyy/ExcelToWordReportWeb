package com.reportweb.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialPropertyServiceLeebWeldFallbackTest {

    private final MaterialPropertyService service = new MaterialPropertyService();

    @Test
    void parseBrinell_rangeWithWaveDash() {
        Optional<double[]> r = MaterialPropertyService.parseBrinellToMinMax("125～195");
        assertTrue(r.isPresent());
        assertArrayEquals(new double[]{125, 195}, r.get(), 1e-9);
    }

    @Test
    void parseBrinell_upperBoundOnly() {
        Optional<double[]> r = MaterialPropertyService.parseBrinellToMinMax("≤179");
        assertTrue(r.isPresent());
        assertArrayEquals(new double[]{179, 179}, r.get(), 1e-9);
    }

    @Test
    void parseBrinell_upperBoundAscii() {
        Optional<double[]> r = MaterialPropertyService.parseBrinellToMinMax("<=179");
        assertTrue(r.isPresent());
        assertArrayEquals(new double[]{179, 179}, r.get(), 1e-9);
    }

    @Test
    void parseBrinell_hyphenRange() {
        Optional<double[]> r = MaterialPropertyService.parseBrinellToMinMax("125-195");
        assertTrue(r.isPresent());
        assertArrayEquals(new double[]{125, 195}, r.get(), 1e-9);
    }

    @Test
    void parseBrinell_rejectsWallThicknessNarrative() {
        assertTrue(MaterialPropertyService.parseBrinellToMinMax("壁厚≥8mm，≤179").isEmpty());
    }

    @Test
    void derivedWeld_noBrinell_usesSteelLeebInterval_likeWb36() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "190～255");
        m.put("里氏-焊缝", "");
        m.put("里氏", "");
        Optional<String> d = service.resolveBrinellDerivedLeebWeldRange(m);
        assertTrue(d.isPresent());
        assertEquals("171～270", d.get());
        assertEquals("171～270", service.resolveLeebWeldRangeForComparison(m));
    }

    @Test
    void derivedWeld_brinellNarrative_fallsBackToSteelLeeb() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "190～255");
        m.put("里氏-焊缝", "");
        m.put("里氏", "");
        m.put("布氏", "壁厚≥8mm，≤179");
        Optional<String> d = service.resolveBrinellDerivedLeebWeldRange(m);
        assertTrue(d.isPresent());
        assertEquals("171～270", d.get());
    }

    @Test
    void wb36_fromStaticLibrary_derivesWeldFromSteelLeeb() {
        Map<String, String> p = service.getMaterialProperty("WB36");
        assertNotNull(p);
        assertTrue(service.resolveBrinellDerivedLeebWeldRange(p).isPresent());
        assertEquals("171～270", service.resolveLeebWeldRangeForComparison(p));
    }

    @Test
    void derivedWeld_range125to195_yields112point5to270() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "130～197");
        m.put("里氏-管件", "");
        m.put("里氏-焊缝", "");
        m.put("里氏", "");
        m.put("布氏", "125～195");
        Optional<String> d = service.resolveBrinellDerivedLeebWeldRange(m);
        assertTrue(d.isPresent());
        assertEquals("112.5～270", d.get());
        assertEquals("112.5～270", service.resolveLeebWeldRangeForComparison(m));
    }

    @Test
    void derivedWeld_le179_yields161point1to270() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "130～180");
        m.put("里氏-焊缝", "");
        m.put("里氏", "");
        m.put("布氏", "≤179");
        Optional<String> d = service.resolveBrinellDerivedLeebWeldRange(m);
        assertTrue(d.isPresent());
        assertEquals("161.1～270", d.get());
    }

    @Test
    void derivedWeld_ge510_abandoned() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "190～250");
        m.put("里氏-焊缝", "");
        m.put("里氏", "");
        m.put("布氏", "≥510");
        assertTrue(service.resolveBrinellDerivedLeebWeldRange(m).isEmpty());
        assertEquals("", service.resolveLeebWeldRangeForComparison(m));
    }

    @Test
    void noSteelPipe_noDerived() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-管件", "135～195");
        m.put("里氏-焊缝", "");
        m.put("里氏", "");
        m.put("布氏", "125～195");
        assertTrue(service.resolveBrinellDerivedLeebWeldRange(m).isEmpty());
    }

    @Test
    void explicitWeld_noDerived() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "190～255");
        m.put("里氏-焊缝", "185～290");
        m.put("布氏", "125～195");
        assertTrue(service.resolveBrinellDerivedLeebWeldRange(m).isEmpty());
        assertEquals("185～290", service.resolveLeebWeldRangeForComparison(m));
    }

    @Test
    void genericLeeb_noDerived() {
        Map<String, String> m = new HashMap<>();
        m.put("里氏-钢管", "190～255");
        m.put("里氏-焊缝", "");
        m.put("里氏", "180～250");
        m.put("布氏", "125～195");
        assertTrue(service.resolveBrinellDerivedLeebWeldRange(m).isEmpty());
        assertEquals("180～250", service.resolveLeebWeldRangeForComparison(m));
    }

    @Test
    void dl869ConstantPresent() {
        assertFalse(MaterialPropertyService.DL_T869_LEEB_WELD_CONCLUSION_APPEND.isEmpty());
        assertTrue(MaterialPropertyService.DL_T869_LEEB_WELD_CONCLUSION_APPEND.contains("DL/T869-2021"));
    }
}
