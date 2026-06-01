package com.reportweb.controller;

import com.reportweb.dto.ExperimentTypeDTOs;
import com.reportweb.entity.ExperimentType;
import com.reportweb.repository.ExperimentTypeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/experimenttypes")
@RequiredArgsConstructor
@Slf4j
public class ExperimentTypesController {

    private final ExperimentTypeRepository experimentTypeRepository;

    @GetMapping
    public ResponseEntity<List<ExperimentTypeDTOs.ExperimentTypeList>> getExperimentTypes() {
        try {
            List<ExperimentType> experimentTypes = experimentTypeRepository.findByIsActiveTrue();
            
            List<ExperimentTypeDTOs.ExperimentTypeList> experimentTypeList = experimentTypes.stream()
                .map(this::convertToExperimentTypeListDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(experimentTypeList);
        } catch (Exception ex) {
            log.error("Error getting experiment types", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperimentTypeDTOs.ExperimentTypeDetail> getExperimentType(@PathVariable Integer id) {
        try {
            ExperimentType experimentType = experimentTypeRepository.findById(id)
                .orElse(null);

            if (experimentType == null) {
                return ResponseEntity.notFound().build();
            }

            ExperimentTypeDTOs.ExperimentTypeDetail experimentTypeDetail = convertToExperimentTypeDetailDTO(experimentType);
            return ResponseEntity.ok(experimentTypeDetail);
        } catch (Exception ex) {
            log.error("Error getting experiment type with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createExperimentType(@Valid @RequestBody ExperimentTypeDTOs.CreateExperimentType createExperimentTypeDTO) {
        try {
            // 检查代码是否已存在
            if (experimentTypeRepository.existsByCode(createExperimentTypeDTO.getCode())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "检测类型代码已存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            ExperimentType experimentType = new ExperimentType();
            experimentType.setName(createExperimentTypeDTO.getName());
            experimentType.setCode(createExperimentTypeDTO.getCode());
            experimentType.setTableSchema(createExperimentTypeDTO.getTableSchema());
            experimentType.setReportFieldsSchema(createExperimentTypeDTO.getReportFieldsSchema());
            experimentType.setIsActive(createExperimentTypeDTO.getIsActive());

            ExperimentType savedExperimentType = experimentTypeRepository.save(experimentType);
            ExperimentTypeDTOs.ExperimentTypeDetail response = convertToExperimentTypeDetailDTO(savedExperimentType);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating experiment type", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建检测类型失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExperimentType(@PathVariable Integer id, @Valid @RequestBody ExperimentTypeDTOs.UpdateExperimentType updateExperimentTypeDTO) {
        try {
            ExperimentType experimentType = experimentTypeRepository.findById(id)
                .orElse(null);

            if (experimentType == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查代码是否已存在（排除当前检测类型）
            if (experimentTypeRepository.existsByCodeAndIdNot(updateExperimentTypeDTO.getCode(), id)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "检测类型代码已存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            experimentType.setName(updateExperimentTypeDTO.getName());
            experimentType.setCode(updateExperimentTypeDTO.getCode());
            experimentType.setTableSchema(updateExperimentTypeDTO.getTableSchema());
            experimentType.setReportFieldsSchema(updateExperimentTypeDTO.getReportFieldsSchema());
            experimentType.setIsActive(updateExperimentTypeDTO.getIsActive());

            experimentTypeRepository.save(experimentType);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error updating experiment type with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新检测类型失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExperimentType(@PathVariable Integer id) {
        try {
            ExperimentType experimentType = experimentTypeRepository.findById(id)
                .orElse(null);

            if (experimentType == null) {
                return ResponseEntity.notFound().build();
            }

            // 软删除：设置为非活跃状态
            experimentType.setIsActive(false);
            experimentTypeRepository.save(experimentType);
            
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting experiment type with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除检测类型失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private ExperimentTypeDTOs.ExperimentTypeList convertToExperimentTypeListDTO(ExperimentType experimentType) {
        ExperimentTypeDTOs.ExperimentTypeList dto = new ExperimentTypeDTOs.ExperimentTypeList();
        dto.setId(experimentType.getId());
        dto.setName(experimentType.getName());
        dto.setCode(experimentType.getCode());
        dto.setTableSchema(experimentType.getTableSchema());
        dto.setReportFieldsSchema(experimentType.getReportFieldsSchema());
        dto.setIsActive(experimentType.getIsActive());
        return dto;
    }

    private ExperimentTypeDTOs.ExperimentTypeDetail convertToExperimentTypeDetailDTO(ExperimentType experimentType) {
        ExperimentTypeDTOs.ExperimentTypeDetail dto = new ExperimentTypeDTOs.ExperimentTypeDetail();
        dto.setId(experimentType.getId());
        dto.setName(experimentType.getName());
        dto.setCode(experimentType.getCode());
        dto.setTableSchema(experimentType.getTableSchema());
        dto.setReportFieldsSchema(experimentType.getReportFieldsSchema());
        dto.setIsActive(experimentType.getIsActive());
        return dto;
    }
}


