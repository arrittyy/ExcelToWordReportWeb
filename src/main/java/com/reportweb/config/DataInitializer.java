package com.reportweb.config;

import com.reportweb.entity.User;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Report;
import com.reportweb.repository.UserRepository;
import com.reportweb.repository.ExperimentTypeRepository;
import com.reportweb.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExperimentTypeRepository experimentTypeRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 生成并记录 123456 的正确 BCrypt 哈希值（用于更新数据库）
        String testPassword = "123456";
        String correctHash = passwordEncoder.encode(testPassword);
        log.info("===========================================");
        log.info("正确的 '123456' BCrypt 哈希值: {}", correctHash);
        log.info("验证测试: {}", passwordEncoder.matches(testPassword, correctHash));
        log.info("===========================================");
        
        initializeDefaultUsers();
        fixPasswordHashes(correctHash); // 修复所有用户的密码哈希值
        initializeExperimentTypes();
        fixEmptyFieldsInReports();
    }
    
    /**
     * 修复所有用户的密码哈希值（如果使用的是旧的错误哈希值）
     */
    private void fixPasswordHashes(String correctHash) {
        try {
            log.info("开始检查并修复用户密码哈希值...");
            String oldHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi";
            List<User> allUsers = userRepository.findAll();
            int fixedCount = 0;
            
            for (User user : allUsers) {
                String currentHash = user.getPasswordHash();
                // 如果用户使用的是旧的错误哈希值，则更新为正确的哈希值
                if (currentHash != null && currentHash.equals(oldHash)) {
                    // 验证旧哈希值确实无法验证 123456
                    if (!passwordEncoder.matches("123456", currentHash)) {
                        user.setPasswordHash(correctHash);
                        userRepository.save(user);
                        fixedCount++;
                        log.info("已修复用户 {} 的密码哈希值", user.getUserName());
                    }
                }
            }
            
            if (fixedCount > 0) {
                log.info("密码哈希值修复完成：总用户数={}, 修复用户数={}", allUsers.size(), fixedCount);
            } else {
                log.info("所有用户密码哈希值检查完成：无需修复（总用户数={}）", allUsers.size());
            }
        } catch (Exception e) {
            log.error("修复密码哈希值时发生错误", e);
        }
    }

    private void initializeDefaultUsers() {
        // 检查是否已存在 admin 用户
        if (!userRepository.existsByUserName("admin")) {
            log.info("Creating default admin user...");
            
            User adminUser = new User();
            adminUser.setId(UUID.randomUUID().toString());
            adminUser.setUserName("admin");
            adminUser.setNormalizedUserName("ADMIN");
            adminUser.setEmail("admin@example.com");
            adminUser.setNormalizedEmail("ADMIN@EXAMPLE.COM");
            adminUser.setEmailConfirmed(true);
            adminUser.setPasswordHash(passwordEncoder.encode("Admin123!"));
            adminUser.setSecurityStamp(UUID.randomUUID().toString());
            adminUser.setConcurrencyStamp(UUID.randomUUID().toString());
            adminUser.setPhoneNumberConfirmed(false);
            adminUser.setTwoFactorEnabled(false);
            adminUser.setLockoutEnabled(true);
            adminUser.setAccessFailedCount(0);
            adminUser.setFullName("系统管理员");
            adminUser.setDepartment("技术部");
            adminUser.setCreatedAt(LocalDateTime.now());

            userRepository.save(adminUser);
            log.info("Default admin user created successfully");
        } else {
            log.info("Admin user already exists, skipping creation");
        }

        // 检查是否已存在 testuser 用户
        if (!userRepository.existsByUserName("testuser")) {
            log.info("Creating default test user...");
            
            User testUser = new User();
            testUser.setId(UUID.randomUUID().toString());
            testUser.setUserName("testuser");
            testUser.setNormalizedUserName("TESTUSER");
            testUser.setEmail("test@example.com");
            testUser.setNormalizedEmail("TEST@EXAMPLE.COM");
            testUser.setEmailConfirmed(true);
            testUser.setPasswordHash(passwordEncoder.encode("Test123!"));
            testUser.setSecurityStamp(UUID.randomUUID().toString());
            testUser.setConcurrencyStamp(UUID.randomUUID().toString());
            testUser.setPhoneNumberConfirmed(false);
            testUser.setTwoFactorEnabled(false);
            testUser.setLockoutEnabled(true);
            testUser.setAccessFailedCount(0);
            testUser.setFullName("测试用户");
            testUser.setDepartment("测试部");
            testUser.setCreatedAt(LocalDateTime.now());

            userRepository.save(testUser);
            log.info("Default test user created successfully");
        } else {
            log.info("Test user already exists, skipping creation");
        }
    }

    private void initializeExperimentTypes() {
        log.info("Checking and initializing experiment types...");

        String reportFieldsSchemaTemplate = "{\"fields\":[{\"name\":\"serialNumber\",\"label\":\"序号\",\"type\":\"text\",\"autoGenerate\":true},{\"name\":\"equipmentCategory\",\"label\":\"设备类别\",\"type\":\"text\"},{\"name\":\"equipmentName\",\"label\":\"设备名称\",\"type\":\"text\"},{\"name\":\"componentSpec\",\"label\":\"部件规格\",\"type\":\"text\"},{\"name\":\"instrumentModel\",\"label\":\"仪器型号\",\"type\":\"text\"},{\"name\":\"inspector\",\"label\":\"检测人员\",\"type\":\"text\"},{\"name\":\"location\",\"label\":\"检测地点\",\"type\":\"text\"},{\"name\":\"testDate\",\"label\":\"检测日期\",\"type\":\"date\"}]}";

        List<ExperimentTypeConfig> configs = List.of(
            // 超声检测：序号、位置、波幅（dB）、深度（mm）、长度（mm）、高度（mm）、级别、备注
            ExperimentTypeConfig.of("超声检测", "UT",
                createTableSchema(
                    new Column("序号", "序号", "text"),
                    new Column("位置", "位置", "text"),
                    new Column("波幅", "波幅（dB）", "text"),
                    new Column("深度", "深度（mm）", "text"),
                    new Column("长度", "长度（mm）", "text"),
                    new Column("高度", "高度（mm）", "text"),
                    new Column("级别", "级别", "text"),
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 渗透检测：编号、起点位置、终点位置、长度、级别、备注
            ExperimentTypeConfig.of("渗透检测", "PT",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("起点位置", "起点位置", "text"),
                    new Column("终点位置", "终点位置", "text"),
                    new Column("长度", "长度", "text"),
                    new Column("级别", "级别", "text"),
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 磁粉检测：编号、起点位置、终点位置、长度、级别、备注
            ExperimentTypeConfig.of("磁粉检测", "MT",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("起点位置", "起点位置", "text"),
                    new Column("终点位置", "终点位置", "text"),
                    new Column("长度", "长度", "text"),
                    new Column("级别", "级别", "text"),
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 射线检测：与 Word 缺陷表一致，9 列均为 text
            ExperimentTypeConfig.of("射线检测", "RT",
                createTableSchema(
                    new Column("序号", "序号", "text"),
                    new Column("焊接接头编号", "焊接接头编号", "text"),
                    new Column("底片编号", "底片编号", "text"),
                    new Column("黑度", "黑度", "text"),
                    new Column("厚度 mm", "厚度 mm", "text"),
                    new Column("识别丝号", "识别丝号", "text"),
                    new Column("缺陷位置、性质及数量", "缺陷位置、性质及数量", "text"),
                    new Column("评定级别", "评定级别", "text"),
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 内窥镜检测：检测数据表仅「备注」（检测方式在检测内容行 detectionContent.rows[].method）
            ExperimentTypeConfig.of("内窥镜检测", "VT",
                createTableSchema(
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 涡流检测：编号、缺陷位置、幅值（dB）、相位（°）、减薄量
            ExperimentTypeConfig.of("涡流检测", "ET",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("缺陷位置", "缺陷位置", "text"),
                    new Column("幅值", "幅值（dB）", "text"),
                    new Column("相位", "相位（°）", "text"),
                    new Column("减薄量", "减薄量", "text")
                ),
                reportFieldsSchemaTemplate),
            // 超声波测厚：测点编号、实测厚度（mm）最小需要厚度在检测内容中整份填写
            ExperimentTypeConfig.of("超声波测厚", "UTM",
                createTableSchema(
                    new Column("测点编号", "测点编号", "text"),
                    new Column("实测厚度", "实测厚度（mm）", "text")
                ),
                reportFieldsSchemaTemplate),
            // 管径测量：测点编号、实测管径（mm）
            ExperimentTypeConfig.of("管径测量", "PDM",
                createTableSchema(
                    new Column("测点编号", "测点编号", "text"),
                    new Column("实测管径", "实测管径（mm）", "text")
                ),
                reportFieldsSchemaTemplate),
            // 氧化皮堆积检测：编号、堆积量（％）
            ExperimentTypeConfig.of("氧化皮堆积检测", "SOD",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("堆积量", "堆积量（％）", "text")
                ),
                reportFieldsSchemaTemplate),
            // 里氏硬度检测：编号、里氏分类（管件/钢管/焊缝，可保存后弹窗确认）、1～5、平均
            ExperimentTypeConfig.of("里氏硬度检测", "LHT",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("里氏分类", "里氏分类", "text"),
                    new Column("1", "1", "text"),
                    new Column("2", "2", "text"),
                    new Column("3", "3", "text"),
                    new Column("4", "4", "text"),
                    new Column("5", "5", "text"),
                    new Column("平均", "平均", "text")
                ),
                reportFieldsSchemaTemplate),
            // 布氏硬度检测：编号、1、2、3、平均
            ExperimentTypeConfig.of("布氏硬度检测", "BHT",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("1", "1", "text"),
                    new Column("2", "2", "text"),
                    new Column("3", "3", "text"),
                    new Column("平均", "平均", "text")
                ),
                reportFieldsSchemaTemplate),
            // 金相检测：备注
            ExperimentTypeConfig.of("金相检测", "MET",
                createTableSchema(
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 合金分析检测：编号（元素列由前端动态生成）
            ExperimentTypeConfig.of("合金分析检测", "PMI",
                createTableSchema(
                    new Column("编号", "编号", "text")
                ),
                reportFieldsSchemaTemplate),
            // 相控阵超声波检测：序号、位置、波幅（dB）、深度（mm）、长度（mm）、高度（mm）、级别、备注
            ExperimentTypeConfig.of("相控阵超声波检测", "PAUT",
                createTableSchema(
                    new Column("序号", "序号", "text"),
                    new Column("位置", "位置", "text"),
                    new Column("波幅", "波幅（dB）", "text"),
                    new Column("深度", "深度（mm）", "text"),
                    new Column("长度", "长度（mm）", "text"),
                    new Column("高度", "高度（mm）", "text"),
                    new Column("级别", "级别", "text"),
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate),
            // 圆度测量：弯头编号、公称直径（mm）、弧面直径（mm）、侧面直径（mm）、测量圆度值、允许圆度值
            ExperimentTypeConfig.of("圆度测量", "RDM",
                createTableSchema(
                    new Column("弯头编号", "弯头编号", "text"),
                    new Column("公称直径", "公称直径（mm）", "text"),
                    new Column("弧面直径", "弧面直径（mm）", "text"),
                    new Column("侧面直径", "侧面直径（mm）", "text"),
                    new Column("测量圆度值", "测量圆度值", "text"),
                    new Column("允许圆度值", "允许圆度值", "text")
                ),
                reportFieldsSchemaTemplate),
            // 维氏硬度检测：编号、1、2、3、平均
            ExperimentTypeConfig.of("维氏硬度检测", "VHT",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("1", "1", "text"),
                    new Column("2", "2", "text"),
                    new Column("3", "3", "text"),
                    new Column("平均", "平均", "text")
                ),
                reportFieldsSchemaTemplate),
            // 洛氏硬度检测：编号、1、2、3、4、5、平均
            ExperimentTypeConfig.of("洛氏硬度检测", "RHT",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("1", "1", "text"),
                    new Column("2", "2", "text"),
                    new Column("3", "3", "text"),
                    new Column("4", "4", "text"),
                    new Column("5", "5", "text"),
                    new Column("平均", "平均", "text")
                ),
                reportFieldsSchemaTemplate),
            // 冲击吸收能量检测：编号、1、2、3
            ExperimentTypeConfig.of("冲击吸收能量检测", "IMP",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("1", "1", "text"),
                    new Column("2", "2", "text"),
                    new Column("3", "3", "text")
                ),
                reportFieldsSchemaTemplate),
            // 室温拉伸检测：编号、抗拉强度Rm/MPa、下屈服强度或规定塑性延伸强度ReL或RP0.2/MPa、断后伸长率A/%
            ExperimentTypeConfig.of("室温拉伸检测", "RTN",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("抗拉强度Rm", "抗拉强度Rm/MPa", "text"),
                    new Column("下屈服强度或规定塑性延伸强度ReL或RP0.2", "下屈服强度或规定塑性延伸强度ReL或RP0.2/MPa", "text"),
                    new Column("断后伸长率A", "断后伸长率A/%", "text")
                ),
                reportFieldsSchemaTemplate),
            // 高温拉伸检测：编号、抗拉强度 R/MPa、高温规定塑性延伸强度 R/MPa、断后伸长率 A/%
            ExperimentTypeConfig.of("高温拉伸检测", "HTN",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("抗拉强度", "抗拉强度 R/MPa", "text"),
                    new Column("高温规定塑性延伸强度", "高温规定塑性延伸强度 R/MPa", "text"),
                    new Column("断后伸长率", "断后伸长率 A/%", "text")
                ),
                reportFieldsSchemaTemplate),
            // 高温持久强度检测：编号、断裂时间tu/h、断后伸长率A/%
            ExperimentTypeConfig.of("高温持久强度检测", "HTC",
                createTableSchema(
                    new Column("编号", "编号", "text"),
                    new Column("断裂时间tu", "断裂时间tu/h", "text"),
                    new Column("断后伸长率A", "断后伸长率A/%", "text")
                ),
                reportFieldsSchemaTemplate),
            // 有效硬化层深度检测：至边缘距离、硬度
            ExperimentTypeConfig.of("有效硬化层深度检测", "CHD",
                createTableSchema(
                    new Column("至边缘距离", "至边缘距离", "text"),
                    new Column("硬度", "硬度", "text")
                ),
                reportFieldsSchemaTemplate),
            // 目视检测：检测数据占位列（实际结果在 detectionContent visual-groups）
            ExperimentTypeConfig.of("目视检测", "VIS",
                createTableSchema(
                    new Column("备注", "备注", "text")
                ),
                reportFieldsSchemaTemplate)
        );

        configs.forEach(this::upsertExperimentType);
        log.info("Experiment types initialization completed");
    }

    private static String createTableSchema(Column... columns) {
        StringBuilder sb = new StringBuilder("{\"columns\":[");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"key\":\"%s\",\"label\":\"%s\",\"type\":\"%s\"",
                columns[i].key, columns[i].label, columns[i].type));
            if (columns[i].options != null && !columns[i].options.isEmpty()) {
                sb.append(",\"options\":[");
                for (int j = 0; j < columns[i].options.size(); j++) {
                    if (j > 0) sb.append(",");
                    sb.append("\"").append(columns[i].options.get(j)).append("\"");
                }
                sb.append("]");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static class Column {
        final String key;
        final String label;
        final String type;
        final List<String> options;  // 新增字段

        Column(String key, String label, String type) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.options = null;  // 默认无选项
        }

        Column(String key, String label, String type, List<String> options) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.options = options;
        }
    }

    private void upsertExperimentType(ExperimentTypeConfig config) {
        ExperimentType existing = experimentTypeRepository.findByCode(config.code()).orElse(null);
        if (existing == null) {
            String legacyCode = legacyCodeFor(config.code());
            if (legacyCode != null) {
                existing = experimentTypeRepository.findByCode(legacyCode).orElse(null);
                if (existing != null) {
                    existing.setCode(config.code());
                }
            }
        }
        if (existing == null) {
            ExperimentType type = new ExperimentType();
            type.setName(config.name());
            type.setCode(config.code());
            type.setTableSchema(config.tableSchema());
            type.setReportFieldsSchema(config.reportFieldsSchema());
            type.setIsActive(true);
            experimentTypeRepository.save(type);
            log.info("Created experiment type: {} ({})", config.name(), config.code());
            return;
        }

        boolean updated = false;
        if (!config.name().equals(existing.getName())) {
            existing.setName(config.name());
            updated = true;
        }
        if (existing.getTableSchema() == null || !existing.getTableSchema().equals(config.tableSchema())) {
            existing.setTableSchema(config.tableSchema());
            updated = true;
        }
        if (existing.getReportFieldsSchema() == null || !existing.getReportFieldsSchema().equals(config.reportFieldsSchema())) {
            existing.setReportFieldsSchema(config.reportFieldsSchema());
            updated = true;
        }
        if (existing.getIsActive() == null || !existing.getIsActive()) {
            existing.setIsActive(true);
            updated = true;
        }

        if (updated) {
            experimentTypeRepository.save(existing);
            log.info("Updated experiment type: {} ({})", config.name(), config.code());
        } else {
            log.info("Experiment type {} ({}) already up-to-date", config.name(), config.code());
        }
    }

    private static String legacyCodeFor(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "UTM" -> "UTT";
            case "LHT" -> "LHD";
            case "VHT" -> "VHN";
            case "RHT" -> "RHN";
            case "PMI" -> "AAT";
            case "BHT" -> "BHD";
            default -> null;
        };
    }

    private record ExperimentTypeConfig(String name, String code, String tableSchema, String reportFieldsSchema) {
        static ExperimentTypeConfig of(String name, String code, String tableSchema, String reportFieldsSchema) {
            return new ExperimentTypeConfig(name, code, tableSchema, reportFieldsSchema);
        }
    }

    private void fixEmptyFieldsInReports() {
        try {
            log.info("开始检查并修复Reports表中的空值...");
            List<Report> allReports = reportRepository.findAll();
            int fixedCount = 0;

            for (Report report : allReports) {
                boolean needsUpdate = false;
                
                // 检查并修复location字段
                if (report.getLocation() == null || report.getLocation().trim().isEmpty()) {
                    report.setLocation("/");
                    needsUpdate = true;
                }
                
                // 检查并修复inspector字段
                if (report.getInspector() == null || report.getInspector().trim().isEmpty()) {
                    report.setInspector("/");
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    reportRepository.save(report);
                    fixedCount++;
                }
            }

            log.info("空值修复完成：总记录数={}, 修复记录数={}", allReports.size(), fixedCount);
        } catch (Exception e) {
            log.error("修复Reports表空值时发生错误", e);
        }
    }
}
