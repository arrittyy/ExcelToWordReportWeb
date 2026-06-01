package com.reportweb.controller;

import com.reportweb.dto.UnitComponentDTOs;
import com.reportweb.entity.UnitComponent;
import com.reportweb.repository.UnitComponentRepository;
import com.reportweb.repository.UnitRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class UnitComponentsController {

    private final UnitComponentRepository unitComponentRepository;
    private final UnitRepository unitRepository;

    @GetMapping("/units/{unitId}/components")
    public ResponseEntity<List<UnitComponentDTOs.UnitComponentList>> getUnitComponents(
            @PathVariable Integer unitId) {
        try {
            if (!unitRepository.existsById(unitId)) {
                return ResponseEntity.notFound().build();
            }

            List<UnitComponent> components = unitComponentRepository.findByUnitId(unitId);
            List<UnitComponentDTOs.UnitComponentList> componentList = components.stream()
                    .map(this::convertToUnitComponentListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(componentList);
        } catch (Exception ex) {
            log.error("Error getting unit components for unit {}", unitId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/units/{unitId}/components")
    public ResponseEntity<?> createUnitComponent(
            @PathVariable Integer unitId,
            @Valid @RequestBody UnitComponentDTOs.CreateUnitComponent createComponentDTO) {
        try {
            if (!unitRepository.existsById(unitId)) {
                return ResponseEntity.notFound().build();
            }

            UnitComponent component = new UnitComponent();
            component.setUnitId(unitId);
            component.setComponentName(createComponentDTO.getComponentName());
            component.setMaterial(createComponentDTO.getMaterial());
            component.setCategory(createComponentDTO.getCategory());
            component.setPipeDiameter(createComponentDTO.getPipeDiameter());
            component.setWallThickness(createComponentDTO.getWallThickness());
            component.setRemark(createComponentDTO.getRemark());
            component.setCreatedAt(LocalDateTime.now());
            component.setUpdatedAt(LocalDateTime.now());

            UnitComponent savedComponent = unitComponentRepository.save(component);

            UnitComponentDTOs.UnitComponentList response = convertToUnitComponentListDTO(savedComponent);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating unit component", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/unit-components/{id}")
    public ResponseEntity<?> updateUnitComponent(
            @PathVariable Integer id,
            @Valid @RequestBody UnitComponentDTOs.UpdateUnitComponent updateComponentDTO) {
        try {
            UnitComponent component = unitComponentRepository.findById(id).orElse(null);
            if (component == null) {
                return ResponseEntity.notFound().build();
            }

            component.setComponentName(updateComponentDTO.getComponentName());
            component.setMaterial(updateComponentDTO.getMaterial());
            component.setCategory(updateComponentDTO.getCategory());
            component.setPipeDiameter(updateComponentDTO.getPipeDiameter());
            component.setWallThickness(updateComponentDTO.getWallThickness());
            component.setRemark(updateComponentDTO.getRemark());
            component.setUpdatedAt(LocalDateTime.now());

            unitComponentRepository.save(component);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error updating unit component with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/unit-components/{id}")
    public ResponseEntity<?> deleteUnitComponent(@PathVariable Integer id) {
        try {
            if (!unitComponentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            unitComponentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting unit component with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
