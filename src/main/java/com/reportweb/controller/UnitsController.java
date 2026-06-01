package com.reportweb.controller;

import com.reportweb.dto.UnitDTOs;
import com.reportweb.dto.UnitComponentDTOs;
import com.reportweb.entity.Unit;
import com.reportweb.entity.UnitComponent;
import com.reportweb.entity.PowerPlant;
import com.reportweb.repository.UnitRepository;
import com.reportweb.repository.UnitComponentRepository;
import com.reportweb.repository.PowerPlantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UnitsController {

    private final UnitRepository unitRepository;
    private final UnitComponentRepository unitComponentRepository;
    private final PowerPlantRepository powerPlantRepository;

    @GetMapping("/power-plants/{powerPlantId}/units")
    @Transactional
    public ResponseEntity<List<UnitDTOs.UnitList>> getUnitsByPowerPlantId(
            @PathVariable Integer powerPlantId) {
        try {
            if (!powerPlantRepository.existsById(powerPlantId)) {
                return ResponseEntity.notFound().build();
            }

            List<Unit> units = unitRepository.findByPowerPlantId(powerPlantId);
            List<UnitDTOs.UnitList> unitList = units.stream()
                    .map(this::convertToUnitListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(unitList);
        } catch (Exception ex) {
            log.error("Error getting units for power plant {}", powerPlantId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/units/{id}")
    @Transactional
    public ResponseEntity<UnitDTOs.UnitResponse> getUnit(@PathVariable Integer id) {
        try {
            Unit unit = unitRepository.findById(id).orElse(null);
            if (unit == null) {
                return ResponseEntity.notFound().build();
            }

            UnitDTOs.UnitResponse response = convertToUnitResponseDTO(unit);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error getting unit with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/power-plants/{powerPlantId}/units")
    public ResponseEntity<?> createUnit(
            @PathVariable Integer powerPlantId,
            @Valid @RequestBody UnitDTOs.CreateUnit createUnitDTO) {
        try {
            if (!powerPlantRepository.existsById(powerPlantId)) {
                return ResponseEntity.notFound().build();
            }

            // 检查机组编号是否重复
            if (createUnitDTO.getUnitNumber() != null && !createUnitDTO.getUnitNumber().trim().isEmpty()) {
                if (unitRepository.existsByPowerPlantIdAndUnitNumber(powerPlantId, createUnitDTO.getUnitNumber())) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "机组编号已存在");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }

            // 生成机组名称：电厂名称 + 机组编号
            PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                    .orElseThrow(() -> new RuntimeException("电厂不存在"));

            String unitName = powerPlant.getName();
            if (createUnitDTO.getUnitNumber() != null && !createUnitDTO.getUnitNumber().trim().isEmpty()) {
                unitName += "-" + createUnitDTO.getUnitNumber();
            }

            Unit unit = new Unit();
            unit.setPowerPlantId(powerPlantId);
            unit.setUnitName(unitName);
            unit.setUnitNumber(createUnitDTO.getUnitNumber());
            unit.setInstalledCapacity(createUnitDTO.getInstalledCapacity());
            unit.setCreatedAt(LocalDateTime.now());
            unit.setUpdatedAt(LocalDateTime.now());

            Unit savedUnit = unitRepository.save(unit);

            UnitDTOs.UnitResponse response = convertToUnitResponseDTO(savedUnit);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating unit", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建机组失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/units/{id}")
    public ResponseEntity<?> updateUnit(
            @PathVariable Integer id,
            @Valid @RequestBody UnitDTOs.UpdateUnit updateUnitDTO) {
        try {
            Unit unit = unitRepository.findById(id).orElse(null);
            if (unit == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查机组编号是否重复（排除当前机组）
            if (updateUnitDTO.getUnitNumber() != null && !updateUnitDTO.getUnitNumber().trim().isEmpty()) {
                if (!updateUnitDTO.getUnitNumber().equals(unit.getUnitNumber()) &&
                        unitRepository.existsByPowerPlantIdAndUnitNumberAndIdNot(unit.getPowerPlantId(), updateUnitDTO.getUnitNumber(), id)) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "机组编号已存在");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }

            // 更新机组名称
            PowerPlant powerPlant = powerPlantRepository.findById(unit.getPowerPlantId())
                    .orElseThrow(() -> new RuntimeException("电厂不存在"));

            String unitName = powerPlant.getName();
            if (updateUnitDTO.getUnitNumber() != null && !updateUnitDTO.getUnitNumber().trim().isEmpty()) {
                unitName += "-" + updateUnitDTO.getUnitNumber();
            }

            unit.setUnitName(unitName);
            unit.setUnitNumber(updateUnitDTO.getUnitNumber());
            unit.setInstalledCapacity(updateUnitDTO.getInstalledCapacity());
            unit.setUpdatedAt(LocalDateTime.now());

            unitRepository.save(unit);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error updating unit with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新机组失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/units/{id}")
    public ResponseEntity<?> deleteUnit(@PathVariable Integer id) {
        try {
            if (!unitRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            unitRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting unit with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除机组失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private UnitDTOs.UnitList convertToUnitListDTO(Unit unit) {
        UnitDTOs.UnitList dto = new UnitDTOs.UnitList();
        dto.setId(unit.getId());
        dto.setPowerPlantId(unit.getPowerPlantId());
        dto.setUnitName(unit.getUnitName());
        dto.setUnitNumber(unit.getUnitNumber());
        dto.setInstalledCapacity(unit.getInstalledCapacity());
        dto.setRemark(unit.getRemark());
        dto.setCreatedAt(unit.getCreatedAt());
        dto.setUpdatedAt(unit.getUpdatedAt());
        return dto;
    }

    private UnitDTOs.UnitResponse convertToUnitResponseDTO(Unit unit) {
        UnitDTOs.UnitResponse dto = new UnitDTOs.UnitResponse();
        dto.setId(unit.getId());
        dto.setPowerPlantId(unit.getPowerPlantId());
        dto.setUnitName(unit.getUnitName());
        dto.setUnitNumber(unit.getUnitNumber());
        dto.setInstalledCapacity(unit.getInstalledCapacity());
        dto.setRemark(unit.getRemark());
        dto.setCreatedAt(unit.getCreatedAt());
        dto.setUpdatedAt(unit.getUpdatedAt());

        // 加载部件列表
        List<UnitComponent> components = unitComponentRepository.findByUnitId(unit.getId());
        List<UnitComponentDTOs.UnitComponentList> componentList = components.stream()
                .map(this::convertToUnitComponentListDTO)
                .collect(Collectors.toList());
        dto.setComponents(componentList);

        return dto;
    }

    private UnitComponentDTOs.UnitComponentList convertToUnitComponentListDTO(UnitComponent component) {
        UnitComponentDTOs.UnitComponentList dto = new UnitComponentDTOs.UnitComponentList();
        dto.setId(component.getId());
        dto.setUnitId(component.getUnitId());
        dto.setComponentName(component.getComponentName());
        dto.setMaterial(component.getMaterial());
        dto.setCategory(component.getCategory());
        dto.setPipeDiameter(component.getPipeDiameter());
        dto.setWallThickness(component.getWallThickness());
        dto.setRemark(component.getRemark());
        dto.setCreatedAt(component.getCreatedAt());
        dto.setUpdatedAt(component.getUpdatedAt());
        return dto;
    }
}
