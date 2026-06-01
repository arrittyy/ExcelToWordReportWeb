package com.reportweb.controller;

import com.reportweb.entity.ExperimentType;
import com.reportweb.repository.ExperimentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final ExperimentTypeRepository experimentTypeRepository;

    @PostMapping("/fix-experiment-types")
    public ResponseEntity<Map<String, Object>> fixExperimentTypes() {
        try {
            log.info("开始修复检测类型配置...");
            
            // 磁粉检测 (MT)
            ExperimentType mt = experimentTypeRepository.findByName("磁粉检测");
            if (mt != null) {
                mt.setReportFieldsSchema("{\"fields\":[{\"name\":\"serialNumber\",\"label\":\"序号\",\"type\":\"text\",\"autoGenerate\":true},{\"name\":\"equipmentCategory\",\"label\":\"设备类别\",\"type\":\"text\"},{\"name\":\"equipmentName\",\"label\":\"设备名称\",\"type\":\"text\"},{\"name\":\"componentSpec\",\"label\":\"部件规格\",\"type\":\"text\"},{\"name\":\"instrumentModel\",\"label\":\"仪器型号\",\"type\":\"text\"},{\"name\":\"inspector\",\"label\":\"检测人员\",\"type\":\"text\"},{\"name\":\"location\",\"label\":\"检测地点\",\"type\":\"text\"},{\"name\":\"testDate\",\"label\":\"检测日期\",\"type\":\"date\"}]}");
                mt.setTableSchema("{\"columns\":[{\"name\":\"序号\",\"type\":\"text\"},{\"name\":\"起始位置\",\"type\":\"text\"},{\"name\":\"终点位置\",\"type\":\"text\"},{\"name\":\"长度\",\"type\":\"text\"},{\"name\":\"级别\",\"type\":\"select\",\"options\":[\"I\",\"II\",\"III\",\"IV\"]},{\"name\":\"备注\",\"type\":\"text\"}]}");
                experimentTypeRepository.save(mt);
                log.info("已修复磁粉检测配置");
            }

            // 渗透检测 (PT)
            ExperimentType pt = experimentTypeRepository.findByName("渗透检测");
            if (pt != null) {
                pt.setReportFieldsSchema("{\"fields\":[{\"name\":\"serialNumber\",\"label\":\"序号\",\"type\":\"text\",\"autoGenerate\":true},{\"name\":\"equipmentCategory\",\"label\":\"设备类别\",\"type\":\"text\"},{\"name\":\"equipmentName\",\"label\":\"设备名称\",\"type\":\"text\"},{\"name\":\"componentSpec\",\"label\":\"部件规格\",\"type\":\"text\"},{\"name\":\"instrumentModel\",\"label\":\"仪器型号\",\"type\":\"text\"},{\"name\":\"inspector\",\"label\":\"检测人员\",\"type\":\"text\"},{\"name\":\"location\",\"label\":\"检测地点\",\"type\":\"text\"},{\"name\":\"testDate\",\"label\":\"检测日期\",\"type\":\"date\"}]}");
                pt.setTableSchema("{\"columns\":[{\"name\":\"序号\",\"type\":\"text\"},{\"name\":\"起始位置\",\"type\":\"text\"},{\"name\":\"终点位置\",\"type\":\"text\"},{\"name\":\"长度\",\"type\":\"text\"},{\"name\":\"级别\",\"type\":\"select\",\"options\":[\"I\",\"II\",\"III\",\"IV\"]},{\"name\":\"备注\",\"type\":\"text\"}]}");
                experimentTypeRepository.save(pt);
                log.info("已修复渗透检测配置");
            }

            // 超声检测 (UT)
            ExperimentType ut = experimentTypeRepository.findByName("超声检测");
            if (ut != null) {
                ut.setReportFieldsSchema("{\"fields\":[{\"name\":\"serialNumber\",\"label\":\"序号\",\"type\":\"text\",\"autoGenerate\":true},{\"name\":\"equipmentCategory\",\"label\":\"设备类别\",\"type\":\"text\"},{\"name\":\"equipmentName\",\"label\":\"设备名称\",\"type\":\"text\"},{\"name\":\"componentSpec\",\"label\":\"部件规格\",\"type\":\"text\"},{\"name\":\"instrumentModel\",\"label\":\"仪器型号\",\"type\":\"text\"},{\"name\":\"inspector\",\"label\":\"检测人员\",\"type\":\"text\"},{\"name\":\"location\",\"label\":\"检测地点\",\"type\":\"text\"},{\"name\":\"testDate\",\"label\":\"检测日期\",\"type\":\"date\"}]}");
                ut.setTableSchema("{\"columns\":[{\"name\":\"序号\",\"type\":\"text\"},{\"name\":\"起始位置\",\"type\":\"text\"},{\"name\":\"终点位置\",\"type\":\"text\"},{\"name\":\"长度\",\"type\":\"text\"},{\"name\":\"级别\",\"type\":\"select\",\"options\":[\"I\",\"II\",\"III\",\"IV\"]},{\"name\":\"备注\",\"type\":\"text\"}]}");
                experimentTypeRepository.save(ut);
                log.info("已修复超声检测配置");
            }

            // 射线检测 (RT)
            ExperimentType rt = experimentTypeRepository.findByName("射线检测");
            if (rt != null) {
                rt.setReportFieldsSchema("{\"fields\":[{\"name\":\"serialNumber\",\"label\":\"序号\",\"type\":\"text\",\"autoGenerate\":true},{\"name\":\"equipmentCategory\",\"label\":\"设备类别\",\"type\":\"text\"},{\"name\":\"equipmentName\",\"label\":\"设备名称\",\"type\":\"text\"},{\"name\":\"componentSpec\",\"label\":\"部件规格\",\"type\":\"text\"},{\"name\":\"instrumentModel\",\"label\":\"仪器型号\",\"type\":\"text\"},{\"name\":\"inspector\",\"label\":\"检测人员\",\"type\":\"text\"},{\"name\":\"location\",\"label\":\"检测地点\",\"type\":\"text\"},{\"name\":\"testDate\",\"label\":\"检测日期\",\"type\":\"date\"}]}");
                rt.setTableSchema("{\"columns\":[{\"key\":\"序号\",\"label\":\"序号\",\"type\":\"text\"},{\"key\":\"焊接接头编号\",\"label\":\"焊接接头编号\",\"type\":\"text\"},{\"key\":\"底片编号\",\"label\":\"底片编号\",\"type\":\"text\"},{\"key\":\"黑度\",\"label\":\"黑度\",\"type\":\"text\"},{\"key\":\"厚度 mm\",\"label\":\"厚度 mm\",\"type\":\"text\"},{\"key\":\"识别丝号\",\"label\":\"识别丝号\",\"type\":\"text\"},{\"key\":\"缺陷位置、性质及数量\",\"label\":\"缺陷位置、性质及数量\",\"type\":\"text\"},{\"key\":\"评定级别\",\"label\":\"评定级别\",\"type\":\"text\"},{\"key\":\"备注\",\"label\":\"备注\",\"type\":\"text\"}]}");
                experimentTypeRepository.save(rt);
                log.info("已修复射线检测配置");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "检测类型配置已修复");
            response.put("fixedTypes", new String[]{"磁粉检测", "渗透检测", "超声检测", "射线检测"});
            
            log.info("检测类型配置修复完成");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("修复检测类型配置失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "修复失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/check-experiment-types")
    public ResponseEntity<Map<String, Object>> checkExperimentTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> types = new HashMap<>();
            
            String[] typeNames = {"磁粉检测", "渗透检测", "超声检测", "射线检测"};
            
            for (String typeName : typeNames) {
                ExperimentType type = experimentTypeRepository.findByName(typeName);
                if (type != null) {
                    Map<String, Object> typeInfo = new HashMap<>();
                    typeInfo.put("id", type.getId());
                    typeInfo.put("name", type.getName());
                    typeInfo.put("code", type.getCode());
                    typeInfo.put("reportFieldsSchemaLength", type.getReportFieldsSchema() != null ? type.getReportFieldsSchema().length() : 0);
                    typeInfo.put("tableSchemaLength", type.getTableSchema() != null ? type.getTableSchema().length() : 0);
                    typeInfo.put("isActive", type.getIsActive());
                    types.put(typeName, typeInfo);
                } else {
                    types.put(typeName, "NOT_FOUND");
                }
            }
            
            response.put("success", true);
            response.put("types", types);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("检查检测类型配置失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
