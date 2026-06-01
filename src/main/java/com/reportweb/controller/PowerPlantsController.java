package com.reportweb.controller;

import com.reportweb.dto.PowerPlantDTOs;
import com.reportweb.dto.UnitDTOs;
import com.reportweb.entity.PowerPlant;
import com.reportweb.entity.Unit;
import com.reportweb.repository.PowerPlantRepository;
import com.reportweb.repository.UnitRepository;
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
@RequestMapping("/api/power-plants")
@RequiredArgsConstructor
@Slf4j
public class PowerPlantsController {

    private final PowerPlantRepository powerPlantRepository;
    private final UnitRepository unitRepository;

    @GetMapping
    public ResponseEntity<List<PowerPlantDTOs.PowerPlantList>> getAllPowerPlants() {
        try {
            List<PowerPlant> powerPlants = powerPlantRepository.findAll();
            List<PowerPlantDTOs.PowerPlantList> powerPlantList = powerPlants.stream()
                    .map(this::convertToPowerPlantListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(powerPlantList);
        } catch (Exception ex) {
            log.error("Error getting power plants", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<PowerPlantDTOs.PowerPlantResponse> getPowerPlant(@PathVariable Integer id) {
        try {
            PowerPlant powerPlant = powerPlantRepository.findById(id).orElse(null);
            if (powerPlant == null) {
                return ResponseEntity.notFound().build();
            }

            // 触发懒加载，确保关联机组已加载
            if (powerPlant.getUnits() != null) {
                powerPlant.getUnits().size();
            }

            PowerPlantDTOs.PowerPlantResponse response = convertToPowerPlantResponseDTO(powerPlant);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error getting power plant with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createPowerPlant(
            @Valid @RequestBody PowerPlantDTOs.CreatePowerPlant createPowerPlantDTO) {
        try {
            // 检查名称是否已存在
            if (powerPlantRepository.existsByName(createPowerPlantDTO.getName())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "电厂名称已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            PowerPlant powerPlant = new PowerPlant();
            powerPlant.setName(createPowerPlantDTO.getName());
            powerPlant.setRegion(createPowerPlantDTO.getRegion());
            powerPlant.setShortName(createPowerPlantDTO.getShortName());
            powerPlant.setProvince(createPowerPlantDTO.getProvince());
            powerPlant.setCity(createPowerPlantDTO.getCity());
            powerPlant.setAddress(createPowerPlantDTO.getAddress());
            powerPlant.setPhone(createPowerPlantDTO.getPhone());
            powerPlant.setFax(createPowerPlantDTO.getFax());
            powerPlant.setRemark(createPowerPlantDTO.getRemark());
            powerPlant.setCreatedAt(LocalDateTime.now());
            powerPlant.setUpdatedAt(LocalDateTime.now());

            PowerPlant savedPowerPlant = powerPlantRepository.save(powerPlant);

            PowerPlantDTOs.PowerPlantResponse response = convertToPowerPlantResponseDTO(savedPowerPlant);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating power plant", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建电厂失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePowerPlant(
            @PathVariable Integer id,
            @Valid @RequestBody PowerPlantDTOs.UpdatePowerPlant updatePowerPlantDTO) {
        try {
            PowerPlant powerPlant = powerPlantRepository.findById(id).orElse(null);
            if (powerPlant == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查名称是否已被其他电厂使用
            if (!powerPlant.getName().equals(updatePowerPlantDTO.getName()) &&
                    powerPlantRepository.existsByNameAndIdNot(updatePowerPlantDTO.getName(), id)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "电厂名称已被其他电厂使用");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            powerPlant.setName(updatePowerPlantDTO.getName());
            powerPlant.setRegion(updatePowerPlantDTO.getRegion());
            powerPlant.setShortName(updatePowerPlantDTO.getShortName());
            powerPlant.setProvince(updatePowerPlantDTO.getProvince());
            powerPlant.setCity(updatePowerPlantDTO.getCity());
            powerPlant.setAddress(updatePowerPlantDTO.getAddress());
            powerPlant.setPhone(updatePowerPlantDTO.getPhone());
            powerPlant.setFax(updatePowerPlantDTO.getFax());
            powerPlant.setRemark(updatePowerPlantDTO.getRemark());
            powerPlant.setUpdatedAt(LocalDateTime.now());

            powerPlantRepository.save(powerPlant);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error updating power plant with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新电厂失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePowerPlant(@PathVariable Integer id) {
        try {
            if (!powerPlantRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            powerPlantRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting power plant with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除电厂失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private PowerPlantDTOs.PowerPlantList convertToPowerPlantListDTO(PowerPlant powerPlant) {
        PowerPlantDTOs.PowerPlantList dto = new PowerPlantDTOs.PowerPlantList();
        dto.setId(powerPlant.getId());
        dto.setName(powerPlant.getName());
        dto.setRegion(powerPlant.getRegion());
        dto.setShortName(powerPlant.getShortName());
        dto.setProvince(powerPlant.getProvince());
        dto.setCity(powerPlant.getCity());
        dto.setAddress(powerPlant.getAddress());
        dto.setPhone(powerPlant.getPhone());
        dto.setFax(powerPlant.getFax());
        dto.setRemark(powerPlant.getRemark());
        dto.setCreatedAt(powerPlant.getCreatedAt());
        dto.setUpdatedAt(powerPlant.getUpdatedAt());
        return dto;
    }

    private PowerPlantDTOs.PowerPlantResponse convertToPowerPlantResponseDTO(PowerPlant powerPlant) {
        PowerPlantDTOs.PowerPlantResponse dto = new PowerPlantDTOs.PowerPlantResponse();
        dto.setId(powerPlant.getId());
        dto.setName(powerPlant.getName());
        dto.setRegion(powerPlant.getRegion());
        dto.setShortName(powerPlant.getShortName());
        dto.setProvince(powerPlant.getProvince());
        dto.setCity(powerPlant.getCity());
        dto.setAddress(powerPlant.getAddress());
        dto.setPhone(powerPlant.getPhone());
        dto.setFax(powerPlant.getFax());
        dto.setRemark(powerPlant.getRemark());
        dto.setCreatedAt(powerPlant.getCreatedAt());
        dto.setUpdatedAt(powerPlant.getUpdatedAt());

        // 加载机组列表
        List<Unit> units = unitRepository.findByPowerPlantId(powerPlant.getId());
        List<UnitDTOs.UnitList> unitList = units.stream()
                .map(this::convertToUnitListDTO)
                .collect(Collectors.toList());
        dto.setUnits(unitList);

        return dto;
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
}
