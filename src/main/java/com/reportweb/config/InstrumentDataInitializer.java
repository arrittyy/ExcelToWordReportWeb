package com.reportweb.config;

import com.reportweb.entity.Instrument;
import com.reportweb.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局仪器设备数据初始化器
 * 批量添加仪器设备数据到全局库
 * 通过配置开关 app.init.instruments=true 控制是否执行
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(3) // 在DataInitializer和PowerPlantDataInitializer之后执行
@ConditionalOnProperty(name = "app.init.instruments", havingValue = "true", matchIfMissing = false)
public class InstrumentDataInitializer implements CommandLineRunner {

    private final InstrumentRepository instrumentRepository;

    @Override
    public void run(String... args) throws Exception {
        if (instrumentRepository.count() > 0) {
            log.info("仪器设备数据已存在，跳过初始化");
            return;
        }
        log.info("开始初始化仪器设备数据...");
        addInstruments();
        log.info("仪器设备数据初始化完成。");
    }

    private void addInstruments() {
        List<Instrument> instrumentsToCreate = new ArrayList<>();

        // 用户提供的设备数据
        addInstrument(instrumentsToCreate, "氧化皮检测仪", "OMD-200", "CL1-20Q012");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "SY-01", "CL1-20Q011");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "SY-01", "CL1-20Q010");
        addInstrument(instrumentsToCreate, "电子万能试验机", "UTM5105", "CL1-20G004");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBINO2", "CL2-20G005");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBINO2", "CL2-20G006");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBINO2", "CL2-20G007");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBINO2", "CL2-20G008");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBINO2", "CL2-20G009");
        addInstrument(instrumentsToCreate, "手持式合金分析仪", "X-MET8000 Optimum", "CL1-20G002");
        addInstrument(instrumentsToCreate, "手持式合金分析仪", "X-MET8000 Optimum", "CL1-20G003");
        addInstrument(instrumentsToCreate, "标准负荷测力仪", "HS2000-300A", "CL2-20G001");
        addInstrument(instrumentsToCreate, "管道应力分析软件", "CAESAR II 2019", "CL1-19Q019");
        addInstrument(instrumentsToCreate, "电解腐蚀抛光仪", "EP-06", "CL1-19Q016");
        addInstrument(instrumentsToCreate, "自动磨抛机", "ECOMET30", "CL1-19Q015");
        addInstrument(instrumentsToCreate, "热压镶嵌机", "MET4000", "CL1-19Q014");
        addInstrument(instrumentsToCreate, "手动砂轮切割机", "MET250", "CL1-19Q013");
        addInstrument(instrumentsToCreate, "镀层测厚仪", "OLYMPUS 38DL PLUS", "CL1-19G017");
        addInstrument(instrumentsToCreate, "镀层测厚仪", "DeFelsko PosiTector 600", "CL2-19G018");
        addInstrument(instrumentsToCreate, "摆锤式冲击试验机", "302D", "CL1-19G012");
        addInstrument(instrumentsToCreate, "磁轭探伤仪", "1702", "CL1-19G011");
        addInstrument(instrumentsToCreate, "磁轭探伤仪", "1702", "CL1-19G010");
        addInstrument(instrumentsToCreate, "磁轭探伤仪", "1702", "CL1-19G009");
        addInstrument(instrumentsToCreate, "磁轭探伤仪", "1702", "CL1-19G008");
        addInstrument(instrumentsToCreate, "磁轭探伤仪", "1702", "CL1-19G007");
        addInstrument(instrumentsToCreate, "磁轭探伤仪", "1702", "CL1-19G006");
        addInstrument(instrumentsToCreate, "自动显微维氏硬度计", "VH1202", "CL1-18G053");
        addInstrument(instrumentsToCreate, "微机控制电子万能试验机", "UTM5305HA", "CL1-18G060");
        addInstrument(instrumentsToCreate, "体视显微镜", "M125C", "CL1-18G061");
        addInstrument(instrumentsToCreate, "数显布氏硬度计", "BH3000", "CL1-18G062");
        addInstrument(instrumentsToCreate, "锤击布氏硬度计", "PHB-1", "CL2-18G031");
        addInstrument(instrumentsToCreate, "锤击布氏硬度计", "PHB-1", "CL2-18G030");
        addInstrument(instrumentsToCreate, "锤击布氏硬度计", "PHB-1", "CL2-18G029");
        addInstrument(instrumentsToCreate, "锤击布氏硬度计", "PHB-1", "CL2-18G028");
        addInstrument(instrumentsToCreate, "磁力数显布氏硬度计", "PHB-200", "CL1-18G032");
        addInstrument(instrumentsToCreate, "红外热成像仪", "F562", "CL2-18Q066");
        addInstrument(instrumentsToCreate, "红外热成像仪", "F562", "CL2-18Q065");
        addInstrument(instrumentsToCreate, "红外热成像仪", "F562", "CL2-18Q064");
        addInstrument(instrumentsToCreate, "红外热成像仪", "F562", "CL2-18Q063");
        addInstrument(instrumentsToCreate, "C型布洛硬度计", "PHBR-4", "CL1-18G054");
        addInstrument(instrumentsToCreate, "数显洛氏硬度计", "574R", "CL1-18G052");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBIN02", "CL2-18G012");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBIN02", "CL2-18G011");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBIN02", "CL2-18G010");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBIN02", "CL2-18G009");
        addInstrument(instrumentsToCreate, "里氏硬度计", "BAMBIN02", "CL2-18G008");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "XZD-500", "CL1-18G025");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "XZD-500", "CL1-18G024");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "XZD-500", "CL1-18G023");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "XZD-500", "CL1-18G022");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "XZD-500", "CL1-18G021");
        addInstrument(instrumentsToCreate, "手动磨抛", "830", "CL2-18Q059");
        addInstrument(instrumentsToCreate, "手动磨抛", "830", "CL2-18Q058");
        addInstrument(instrumentsToCreate, "手动磨抛", "820", "CL2-18Q057");
        addInstrument(instrumentsToCreate, "手动磨抛", "820", "CL2-18Q056");
        addInstrument(instrumentsToCreate, "台式倒置金相显微镜", "DMi8C", "CL1-18G055");
        addInstrument(instrumentsToCreate, "便携型远场涡流探伤仪", "EEC-39RFT", "CL1-18G049");
        addInstrument(instrumentsToCreate, "工业内窥镜", "XLVUD84100", "CL1-18Q051");
        addInstrument(instrumentsToCreate, "工业内窥镜", "XLLVB84100", "CL1-18Q050");
        addInstrument(instrumentsToCreate, "旋转磁场探伤仪", "ZCM-DX1203A", "CL2-18G046");
        addInstrument(instrumentsToCreate, "旋转磁场探伤仪", "ZCM-DX1203A", "CL2-18G045");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS700", "CL1-18G039");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS700", "CL1-18G038");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS700", "CL1-18G037");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS700", "CL1-18G036");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS700", "CL1-18G035");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "LBNB-22016", "CL2-18G044");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "LBNB-22016", "CL2-18G043");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "LBNB-22016", "CL2-18G042");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "LBNB-22016", "CL2-18G041");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "LBNB-22016", "CL2-18G040");
        addInstrument(instrumentsToCreate, "波形显示型测厚仪", "TS-ATG11", "CL2-18G047");
        addInstrument(instrumentsToCreate, "合金分析仪", "X-MET8000", "CL1-18G027");
        addInstrument(instrumentsToCreate, "合金分析仪", "X-MET8000", "CL1-18G026");
        addInstrument(instrumentsToCreate, "胶片冲洗装置", "DL-P14A-NDT", "CL1-18Q048");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-3005", "CL1-18G020");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-3005", "CL1-18G019");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-2505", "CL1-18G018");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-2505", "CL1-18G017");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-2005", "CL1-18G016");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-2005", "CL1-18G015");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-2005", "CL1-18G014");
        addInstrument(instrumentsToCreate, "X射线探伤机", "XXQ-2005", "CL1-18G013");
        addInstrument(instrumentsToCreate, "应力分析工作站", "T7920", "CL1-18Q034");
        addInstrument(instrumentsToCreate, "应力分析工作站", "T7920", "CL1-18Q033");
        addInstrument(instrumentsToCreate, "电阻应变仪", "AFT-CM-10", "CL2-18G006");
        addInstrument(instrumentsToCreate, "辐射监测仪", "R-PD", "JS2-17G020");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "LKNB-22016", "JS2-16G029");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "LKNB-22016", "JS2-16G028");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "LKNB-22016", "JS2-16G027");
        addInstrument(instrumentsToCreate, "磁力数显布氏硬度计", "PHB-200", "JS1-16G031");
        addInstrument(instrumentsToCreate, "C型布洛硬度计", "PHBR-4-3", "JS2-16G032");
        addInstrument(instrumentsToCreate, "人工智能箱式电阻炉", "SGM.M4/13AS", "JS2-16Q033");
        addInstrument(instrumentsToCreate, "相控阵检测探头", "4L32", "JS1-16Q013");
        addInstrument(instrumentsToCreate, "相控阵检测探头", "4L32", "JS1-16Q012");
        addInstrument(instrumentsToCreate, "多功能气体检测仪", "PG610-P", "JS2-16G003");
        addInstrument(instrumentsToCreate, "多功能气体检测仪", "PG610-P", "JS2-16G002");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "TIME2110", "JS2-16G009");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "TIME2110", "JS2-16G008");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "TIME2110", "JS2-16G007");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "TIME2110", "JS2-16G006");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "TIME2110", "JS2-16G005");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "TIME2110", "JS2-16G004");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS620", "JS1-16G011");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "HS620", "JS1-16G010");
        addInstrument(instrumentsToCreate, "工业内窥镜", "GT200A S252", "JS1-15Q044");
        addInstrument(instrumentsToCreate, "工业内窥镜", "GT200A S252", "JS1-15Q043");
        addInstrument(instrumentsToCreate, "电子拉力计", "EDX-10T", "JS1-15G042");
        addInstrument(instrumentsToCreate, "电子拉力计", "EDX-5T", "JS1-15G041");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "MY-2", "JS2-15G024");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "MY-2", "JS2-15G023");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "MY-2", "JS2-15G022");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "MY-2", "JS2-15G021");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "MY-2", "JS2-15G020");
        addInstrument(instrumentsToCreate, "交流磁粉探伤仪", "MY-2", "JS2-15G019");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G018");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G017");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G016");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G015");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G014");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G013");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G012");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-15G011");
        addInstrument(instrumentsToCreate, "里氏硬度计", "Bambino2", "JS2-15G010");
        addInstrument(instrumentsToCreate, "里氏硬度计", "Bambino2", "JS2-15G009");
        addInstrument(instrumentsToCreate, "里氏硬度计", "Bambino2", "JS2-15G006");
        addInstrument(instrumentsToCreate, "里氏硬度计", "Bambino2", "JS2-15G005");
        addInstrument(instrumentsToCreate, "里氏硬度计", "Bambino2", "JS2-15G004");
        addInstrument(instrumentsToCreate, "合金分析仪", "X-MET8000", "JS1-15G039");
        addInstrument(instrumentsToCreate, "安全阀在线校验仪", "JY-S", "JS1-15G003");
        addInstrument(instrumentsToCreate, "超声相控阵检测仪", "SUPOR-32P", "JS1-14G020");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "CY-1000", "JS2-14G024");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "CTS-9006PLUS", "JS1-14G023");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "CTS-9006PLUS", "JS1-14G022");
        addInstrument(instrumentsToCreate, "超声波探伤仪", "CTS-9006PLUS", "JS1-14G021");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-14G009");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-14G008");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-14G007");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-14G006");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-14G005");
        addInstrument(instrumentsToCreate, "超声波测厚仪", "MT-160", "JS2-14G004");
        addInstrument(instrumentsToCreate, "现场金相显微镜", "JXD-900", "JS1-14Q003");
        addInstrument(instrumentsToCreate, "电子拉力计", "EDIR-10T", "JS1-14G002");
        addInstrument(instrumentsToCreate, "电子拉力计", "EDIR-5T", "JS2-14G001");
        addInstrument(instrumentsToCreate, "手持式合金分析仪", "Niton XL3t980", "JS1-13G012");
        addInstrument(instrumentsToCreate, "相控阵检测仪", "PHSCAN32/64", "JS1-13Q002");
        addInstrument(instrumentsToCreate, "氧化皮检测仪", "OMD-100", "JS1-12Q030");
        addInstrument(instrumentsToCreate, "工业内窥镜", "WIWA ES325", "JS1-12Q029");
        addInstrument(instrumentsToCreate, "安全阀在线校验仪", "YLT-DL-S", "JS1-12Q033");
        addInstrument(instrumentsToCreate, "便携式智能超声波检测系统", "Isonic2005", "JS1-07G175");
        addInstrument(instrumentsToCreate, "超声波硬度计", "newsonic", "JS1-14G017");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "MY-2", "JS2-14G014");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "MY-2", "JS2-14G013");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "MY-2", "JS2-14G012");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "MY-2", "JS2-14G011");
        addInstrument(instrumentsToCreate, "磁粉探伤仪", "MY-2", "JS2-14G010");
        addInstrument(instrumentsToCreate, "红外热成像仪", "Fluke Ti125", "JS1-14Q019");
        addInstrument(instrumentsToCreate, "红外热成像仪", "Fluke Ti125", "JS1-14Q018");
        addInstrument(instrumentsToCreate, "16晶片相控阵检测探头", "2L16", "JS1-12Q035");
        addInstrument(instrumentsToCreate, "安全阀动态研磨机", "SFX-150I", "JS1-12Q032");
        addInstrument(instrumentsToCreate, "安全阀定压校验台", "SAT-Q32S", "JS1-12Q031");
        addInstrument(instrumentsToCreate, "试压泵", "DSY-10MPa", "GJ2-06G26");
        addInstrument(instrumentsToCreate, "全站仪", "RTS238", "GJ2-06G24");
        addInstrument(instrumentsToCreate, "便携式可燃气体检测仪", "SNE168", "GJ2-06G25");
        addInstrument(instrumentsToCreate, "相控阵超声检测开发平台", "", "CL1-19Q002");

        // 批量保存，去重处理
        int savedCount = 0;
        int skippedCount = 0;
        for (Instrument instrument : instrumentsToCreate) {
            // 检查是否已存在（按名称+型号+编号）
            boolean exists = false;
            if (instrument.getInstrumentNumber() != null && !instrument.getInstrumentNumber().isEmpty()) {
                exists = instrumentRepository.findByInstrumentNameAndInstrumentModelAndInstrumentNumber(
                    instrument.getInstrumentName(),
                    instrument.getInstrumentModel(),
                    instrument.getInstrumentNumber()
                ).isPresent();
            }
            
            if (exists) {
                skippedCount++;
                log.debug("仪器设备已存在，跳过: {} - {} - {}", 
                    instrument.getInstrumentName(), 
                    instrument.getInstrumentModel(), 
                    instrument.getInstrumentNumber());
            } else {
                instrumentRepository.save(instrument);
                savedCount++;
            }
        }
        
        log.info("成功添加 {} 条仪器设备记录，跳过 {} 条重复记录", savedCount, skippedCount);
    }

    private void addInstrument(List<Instrument> list, String name, String model, String number) {
        Instrument instrument = new Instrument();
        instrument.setInstrumentName(name);
        instrument.setInstrumentModel(model != null && !model.isEmpty() ? model : null);
        instrument.setInstrumentNumber(number != null && !number.isEmpty() ? number : null);
        instrument.setCreatedAt(LocalDateTime.now());
        instrument.setUpdatedAt(LocalDateTime.now());
        list.add(instrument);
    }
}

