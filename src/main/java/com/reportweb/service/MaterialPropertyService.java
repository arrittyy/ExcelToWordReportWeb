package com.reportweb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 材质属性查询服务
 * 提供材质属性数据的查询功能，支持通过GB5310牌号或国外牌号查询
 */
@Service
@Slf4j
public class MaterialPropertyService {

    /**
     * 里氏检测结论中，当焊缝范围由母材布氏推算时，在 DL/T438 前缀后追加的 DL/T869-2021 规程表述。
     */
    public static final String DL_T869_LEEB_WELD_CONCLUSION_APPEND =
            "DL/T869-2021规程要求其焊缝硬度值上限不超过母材布氏硬度值加100HBW且不大于270HBW，下限不应低于母材硬度的90%";

    private static final Pattern FIRST_NUMBER = Pattern.compile("([-+]?\\d+\\.?\\d*)");
    private static final Pattern HYPHEN_BRINELL_RANGE = Pattern.compile(
            "^\\s*([-+]?\\d+\\.?\\d*)\\s*-\\s*([-+]?\\d+\\.?\\d*)\\s*$");

    // ========== 材质属性数据 ==========
    // key: 材质名称（GB5310牌号或国外牌号），value: 属性映射
    private static final Map<String, Map<String, String>> MATERIAL_PROPERTIES = new HashMap<>();
    
    // 初始化材质属性数据（在类加载时执行）
    static {
        initializeMaterialProperties();
        initializeLeebHardnessRanges();
        initializeLeebBoltRanges();
    }
    
    /**
     * 初始化材质属性数据
     */
    private static void initializeMaterialProperties() {
        // GB/T 5310-2023 标准材料
        // 元素标准值格式：如果没有标准值，传空字符串 ""
        // 对于特殊格式如"8C～1.10"、"4C~0.60"（与碳含量相关），留空不填写
        addMaterial("20G", "GB/T 5310-2023", "20G", "", "410～550", "≥245", "≥24", "120～160", "125～170", "",
            "0.35～0.65", "", "", "", "", "", "", "", "", "", "", "", "");
        addMaterial("20MnG", "GB/T 5310-2023", "20MnG", "", "", "", "", "", "", "",
            "0.70～1.00", "", "", "", "", "", "", "", "", "", "", "", "");
        addMaterial("25MnG", "GB/T 5310-2023", "25MnG", "", "", "", "", "", "", "",
            "0.70～1.00", "", "", "", "", "", "", "", "", "", "", "", "");
        addMaterial("15MnG", "GB/T 5310-2023", "15MnG", "", "", "", "", "", "", "",
            "0.70～1.00", "", "", "", "", "", "", "", "", "", "", "", "");
        addMaterial("15MoG", "GB/T 5310-2023", "15MoG", "", "", "", "", "", "", "",
            "0.40～0.80", "", "0.25～0.35", "", "", "", "", "", "", "", "", "", "");
        addMaterial("20MoG", "GB/T 5310-2023", "20MoG", "", "", "", "", "", "", "",
            "0.40～0.70", "", "0.44～0.65", "", "", "", "", "", "", "", "", "", "");
        addMaterial("12CrMoG", "GB/T 5310-2023", "12CrMoG", "", "", "", "", "", "", "",
            "0.40～0.70", "0.40～0.70", "0.40～0.55", "", "", "", "", "", "", "", "", "", "");
        addMaterial("15CrMoG", "GB/T 5310-2023", "15CrMoG", "", "440～640", "≥295", "≥21", "125～195", "130～205", "",
            "0.40～0.70", "0.80～1.10", "0.40～0.55", "", "", "", "", "", "", "", "", "", "");
        addMaterial("12Cr2MoG", "GB/T 5310-2023", "12Cr2MoG", "", "", "", "", "", "", "",
            "0.40～0.60", "2.00～2.50", "0.90～1.13", "", "", "", "", "", "", "", "", "", "");
        addMaterial("T22", "GB/T 5310-2023", "12Cr2MoG", "T22", "", "", "", "", "", "",
            "0.40～0.60", "2.00～2.50", "0.90～1.13", "", "", "", "", "", "", "", "", "", "");
        addMaterial("12Cr1MoV", "GB/T 5310-2023", "12Cr1MoV", "", "", "", "", "", "", "",
            "0.40～0.70", "0.90～1.20", "0.25～0.35", "0.15～0.30", "", "", "", "", "", "", "", "", "");
        addMaterial("12Cr1MoVG", "GB/T 5310-2023", "12Cr1MoVG", "", "470～640", "≥255", "≥21", "135～195", "140～205", "",
            "0.40～0.70", "0.90～1.20", "0.25～0.35", "0.15～0.30", "", "", "", "", "", "", "", "", "");
        addMaterial("12Cr2MoWVTiB", "GB/T 5310-2023", "12Cr2MoWVTiB", "G102", "540～640", "≥255", "≥21", "160～220", "170～230", "85～97",
            "0.45～0.65", "1.60～2.10", "0.50～0.65", "0.28～0.42", "0.08～0.18", "", "", "", "", "0.30～0.55", "", "", "");
        addMaterial("12CrMoMoWVTiB", "GB/T 5310-2023", "12CrMoMoWVTiB", "G102", "540～640", "≥255", "≥21", "160～220", "170～230", "85～97",
            "0.45～0.65", "1.60～2.10", "0.50～0.65", "0.28～0.42", "0.08～0.18", "", "", "", "", "0.30～0.55", "", "", "");
        addMaterial("G102", "GB/T 5310-2023", "12CrMoMoWVTiB", "G102", "", "", "", "", "", "",
            "0.45～0.65", "1.60～2.10", "0.50～0.65", "0.28～0.42", "0.08～0.18", "", "", "", "", "0.30～0.55", "", "", "");
        addMaterial("07Cr2MoW2VNbB", "GB/T 5310-2023", "07Cr2MoW2VNbB", "T23", "≥510", "≥400", "≥22", "150～220", "160～230", "80～97",
            "0.10～0.60", "1.90～2.60", "0.05～0.30", "0.20～0.30", "", "", "0～0.030", "", "0.02～0.08", "1.45～1.75", "", "", "");
        addMaterial("T23", "GB/T 5310-2023", "07Cr2MoW2VNbB", "T23", "≥510", "≥400", "≥22", "150～220", "160～230", "80～97",
            "0.10～0.60", "1.90～2.60", "0.05～0.30", "0.20～0.30", "", "", "0～0.030", "", "0.02～0.08", "1.45～1.75", "", "", "");
        addMaterial("12Cr3MoVSiTiB", "GB/T 5310-2023", "12Cr3MoVSiTiB", "", "", "", "", "", "", "",
            "0.50～0.80", "2.50～3.00", "1.00～1.20", "0.25～0.35", "0.22～0.38", "", "", "", "", "", "", "", "");
        addMaterial("15Ni1MnMoNbCu", "GB/T 5310-2023", "15Ni1MnMoNbCu", "", "", "", "", "", "", "",
            "0.80～1.20", "", "0.25～0.50", "", "", "1.00～1.30", "0～0.050", "0.50～0.80", "0.015～0.045", "", "", "", "");
        addMaterial("10Cr9Mo1VNbN", "GB/T 5310-2023", "10Cr9Mo1VNbN", "T91", "≥585", "≥415", "≥20", "190～250", "200～265", "",
            "0.30～0.60", "8.00～9.50", "0.85～1.05", "0.18～0.25", "", "0～0.40", "0～0.020", "", "0.06～0.10", "", "", "", "");
        addMaterial("SA-335P91", "GB/T 5310-2023", "10Cr9Mo1VNbN", "P91", "", "", "", "", "", "",
            "0.30～0.60", "8.00～9.50", "0.85～1.05", "0.18～0.25", "", "0～0.40", "", "", "0.06～0.10", "", "", "", "");
        addMaterial("P91", "GB/T 5310-2023", "10Cr9Mo1VNbN", "P91", "", "", "", "", "", "",
            "0.30～0.60", "8.00～9.50", "0.85～1.05", "0.18～0.25", "", "0～0.40", "0～0.020", "", "0.06～0.10", "", "", "", "");
        addMaterial("SA-213T91", "GB/T 5310-2023", "10Cr9Mo1VNbN", "T91", "", "", "", "", "", "",
            "0.30～0.60", "8.00～9.50", "0.85～1.05", "0.18～0.25", "", "0～0.40", "0～0.020", "", "0.06～0.10", "", "", "", "");
        addMaterial("T91", "GB/T 5310-2023", "10Cr9Mo1VNbN", "T91", "", "", "", "", "", "",
            "0.30～0.60", "8.00～9.50", "0.85～1.05", "0.18～0.25", "", "0～0.40", "0～0.020", "", "0.06～0.10", "", "", "", "");
        addMaterial("10Cr9MoW2VNbBN", "GB/T 5310-2023", "10Cr9MoW2VNbBN", "T92", "≥620", "≥440", "≥20", "190～250", "200～265", "",
            "0.30～0.60", "8.50～9.50", "0.30～0.60", "0.15～0.25", "", "0～0.40", "0～0.020", "", "0.04～0.09", "1.50～2.00", "", "", "");
        addMaterial("P92", "GB/T 5310-2023", "10Cr9MoW2VNbBN", "P92", "", "", "", "", "", "",
            "0.30～0.60", "8.50～9.50", "0.30～0.60", "0.15～0.25", "", "0～0.40", "0～0.020", "", "0.04～0.09", "1.50～2.00", "", "", "");
        addMaterial("SA-335P92", "GB/T 5310-2023", "10Cr9MoW2VNbBN", "P92", "", "", "", "", "", "",
            "0.30～0.60", "8.50～9.50", "0.30～0.60", "0.15～0.25", "", "0～0.40", "0～0.020", "", "0.04～0.09", "1.50～2.00", "", "", "");
        addMaterial("T92", "GB/T 5310-2023", "10Cr9MoW2VNbBN", "T92", "", "", "", "", "", "",
            "0.30～0.60", "8.50～9.50", "0.30～0.60", "0.15～0.25", "", "0～0.40", "0～0.020", "", "0.04～0.09", "1.50～2.00", "", "", "");
        addMaterial("SA-213T92", "ASME SA-213/SA-213M", "10Cr9MoW2VNbBN", "T92", "", "", "", "", "", "",
            "0.30～0.60", "8.00～9.50", "0.85～1.05", "0.18～0.25", "", "0～0.40", "0～0.020", "0.06～0.10", "0.06～0.10", "", "", "", "");
        addMaterial("10Cr11MoW2VNbCu1BN", "GB/T 5310-2023", "10Cr11MoW2VNbCu1BN", "", "", "", "", "", "", "",
            "0～0.70", "10.00～11.50", "0.25～0.60", "0.15～0.30", "", "0～0.50", "0～0.020", "0.30～1.70", "0.04～0.10", "1.50～2.50", "", "", "");
        addMaterial("11Cr9Mo1W1VNbBN", "GB/T 5310-2023", "11Cr9Mo1W1VNbBN", "", "", "", "", "", "", "",
            "0.30～0.60", "8.50～9.50", "0.90～1.10", "0.18～0.25", "", "0～0.40", "0～0.020", "", "0.06～0.10", "0.90～1.10", "", "", "");
        addMaterial("07Cr19Ni10", "GB/T 5310-2023", "07Cr19Ni10", "TP304H", "", "", "", "", "", "",
            "0～2.00", "18.00～20.00", "", "", "", "8.00～11.00", "", "", "", "", "", "", "");
        addMaterial("TP304H", "GB/T 5310-2023", "07Cr19Ni10", "TP304H", "", "", "", "", "", "",
            "0～2.00", "18.00～20.00", "", "", "", "8.00～11.00", "", "", "", "", "", "", "");
        addMaterial("10Cr18Ni9NbCu3BN", "GB/T 5310-2023", "10Cr18Ni9NbCu3BN", "S30432/SUPER304H", "≥590", "≥235", "≥35", "150～219", "160～230", "80～95",
            "0～1.00", "17.00～19.00", "", "", "", "7.50～10.50", "0.003～0.030", "0.30～0.60", "", "", "", "", "");
        addMaterial("S30432", "GB/T 5310-2023", "10Cr18Ni9NbCu3BN", "S30432/SUPER304H", "", "", "", "", "", "",
            "0～1.00", "17.00～19.00", "", "", "", "7.50～10.50", "0.003～0.030", "0.30～0.60", "", "", "", "", "");
        addMaterial("SUPER304H", "GB/T 5310-2023", "10Cr18Ni9NbCu3BN", "S30432/SUPER304H", "", "", "", "", "", "",
            "0～1.00", "17.00～19.00", "", "", "", "7.50～10.50", "0.003～0.030", "0.30～0.60", "", "", "", "", "");
        addMaterial("07Cr25Ni21", "GB/T 5310-2023", "07Cr25Ni21", "", "", "", "", "", "", "",
            "0～2.00", "24.00～26.00", "", "", "", "19.00～22.00", "", "", "", "", "", "", "");
        addMaterial("07Cr25Ni21NbN", "GB/T 5310-2023", "07Cr25Ni21NbN", "TP310HCbN/HR3C", "≥655", "≥295", "≥30", "150～256", "160～270", "80～100",
            "0～2.00", "24.00～26.00", "", "", "", "19.00～22.00", "", "", "0.20～0.60", "", "", "", "");
        addMaterial("HR3C", "GB/T 5310-2023", "07Cr25Ni21NbN", "TP310HCbN/HR3C", "", "", "", "", "", "",
            "0～2.00", "24.00～26.00", "", "", "", "19.00～22.00", "", "", "0.20～0.60", "", "", "", "");
        addMaterial("TP310CbN", "GB/T 5310-2023", "07Cr25Ni21NbN", "TP310HCbN/HR3C", "", "", "", "", "", "",
            "0～2.00", "24.00～26.00", "", "", "", "19.00～22.00", "", "", "0.20～0.60", "", "", "", "");
        addMaterial("07Cr19Ni11Ti", "GB/T 5310-2023", "07Cr19Ni11Ti", "", "", "", "", "", "", "",
            "0～2.00", "17.00～20.00", "", "", "", "9.00～13.00", "", "", "", "", "", "", "");
        addMaterial("TP347H", "GB/T 5310-2023", "07Cr18Ni11Nb", "TP347H", "", "", "", "", "", "",
            "0～2.00", "17.00～19.00", "", "", "", "9.00～13.00", "", "", "", "", "", "", "");
        addMaterial("07Cr18Ni11Nb", "GB/T 5310-2023", "07Cr18Ni11Nb", "TP347H", "≥520", "≥205", "≥35", "125～192", "130～200", "70～90",
            "0～2.00", "17.00～19.00", "", "", "", "9.00～13.00", "", "", "", "", "", "", "");
        addMaterial("08Cr18Ni11NbFG", "GB/T 5310-2023", "08Cr18Ni11NbFG", "TP347HFG", "≥550", "≥205", "≥35", "140～192", "150～200", "75～90",
            "0～2.00", "17.00～19.00", "", "", "", "10.00～12.00", "", "", "", "", "", "", "");
        addMaterial("TP347HFG", "GB/T 5310-2023", "08Cr18Ni11NbFG", "TP347HFG", "", "", "", "", "", "",
            "0～2.00", "17.00～19.00", "", "", "", "10.00～12.00", "", "", "", "", "", "", "");
        
        // ASME SA-210/SA-210M 标准材料
        addMaterial("SA-210C", "ASME SA-210/SA-210M", "", "SA-210C", "≥485", "≥275", "壁厚≥8mm，≥30 壁厚=7.2mm，≥29 壁厚=6.4mm，≥27", "≤179", "", "≤89",
            "", "", "", "", "", "", "", "", "", "", "", "", "");
        
        // ASME SA-213/SA-213M 标准材料
        addMaterial("T12", "ASME SA-213/SA-213M", "", "T12", "≥415", "≥220", "壁厚≥8mm，≥30 壁厚=7.2mm，≥29 壁厚=6.4mm，≥27", "≤163", "≤170", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "");
        
        // DL/T 439-2018 标准材质
        addMaterial("35", "DL/T 439-2018", "35", "", "", "", "", "", "", "",
            "0.50～0.80", "0～0.25", "", "", "", "0～0.25", "", "", "", "", "", "", "");
        addMaterial("45", "DL/T 439-2018", "45", "", "", "", "", "", "", "",
            "0.50～0.80", "0～0.25", "", "", "", "0～0.25", "", "", "", "", "", "", "");
        addMaterial("20CrMoA", "DL/T 439-2018", "20CrMoA", "", "", "", "", "", "", "",
            "0.40～0.70", "0.80～1.10", "0.15～0.25", "", "", "0～0.30", "", "", "", "", "", "", "");
        addMaterial("35CrMoA", "DL/T 439-2018", "35CrMoA", "", "", "", "", "", "", "",
            "0.40～0.70", "0.80～1.10", "0.15～0.25", "", "", "0～0.30", "", "0～0.25", "", "", "", "", "");
        addMaterial("42CrMoA", "DL/T 439-2018", "42CrMoA", "", "", "", "", "", "", "",
            "0.50～0.80", "0.90～1.20", "0.15～0.25", "", "", "0～0.30", "", "0～0.25", "", "", "", "", "");
        addMaterial("40CrMoVA", "DL/T 439-2018", "40CrMoVA", "", "", "", "", "", "", "",
            "0.45～0.70", "0.80～1.15", "0.50～0.65", "0.25～0.35", "", "0～0.30", "", "0～0.25", "", "", "", "", "");
        addMaterial("45Cr1MoVA", "DL/T 439-2018", "45Cr1MoVA", "", "", "", "", "", "", "",
            "0.45～0.70", "0.80～1.15", "0.45～0.65", "0.25～0.35", "", "0～0.30", "0～0.015", "0～0.25", "", "", "", "", "");
        addMaterial("25Cr2MoVA", "DL/T 439-2018", "25Cr2MoVA", "", "", "", "", "", "", "",
            "0.40～0.70", "1.50～1.80", "0.25～0.35", "0.15～0.35", "", "0～0.30", "", "0～0.25", "", "", "", "", "");
        addMaterial("25Cr2Mo1VA", "DL/T 439-2018", "25Cr2Mo1VA", "", "", "", "", "", "", "",
            "0.50～0.80", "2.10～2.50", "0.90～1.10", "0.30～0.50", "", "0～0.30", "", "0～0.25", "", "", "", "", "");
        addMaterial("21Cr12MoV", "DL/T 439-2018", "21Cr12MoV", "", "", "", "", "", "", "",
            "0.30～0.80", "11.00～12.50", "0.80～1.20", "0.25～0.35", "", "0.30～0.60", "", "0～0.25", "", "", "", "", "");
        addMaterial("20Cr1Mo1V1A", "DL/T 439-2018", "20Cr1Mo1V1A", "", "", "", "", "", "", "",
            "0.30～0.60", "1.00～1.30", "0.80～1.10", "0.70～1.10", "", "0～0.40", "", "0～0.25", "", "", "", "", "");
        addMaterial("20Cr1Mo1VNbTiB", "DL/T 439-2018", "20Cr1Mo1VNbTiB", "", "", "", "", "", "", "",
            "0.40～0.65", "0.90～1.30", "0.75～1.00", "0.50～0.70", "0.05～0.14", "0～0.30", "", "0～0.25", "0.11～0.22", "", "", "", "");
        addMaterial("20Cr1Mo1VTiB", "DL/T 439-2018", "20Cr1Mo1VTiB", "", "", "", "", "", "", "",
            "0.40～0.60", "0.90～1.30", "0.75～1.00", "0.45～0.65", "0.16～0.28", "0～0.30", "", "0～0.25", "", "", "", "", "");
        addMaterial("22Cr12NiWMoV", "DL/T 439-2018", "22Cr12NiWMoV", "C-422", "", "", "", "", "", "",
            "0.50～1.00", "11.00～12.50", "0.90～1.25", "0.20～0.30", "", "0.50～1.00", "", "0～0.25", "", "0.90～1.25", "", "", "");
        addMaterial("C-422", "DL/T 439-2018", "22Cr12NiWMoV", "C-422", "", "", "", "", "", "",
            "0.50～1.00", "11.00～12.50", "0.90～1.25", "0.20～0.30", "", "0.50～1.00", "", "0～0.25", "", "0.90～1.25", "", "", "");
        addMaterial("2Cr12NiMo1W1V", "DL/T 439-2018", "2Cr12NiMo1W1V", "", "", "", "", "", "", "",
            "0.50～1.00", "11.00～12.50", "0.90～1.25", "0.20～0.30", "", "0.50～1.00", "", "0～0.25", "", "0.90～1.25", "", "", "");
        addMaterial("18Cr11NiMoNbVN", "DL/T 439-2018", "18Cr11NiMoNbVN", "", "", "", "", "", "", "",
            "0.50～0.80", "10.00～12.00", "0.60～0.90", "0.20～0.30", "", "0.30～0.60", "0～0.030", "0～0.20", "0.20～0.60", "", "", "", "");
        addMaterial("20Cr11MoNiNbVN", "DL/T 439-2018", "20Cr11MoNiNbVN", "", "", "", "", "", "", "",
            "0.30～0.80", "10.00～11.00", "0.50～0.80", "0.10～0.30", "", "0.20～0.50", "0～0.015", "", "0.25～0.55", "", "", "", "");
        addMaterial("14Cr11W2MoNiVNbN", "DL/T 439-2018", "14Cr11W2MoNiVNbN", "", "", "", "", "", "", "",
            "0.30～0.70", "10.00～11.00", "0.35～0.50", "0.14～0.20", "", "0.35～0.65", "0～0.020", "0～0.10", "0.05～0.11", "1.50～1.90", "", "", "");
        addMaterial("12Cr10Mo1W1NiVNbN", "DL/T 439-2018", "12Cr10Mo1W1NiVNbN", "", "", "", "", "", "", "",
            "0.40～0.60", "10.20～10.80", "1.00～1.10", "0.15～0.25", "", "0.70～0.85", "0～0.020", "", "0.03～0.07", "0.90～1.10", "", "", "");
        addMaterial("2Cr11Mo1VNbN", "DL/T 439-2018", "2Cr11Mo1VNbN", "", "", "", "", "", "", "",
            "0.50～0.80", "10.50～11.50", "0.80～1.10", "", "", "0.30～0.60", "0～0.05", "0～0.20", "0.35～0.55", "", "0.15～0.25", "", "");
        addMaterial("1Cr11Co3W3NiMoVNbNB", "DL/T 439-2018", "1Cr11Co3W3NiMoVNbNB", "", "", "", "", "", "", "",
            "0.35～0.65", "10.00～12.00", "0.10～0.40", "0.15～0.25", "", "0.30～0.70", "", "", "0.05～0.12", "2.40～3.00", "2.5～3.5", "", "");
        addMaterial("2Cr11Mo1NiWVNbN", "DL/T 439-2018", "2Cr11Mo1NiWVNbN", "", "", "", "", "", "", "",
            "0.50～0.90", "10.50～11.50", "0.80～1.10", "0.15～0.25", "", "0.35～0.65", "0～0.05", "", "0.15～0.25", "", "0～0.25", "", "");
        addMaterial("GH6783", "DL/T 439-2018", "GH6783", "", "", "", "", "", "", "",
            "0～0.50", "2.50～3.50", "", "", "", "0～0.40", "26.00～30.00", "5.00～6.00", "0～0.50", "2.50～3.50", "", "", "");
        addMaterial("R-26", "DL/T 439-2018", "R-26", "", "", "", "", "", "", "",
            "0～1.00", "16.00～20.00", "2.50～3.50", "", "", "35.0～39.0", "0～0.25", "", "", "", "18.00～22.00", "", "");
        addMaterial("GH4145", "DL/T 439-2018", "GH4145", "", "", "", "", "", "", "",
            "0～0.35", "14.00～17.00", "", "2.25～2.75", "", "70～78", "0.40～1.00", "0～0.50", "", "", "0～1.00", "0～0.01", "0～0.050");
        
        // DL/T 869 标准材质
        addMaterial("TIG-R71", "DL/T 869-2021", "TIG-R71", "", "", "", "", "", "", "",
            "1.20～1.90", "8.00～10.50", "0.80～1.20", "0.15～0.50", "", "0.20～1.00", "", "", "0.01～0.12", "", "", "", "");
        addMaterial("TIG-R50", "DL/T 869-2022", "TIG-R50", "", "", "", "", "", "", "",
            "0.40～0.70", "4.50～6.00", "0.45～0.65", "", "", "0～0.60", "", "", "", "", "", "", "");
        addMaterial("ZG20CrMo", "DL/T 869-2023", "ZG20CrMo", "", "", "", "", "", "", "",
            "0.50～0.80", "0.50～0.80", "0.50～0.60", "", "", "", "", "", "", "", "", "", "");
        addMaterial("38CrMoAlA", "DL/T 869-2024", "38CrMoAlA", "", "", "", "", "", "", "",
            "0.30～0.60", "1.35～1.65", "0.15～0.25", "", "", "", "", "", "", "", "", "", "");
    }

    /**
     * 初始化里氏硬度的管件 / 焊缝 / 钢管三段范围（仅用于里氏硬度检测 LHD）。
     * 键名约定：\"pipeRange管件\"、\"weldRage焊缝\"、\"steelPipeRage钢管\"。空字符串不写对应键。
     */
    private static void initializeLeebHardnessRanges() {
        addLeebRange("P91", "180～250", "185～270", "190～250");
        addLeebRange("SA-335P91", "180～250", "185～270", "190～250");
        addLeebRange("SA-335P92", "180～250", "185～270", "190～250");
        addLeebRange("P92", "180～250", "185～270", "190～250");
        addLeebRange("T91", "", "185～290", "190～250");
        addLeebRange("SA-213T91", "", "185～290", "190～250");
        addLeebRange("T92", "", "185～290", "190～250");
        addLeebRange("SA-213T92", "", "185～290", "190～250");
        //addLeebRange("10Cr9Mo1VNbN", "180～250", "285～350", "185～250");
        //ddLeebRange("10Cr9MoW2VNbBN", "180～250", "285～350", "185～250");
        addLeebRange("P22", "130～197", "285～350", "125～180");
        //addLeebRange("12Cr1MoVG", "130～197", "235～295", "135～195");
        addLeebRange("WB36", "190～255", "", "190～255");

        //新补充元素
        addLeebRange("20G", "106～160", "", "120～160");
        addLeebRange("25MnG", "", "", "130～180");

        addLeebRange("SA-106B", "130～197", "", "130～180");
        addLeebRange("SA-106C", "130～197", "", "130～180");

        addLeebRange("SA-210C", "", "", "130～180");

        addLeebRange("20MoG", "", "", "125～180");
        addLeebRange("STBA12", "", "", "125～180");
        addLeebRange("15Mo3", "", "", "125～180");

        addLeebRange("12CrMoG", "130～197", "", "125～170");

        addLeebRange("T2", "", "", "125～170");
        addLeebRange("P2", "130～197", "", "125～170");

        addLeebRange("T11", "", "", "125～170");
        addLeebRange("P11", "130～197", "", "125～170");

        addLeebRange("T12", "", "", "125～170");
        addLeebRange("P12", "130～197", "", "125～170");

        addLeebRange("15CrMoG", "130～197", "", "125～195");

        addLeebRange("12Cr2MoG", "", "", "125～180");

        addLeebRange("T22", "", "", "125～180");

        addLeebRange("P21", "130～197", "", "");

        addLeebRange("P22", "130～197", "285～350", "125～180");

        addLeebRange("10CrMo910", "130～197", "", "125～180");

        addLeebRange("12Cr1MoVG", "130～197", "", "135～195");

        addLeebRange("15Cr1Mo1V", "", "", "145～200");

        addLeebRange("T23", "", "", "150～220");
        addLeebRange("07Cr2MoW2VNbB", "", "", "150～220");

        addLeebRange("12Cr2MoWVTiB", "", "", "160～220");
        addLeebRange("G102", "", "", "160～220");

        addLeebRange("WB36", "190～255", "", "190～255");

        addLeebRange("15NiCuMoNb5-6-4", "", "", "190～255");
        addLeebRange("15NiCuMoNb5", "", "", "190～255");
        addLeebRange("15Ni1MnMoNbCu", "", "", "190～255");
        addLeebRange("P36", "", "", "190～255");

        addLeebRange("SA672 B70CL22", "130～197", "", "130～185");
        addLeebRange("SA672 B70CL32", "130～197", "", "130～185");

        addLeebRange("SA691 1-1", "130～197", "", "150～200");
        addLeebRange("4CrCL32", "130～197", "", "150～200");

        addLeebRange("10Cr9Mo1VNbN", "180～250", "285～350", "190～250");
       

        addLeebRange("10Cr9MoW2VNbBN", "180～250", "285～350", "190～250");

        addLeebRange("X11CrMoWVNb9-1-1", "180～250", "", "");

        addLeebRange("10Cr11MoW2VNbCu1BN", "", "", "190～250");

        addLeebRange("X20CrMoV12-1", "", "", "190～250");
        addLeebRange("X20CrMoWV12-1", "", "", "190～250");
        addLeebRange("X20CrMoV11-1", "180～250", "", "");

        addLeebRange("CSN417134", "", "", "190～250");

        addLeebRange("F11CL1", "121～174", "", "");
        addLeebRange("F12CL1", "121～174", "", "");

        addLeebRange("F11CL2", "143～207", "", "");
        addLeebRange("F12CL2", "143～207", "", "");

        addLeebRange("F22CL1", "130～170", "", "");
        addLeebRange("F22CL3", "156～207", "", "");

        addLeebRange("F91", "175～248", "", "");
        addLeebRange("F92", "180～269", "", "");

        addLeebRange("07Cr19Ni10", "", "", "140～192");
        addLeebRange("TP304H", "", "", "140～192");

        addLeebRange("07Cr18Ni11Nb", "", "", "140～192");
        addLeebRange("TP347H", "", "", "140～192");
        addLeebRange("TP347HFG", "", "", "140～192");

        addLeebRange("07Cr19Ni11Ti", "", "", "140～192");
        addLeebRange("TP321H", "", "", "140～192");

        addLeebRange("10Cr18Ni9NbCu3BN", "", "", "150～219");
        addLeebRange("S30432", "", "", "150～219");

        addLeebRange("07Cr25Ni21NbN", "", "", "150～256");
        addLeebRange("HR3C", "", "", "150～256");

        addLeebRange("08Cr9W3Co3VNbCuBN", "", "", "195～250");
        addLeebRange("G115", "", "", "195～250");
    }

    /**
     * 若材质库中已有仅大小写不同的条目，返回该条目的键名，使里氏硬度等同字段写入同一张属性表；
     * 否则返回去除首尾空白后的 {@code key}（可能为尚未存在的新键）。
     */
    private static String canonicalMaterialKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (MATERIAL_PROPERTIES.containsKey(trimmed)) {
            return trimmed;
        }
        for (String k : MATERIAL_PROPERTIES.keySet()) {
            if (k.equalsIgnoreCase(trimmed)) {
                return k;
            }
        }
        return trimmed;
    }

    private static void addLeebRange(String key, String pipeRange, String weldRange, String steelPipeRange) {
        String canonKey = canonicalMaterialKey(key);
        Map<String, String> props = MATERIAL_PROPERTIES.computeIfAbsent(canonKey, k -> new HashMap<>());
        if (pipeRange != null && !pipeRange.isEmpty()) {
            props.put("里氏-管件", pipeRange);
        }
        if (weldRange != null && !weldRange.isEmpty()) {
            props.put("里氏-焊缝", weldRange);
        }
        if (steelPipeRange != null && !steelPipeRange.isEmpty()) {
            props.put("里氏-钢管", steelPipeRange);
        }
    }

    /**
     * 初始化里氏硬度的螺栓范围（仅用于里氏硬度检测 LHD）
     * 键名约定："里氏-螺栓"。
     *
     * 说明：
     * - 这里默认将已有的里氏范围作为螺栓基准：优先使用 "里氏-管件"，其次 "里氏-钢管"，再其次 "里氏"。
     * - 你可以根据螺栓里氏硬度标准表，在此方法中为特定材质手动覆盖更精确的范围。
     */
    private static void initializeLeebBoltRanges() {
        for (Map.Entry<String, Map<String, String>> entry : MATERIAL_PROPERTIES.entrySet()) {
            Map<String, String> props = entry.getValue();
            if (props == null) {
                continue;
            }
            // 若已显式配置里氏-螺栓，则不覆盖
            if (props.containsKey("里氏-螺栓")) {
                continue;
            }
            String pipeRange = props.get("里氏-管件");
            String steelPipeRange = props.get("里氏-钢管");
            String leebRange = props.get("里氏");
            String boltRange = pipeRange != null && !pipeRange.isEmpty()
                    ? pipeRange
                    : (steelPipeRange != null && !steelPipeRange.isEmpty() ? steelPipeRange : leebRange);
            if (boltRange != null && !boltRange.isEmpty()) {
                props.put("里氏-螺栓", boltRange);
            }
        }

        // 根据用户提供的螺栓里氏硬度标准表，为特定材质设置精确的螺栓范围
        setLeebBoltRange("20CrMo", "197～241");
        setLeebBoltRange("35CrMo", "255～311");
        setLeebBoltRange("35CrMo-241", "241～285"); // 如业务中需要区分两段范围，可使用不同 key
        setLeebBoltRange("42CrMo", "255～321");
        setLeebBoltRange("42CrMo-248", "248～311");
        setLeebBoltRange("25Cr2MoV", "248～293");
        setLeebBoltRange("25Cr2Mo1V", "248～293");
        setLeebBoltRange("20Cr1Mo1V1", "248～293");
        setLeebBoltRange("20Cr1Mo1VTiB", "255～293");
        setLeebBoltRange("20Cr1Mo1VNbTiB", "252～302");
        setLeebBoltRange("20Cr12NiMoWV(C422)", "252～302");
        setLeebBoltRange("1Cr11MoNiW1VNbN", "252～302");
        setLeebBoltRange("2Cr11NiMoNbVN", "277～331");
        setLeebBoltRange("2Cr11Mo1VNbN", "290～321");
        setLeebBoltRange("2Cr12NiW1Mo1V", "290～321");
        setLeebBoltRange("2Cr11Mo1NiWVNbN", "290～321");
        setLeebBoltRange("45Cr1MoV", "248～293");
        setLeebBoltRange("R-26(Ni-Cr-Co合金）", "262～331");
        setLeebBoltRange("GH445", "262～331");
    }

    private static void setLeebBoltRange(String key, String boltRange) {
        if (boltRange == null || boltRange.isEmpty()) {
            return;
        }
        String canonKey = canonicalMaterialKey(key);
        Map<String, String> props = MATERIAL_PROPERTIES.computeIfAbsent(canonKey, k -> new HashMap<>());
        props.put("里氏-螺栓", boltRange);
    }
    
    /**
     * 添加材质属性数据（辅助方法）
     * @param key 材质名称（用于查询的key，可以是GB5310牌号或国外牌号）
     * @param standard 评定标准
     * @param gbGrade GB5310中牌号
     * @param foreignGrade 国外牌号
     * @param tensileStrength 抗拉强度Rm/MPa
     * @param yieldStrength 下屈服强度或规定塑性延伸强度（ReL/MPa或Rp0.2/MPa）
     * @param elongation 断后伸长率A/%
     * @param brinell 布氏（HBW）
     * @param vickers 维氏（HV）
     * @param rockwell 洛氏（HRBW）
     * @param mn 锰（Mn）标准值
     * @param cr 铬（Cr）标准值
     * @param mo 钼（Mo）标准值
     * @param v 钒（V）标准值
     * @param ti 钛（Ti）标准值
     * @param ni 镍（Ni）标准值
     * @param al 铝（Al）标准值
     * @param cu 铜（Cu）标准值
     * @param nb 铌（Nb）标准值
     * @param w 钨（W）标准值
     * @param co 钴（Co）标准值
     * @param mg 镁（Mg）标准值
     * @param zr 锆（Zr）标准值
     */
    private static void addMaterial(String key, String standard, String gbGrade, String foreignGrade,
                                   String tensileStrength, String yieldStrength, String elongation,
                                   String brinell, String vickers, String rockwell,
                                   String mn, String cr, String mo, String v, String ti, String ni,
                                   String al, String cu, String nb, String w, String co, String mg, String zr) {
        Map<String, String> properties = new HashMap<>();
        properties.put("评定标准", standard);
        properties.put("GB5310牌号", gbGrade);
        properties.put("国外牌号", foreignGrade);
        properties.put("抗拉强度", tensileStrength);
        properties.put("下屈服强度", yieldStrength);
        properties.put("断后伸长率", elongation);
        properties.put("布氏", brinell);
        properties.put("维氏", vickers);
        properties.put("洛氏", rockwell);
        // 元素标准值
        if (mn != null && !mn.isEmpty()) properties.put("Mn", mn);
        if (cr != null && !cr.isEmpty()) properties.put("Cr", cr);
        if (mo != null && !mo.isEmpty()) properties.put("Mo", mo);
        if (v != null && !v.isEmpty()) properties.put("V", v);
        if (ti != null && !ti.isEmpty()) properties.put("Ti", ti);
        if (ni != null && !ni.isEmpty()) properties.put("Ni", ni);
        if (al != null && !al.isEmpty()) properties.put("Al", al);
        if (cu != null && !cu.isEmpty()) properties.put("Cu", cu);
        if (nb != null && !nb.isEmpty()) properties.put("Nb", nb);
        if (w != null && !w.isEmpty()) properties.put("W", w);
        if (co != null && !co.isEmpty()) properties.put("Co", co);
        if (mg != null && !mg.isEmpty()) properties.put("Mg", mg);
        if (zr != null && !zr.isEmpty()) properties.put("Zr", zr);
        MATERIAL_PROPERTIES.put(key, properties);
    }
    
    /**
     * 根据材质名称查询材料属性（不区分大小写）
     * @param materialName 材质名称（可以是GB5310牌号或国外牌号）
     * @return 材料属性映射，如果找不到则返回 null
     */
    public Map<String, String> getMaterialProperty(String materialName) {
        if (materialName == null || materialName.trim().isEmpty()) {
            return null;
        }
        
        String trimmedName = materialName.trim();
        
        // 先尝试精确匹配（性能优化）
        Map<String, String> result = MATERIAL_PROPERTIES.get(trimmedName);
        if (result != null) {
            return result;
        }
        
        // 如果精确匹配失败，进行不区分大小写的匹配
        for (Map.Entry<String, Map<String, String>> entry : MATERIAL_PROPERTIES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(trimmedName)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * 里氏焊缝比对用范围：优先「里氏-焊缝」，否则通用「里氏」；
     * 若均为空且满足布氏推算触发条件，则返回按母材布氏区间推算的焊缝里氏范围字符串。
     */
    public String resolveLeebWeldRangeForComparison(Map<String, String> materialProperty) {
        if (materialProperty == null) {
            return "";
        }
        String weldCol = trimToEmpty(materialProperty.get("里氏-焊缝"));
        String genericLeeb = trimToEmpty(materialProperty.get("里氏"));
        String explicit = !weldCol.isEmpty() ? weldCol : genericLeeb;
        if (!explicit.isEmpty()) {
            return explicit;
        }
        return resolveBrinellDerivedLeebWeldRange(materialProperty).orElse("");
    }

    /**
     * 当「里氏-钢管」已配置、「里氏-焊缝」与「里氏」均为空时，推算焊缝里氏判定范围（与 DL/T869 表述配套的数值规则）：
     * 优先使用「布氏」区间；若无或非数值（如仅 Leeb 维护的材质无布氏字段），则用「里氏-钢管」区间作参照。
     * 上限 {@code min(参照上限+100, 270)}，下限 {@code 参照下限×0.9}；无效区间则返回 empty。
     */
    public Optional<String> resolveBrinellDerivedLeebWeldRange(Map<String, String> materialProperty) {
        if (materialProperty == null || !isLeebWeldBrinellFallbackTrigger(materialProperty)) {
            return Optional.empty();
        }
        Optional<double[]> hb = parseBrinellToMinMax(materialProperty.get("布氏"));
        if (hb.isEmpty()) {
            hb = parseNumericHardnessInterval(materialProperty.get("里氏-钢管"));
        }
        if (hb.isEmpty()) {
            log.debug("焊缝里氏推算跳过：布氏与里氏-钢管均无法解析为数值区间");
            return Optional.empty();
        }
        double hbMin = hb.get()[0];
        double hbMax = hb.get()[1];
        double weldMax = Math.min(hbMax + 100.0, 270.0);
        double weldMin = hbMin * 0.9;
        if (weldMin > weldMax) {
            log.debug("焊缝里氏布氏推算放弃：推算下限 {} 大于上限 {}", weldMin, weldMax);
            return Optional.empty();
        }
        return Optional.of(formatLeebRange(weldMin, weldMax));
    }

    private static boolean isLeebWeldBrinellFallbackTrigger(Map<String, String> p) {
        String steel = trimToEmpty(p.get("里氏-钢管"));
        if (steel.isEmpty()) {
            return false;
        }
        String weld = trimToEmpty(p.get("里氏-焊缝"));
        String generic = trimToEmpty(p.get("里氏"));
        return weld.isEmpty() && generic.isEmpty();
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 解析布氏标准为 {@code [min, max]}；无法解析或含壁厚等叙述性描述时返回 empty。
     */
    static Optional<double[]> parseBrinellToMinMax(String brinell) {
        if (brinell == null) {
            return Optional.empty();
        }
        String t = brinell.trim();
        if (t.isEmpty()) {
            return Optional.empty();
        }
        if (t.contains("壁厚")) {
            return Optional.empty();
        }
        return parseNumericHardnessInterval(t);
    }

    /**
     * 解析硬度类数值区间（布氏/里氏范围共用）：{@code a～b}、{@code a-b}、{@code ≤x}、{@code ≥x}。
     */
    static Optional<double[]> parseNumericHardnessInterval(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return Optional.empty();
        }

        if (t.contains("～")) {
            String[] parts = t.split("～", 2);
            if (parts.length == 2) {
                Optional<Double> a = parseFirstDouble(parts[0]);
                Optional<Double> b = parseFirstDouble(parts[1]);
                if (a.isPresent() && b.isPresent()) {
                    double lo = Math.min(a.get(), b.get());
                    double hi = Math.max(a.get(), b.get());
                    return Optional.of(new double[]{lo, hi});
                }
            }
            return Optional.empty();
        }

        String trimmedCompat = t.startsWith("<=") ? "≤" + t.substring(2).trim() : t;
        if (trimmedCompat.startsWith("≤")) {
            Optional<Double> v = parseFirstDouble(trimmedCompat.substring(1));
            return v.map(val -> new double[]{val, val});
        }
        String trimmedCompatGe = t.startsWith(">=") ? "≥" + t.substring(2).trim() : t;
        if (trimmedCompatGe.startsWith("≥")) {
            Optional<Double> v = parseFirstDouble(trimmedCompatGe.substring(1));
            return v.map(val -> new double[]{val, val});
        }

        Matcher hm = HYPHEN_BRINELL_RANGE.matcher(t);
        if (hm.matches()) {
            try {
                double a = Double.parseDouble(hm.group(1));
                double b = Double.parseDouble(hm.group(2));
                double lo = Math.min(a, b);
                double hi = Math.max(a, b);
                return Optional.of(new double[]{lo, hi});
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static Optional<Double> parseFirstDouble(String fragment) {
        if (fragment == null) {
            return Optional.empty();
        }
        Matcher m = FIRST_NUMBER.matcher(fragment.trim());
        if (!m.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(m.group(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String formatLeebRange(double min, double max) {
        return formatLeebNumber(min) + "～" + formatLeebNumber(max);
    }

    private static String formatLeebNumber(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
        if (bd.scale() > 0 && bd.stripTrailingZeros().scale() <= 0) {
            bd = bd.stripTrailingZeros();
        }
        return bd.toPlainString();
    }
}
