package com.reportweb.service;

import java.util.*;

/**
 * 设备ID到检测类型代码的映射服务
 * 根据全局仪器库(instruments表)的ID映射到对应的检测类型代码
 */
public class InstrumentTypeMappingService {

    // 设备ID到检测类型代码的映射
    private static final Map<Integer, String> INSTRUMENT_TYPE_MAP = new HashMap<>();

    static {
        // 氧化皮堆积检测 (SOD)
        addMapping(Arrays.asList(1, 147), "SOD");

        // 金相检测 (MET)
        addMapping(Arrays.asList(2, 3, 47, 48, 49, 50, 51, 56, 142, 145), "MET");

        // 里氏硬度检测 (LHT)
        addMapping(Arrays.asList(5, 6, 7, 8, 9, 41, 42, 43, 44, 45, 46, 124, 125, 126, 127, 128), "LHT");

        // 合金分析检测 (PMI)
        addMapping(Arrays.asList(10, 11, 73, 74, 129), "PMI");

        // 磁粉检测 (MT)
        addMapping(Arrays.asList(21, 22, 23, 24, 25, 26, 60, 61, 67, 68, 69, 70, 71, 88, 89, 90, 110, 111, 112, 113, 114, 115, 152, 153, 154, 155, 156), "MT");

        // 维氏硬度检测 (VHT)
        addMapping(Arrays.asList(27), "VHT");

        // 布氏硬度检测 (BHT)
        addMapping(Arrays.asList(30, 31, 32, 33, 34, 35, 91, 92), "BHT");

        // 洛氏硬度检测 (RHT)
        addMapping(Arrays.asList(40, 41), "RHT");

        // 涡流检测 (ET)
        addMapping(Arrays.asList(57), "ET");

        // 内窥镜检测 (VT)
        addMapping(Arrays.asList(58, 59, 106, 107, 148), "VT");

        // 目视检测 (VIS)，体视显微镜等
        addMapping(Arrays.asList(29), "VIS");

        // 超声检测 (UT)
        addMapping(Arrays.asList(62, 63, 64, 65, 66, 104, 105, 133, 134, 135, 150), "UT");

        // 超声波测厚 (UTM)
        addMapping(Arrays.asList(72, 98, 99, 100, 101, 102, 103, 116, 117, 118, 119, 120, 121, 122, 123, 136, 137, 138, 139, 140, 141), "UTM");

        // 射线检测 (RT)
        addMapping(Arrays.asList(76, 77, 78, 79, 80, 81, 82, 83), "RT");

        // 相控阵超声波检测 (PAUT)
        addMapping(Arrays.asList(146, 165), "PAUT");
    }

    /**
     * 辅助方法：将ID列表添加到映射中
     */
    private static void addMapping(List<Integer> ids, String experimentTypeCode) {
        for (Integer id : ids) {
            INSTRUMENT_TYPE_MAP.put(id, experimentTypeCode);
        }
    }

    /**
     * 根据设备ID获取对应的检测类型代码
     * 
     * @param instrumentId 全局仪器库的设备ID
     * @return 检测类型代码，如果未找到则返回null
     */
    public static String getExperimentTypeCodeByInstrumentId(Integer instrumentId) {
        if (instrumentId == null) {
            return null;
        }
        return INSTRUMENT_TYPE_MAP.get(instrumentId);
    }

    /**
     * 检查设备ID是否在映射表中
     * 
     * @param instrumentId 全局仪器库的设备ID
     * @return 如果存在映射则返回true，否则返回false
     */
    public static boolean hasMapping(Integer instrumentId) {
        return instrumentId != null && INSTRUMENT_TYPE_MAP.containsKey(instrumentId);
    }

    /**
     * 获取所有支持的检测类型代码列表
     * 
     * @return 检测类型代码的Set集合
     */
    public static Set<String> getAllExperimentTypeCodes() {
        return new HashSet<>(INSTRUMENT_TYPE_MAP.values());
    }
}
