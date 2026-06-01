package com.reportweb.controller;

import com.reportweb.dto.ProjectComponentDTOs;
import com.reportweb.dto.UnitComponentDTOs;
import com.reportweb.entity.Project;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.UnitComponent;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.repository.UnitComponentRepository;
import com.reportweb.service.ReportComponentMergeHelper;
import com.reportweb.security.CustomUserPrincipal;
import com.reportweb.security.UserRoleUtils;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProjectComponentsController {

    private final ProjectComponentRepository projectComponentRepository;
    private final ProjectRepository projectRepository;
    private final UnitComponentRepository unitComponentRepository;
    private final ReportComponentMergeHelper reportComponentMergeHelper;

    private static final String PROJECT_STATUS_IN_PROGRESS = "InProgress";

    /** GET 列表已迁至 {@link ProjectsController#getProjectComponentsNested}，避免与 /api/projects/{id} 路由冲突。 */

    @PostMapping("/projects/{projectId}/components")
    public ResponseEntity<?> createComponent(
            @PathVariable Integer projectId,
            @Valid @RequestBody ProjectComponentDTOs.CreateComponent createComponentDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            // 管理员可访问任何项目；普通用户/子账号只能访问归属 effectiveUserId 的项目；子账号仅能访问进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(projectId).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(projectId, effectiveUserId).orElse(null);
            }
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String specPrefix;
            String threadPitch;
            try {
                specPrefix = ReportComponentMergeHelper.normalizeSpecPrefixForSave(createComponentDTO.getSpecPrefix());
                threadPitch = ReportComponentMergeHelper.normalizeThreadPitchForSave(createComponentDTO.getThreadPitch());
            } catch (IllegalArgumentException ex) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "规格前缀无效");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            ProjectComponent component = new ProjectComponent();
            component.setProjectId(projectId);
            component.setComponentName(createComponentDTO.getComponentName());
            component.setMaterial(createComponentDTO.getMaterial());
            component.setCategory(createComponentDTO.getCategory());
            component.setPipeDiameter(createComponentDTO.getPipeDiameter());
            component.setWallThickness(createComponentDTO.getWallThickness());
            component.setSpecPrefix(specPrefix);
            component.setThreadPitch(threadPitch);
            component.setRemark(createComponentDTO.getRemark());
            component.setCreatedAt(LocalDateTime.now());
            component.setUpdatedAt(LocalDateTime.now());

            ProjectComponent savedComponent = projectComponentRepository.save(component);
            ProjectComponentDTOs.ComponentList response = convertToComponentListDTO(savedComponent);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating component", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<?> updateComponent(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectComponentDTOs.UpdateComponent updateComponentDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            ProjectComponent component = projectComponentRepository.findById(id)
                    .orElse(null);
            if (component == null) {
                return ResponseEntity.notFound().build();
            }

            // 管理员可操作任何项目；普通用户/子账号只能操作归属 effectiveUserId 的项目；子账号仅能操作进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(component.getProjectId()).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(component.getProjectId(), effectiveUserId).orElse(null);
            }
            if (project == null) {
                Map<String, String> forbiddenBody = new HashMap<>();
                forbiddenBody.put("message", "无权访问此项目");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(forbiddenBody);
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String specPrefix;
            String threadPitch;
            try {
                specPrefix = ReportComponentMergeHelper.normalizeSpecPrefixForSave(updateComponentDTO.getSpecPrefix());
                threadPitch = ReportComponentMergeHelper.normalizeThreadPitchForSave(updateComponentDTO.getThreadPitch());
            } catch (IllegalArgumentException ex) {
                Map<String, String> invalidPrefixBody = new HashMap<>();
                invalidPrefixBody.put("message", ex.getMessage() != null ? ex.getMessage() : "规格前缀无效");
                return ResponseEntity.badRequest().body(invalidPrefixBody);
            }

            component.setComponentName(updateComponentDTO.getComponentName());
            component.setMaterial(updateComponentDTO.getMaterial());
            component.setCategory(updateComponentDTO.getCategory());
            component.setPipeDiameter(updateComponentDTO.getPipeDiameter());
            component.setWallThickness(updateComponentDTO.getWallThickness());
            component.setSpecPrefix(specPrefix);
            component.setThreadPitch(threadPitch);
            component.setRemark(updateComponentDTO.getRemark());
            component.setUpdatedAt(LocalDateTime.now());

            ProjectComponent updatedComponent = projectComponentRepository.save(component);
            ProjectComponentDTOs.ComponentList response = convertToComponentListDTO(updatedComponent);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error updating component", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/components/{id}")
    public ResponseEntity<?> deleteComponent(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            ProjectComponent component = projectComponentRepository.findById(id)
                    .orElse(null);
            if (component == null) {
                return ResponseEntity.notFound().build();
            }

            // 管理员可操作任何项目；普通用户/子账号只能操作归属 effectiveUserId 的项目；子账号仅能操作进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(component.getProjectId()).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(component.getProjectId(), effectiveUserId).orElse(null);
            }
            if (project == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "无权访问此项目");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            projectComponentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting component", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/projects/{projectId}/available-components")
    public ResponseEntity<AvailableComponentsResponse> getAvailableComponents(
            @PathVariable Integer projectId,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            // 管理员可访问任何项目；普通用户/子账号只能访问归属 effectiveUserId 的项目；子账号仅能访问进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(projectId).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(projectId, effectiveUserId).orElse(null);
            }
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            // 获取项目部件
            List<ProjectComponent> projectComponents = projectComponentRepository.findByProjectId(projectId);
            List<ProjectComponentDTOs.ComponentList> projectComponentList = projectComponents.stream()
                    .map(this::convertToComponentListDTO)
                    .collect(Collectors.toList());

            // 获取机组部件（如果项目关联了机组）
            List<UnitComponentDTOs.UnitComponentList> unitComponentList = new ArrayList<>();
            if (project.getUnitId() != null) {
                List<UnitComponent> unitComponents = unitComponentRepository.findByUnitId(project.getUnitId());
                unitComponentList = unitComponents.stream()
                        .map(this::convertToUnitComponentListDTO)
                        .collect(Collectors.toList());
            }

            AvailableComponentsResponse response = new AvailableComponentsResponse();
            response.setProjectComponents(projectComponentList);
            response.setUnitComponents(unitComponentList);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error getting available components", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/projects/{projectId}/components/import-from-unit")
    public ResponseEntity<?> importComponentsFromUnit(
            @PathVariable Integer projectId,
            @RequestBody ImportFromUnitRequest request,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);

            // 管理员可以访问任何项目，普通用户只能访问自己的项目
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(projectId).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(projectId, userId).orElse(null);
            }
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.getUnitComponentIds() == null || request.getUnitComponentIds().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "请选择要导入的部件");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 获取要导入的机组部件
            List<UnitComponent> unitComponents = unitComponentRepository.findAllById(request.getUnitComponentIds());
            
            // 创建项目部件
            List<ProjectComponent> importedComponents = new ArrayList<>();
            for (UnitComponent unitComponent : unitComponents) {
                ProjectComponent projectComponent = new ProjectComponent();
                projectComponent.setProjectId(projectId);
                projectComponent.setComponentName(unitComponent.getComponentName());
                projectComponent.setMaterial(unitComponent.getMaterial());
                projectComponent.setCategory(unitComponent.getCategory());
                projectComponent.setPipeDiameter(unitComponent.getPipeDiameter());
                projectComponent.setWallThickness(unitComponent.getWallThickness());
                projectComponent.setSpecPrefix(null);
                projectComponent.setThreadPitch(null);
                projectComponent.setRemark(unitComponent.getRemark());
                projectComponent.setCreatedAt(LocalDateTime.now());
                projectComponent.setUpdatedAt(LocalDateTime.now());
                
                ProjectComponent saved = projectComponentRepository.save(projectComponent);
                importedComponents.add(saved);
            }

            ImportFromUnitResponse response = new ImportFromUnitResponse();
            response.setImportedComponents(importedComponents.stream()
                    .map(this::convertToComponentListDTO)
                    .collect(Collectors.toList()));
            response.setCount(importedComponents.size());

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error importing components from unit", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "导入部件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private ProjectComponentDTOs.ComponentList convertToComponentListDTO(ProjectComponent component) {
        ProjectComponentDTOs.ComponentList dto = new ProjectComponentDTOs.ComponentList();
        dto.setId(component.getId());
        dto.setProjectId(component.getProjectId());
        dto.setComponentName(component.getComponentName());
        dto.setMaterial(component.getMaterial());
        dto.setCategory(component.getCategory());
        dto.setPipeDiameter(component.getPipeDiameter());
        dto.setWallThickness(component.getWallThickness());
        dto.setSpecPrefix(component.getSpecPrefix());
        dto.setThreadPitch(component.getThreadPitch());
        dto.setDisplaySpec(reportComponentMergeHelper.formatSpecUnified(component));
        dto.setRemark(component.getRemark());
        dto.setCreatedAt(component.getCreatedAt());
        dto.setUpdatedAt(component.getUpdatedAt());
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

    @Data
    public static class AvailableComponentsResponse {
        private List<ProjectComponentDTOs.ComponentList> projectComponents;
        private List<UnitComponentDTOs.UnitComponentList> unitComponents;
    }

    @Data
    public static class ImportFromUnitRequest {
        private List<Integer> unitComponentIds;
    }

    @Data
    public static class ImportFromUnitResponse {
        private List<ProjectComponentDTOs.ComponentList> importedComponents;
        private Integer count;
    }
}

