package com.reportweb.controller;

import com.reportweb.dto.ProjectInstrumentDTOs;
import com.reportweb.entity.Project;
import com.reportweb.entity.ProjectInstrument;
import com.reportweb.repository.ExperimentTypeRepository;
import com.reportweb.repository.ProjectInstrumentRepository;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.security.CustomUserPrincipal;
import com.reportweb.security.UserRoleUtils;
import com.reportweb.service.InstrumentTypeMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
public class ProjectInstrumentsController {

    private final ProjectInstrumentRepository projectInstrumentRepository;
    private final ProjectRepository projectRepository;
    private final ExperimentTypeRepository experimentTypeRepository;

    private static final String PROJECT_STATUS_IN_PROGRESS = "InProgress";

    /** GET 列表已迁至 {@link ProjectsController#getProjectInstrumentsNested}。 */

    @PostMapping("/projects/{projectId}/instruments")
    public ResponseEntity<?> createInstrument(
            @PathVariable Integer projectId,
            @Valid @RequestBody ProjectInstrumentDTOs.CreateInstrument createInstrumentDTO,
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

            // 检查仪器名称是否已存在
            if (projectInstrumentRepository.existsByProjectIdAndInstrumentName(projectId, createInstrumentDTO.getInstrumentName())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "仪器名称已存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String experimentTypeCode = resolveExperimentTypeCode(
                    createInstrumentDTO.getExperimentTypeCode(),
                    createInstrumentDTO.getGlobalInstrumentId()
            );
            if (experimentTypeCode != null && !experimentTypeRepository.existsByCode(experimentTypeCode)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "检测类型无效: " + experimentTypeCode);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 如果设置为默认设备，需要取消同类型其他设备的默认状态
            if (Boolean.TRUE.equals(createInstrumentDTO.getIsDefault()) && experimentTypeCode != null) {
                List<ProjectInstrument> existingDefaults = projectInstrumentRepository
                    .findByProjectIdAndExperimentTypeCodeAndIsDefault(projectId, experimentTypeCode, true);
                for (ProjectInstrument existing : existingDefaults) {
                    existing.setIsDefault(false);
                    projectInstrumentRepository.save(existing);
                }
            }

            ProjectInstrument instrument = new ProjectInstrument();
            instrument.setProjectId(projectId);
            instrument.setInstrumentName(createInstrumentDTO.getInstrumentName());
            instrument.setInstrumentModel(createInstrumentDTO.getInstrumentModel());
            instrument.setInstrumentNumber(createInstrumentDTO.getInstrumentNumber());
            instrument.setGlobalInstrumentId(createInstrumentDTO.getGlobalInstrumentId());
            instrument.setIsDefault(createInstrumentDTO.getIsDefault() != null ? createInstrumentDTO.getIsDefault() : false);
            instrument.setExperimentTypeCode(experimentTypeCode);
            instrument.setCreatedAt(LocalDateTime.now());
            instrument.setUpdatedAt(LocalDateTime.now());

            ProjectInstrument savedInstrument = projectInstrumentRepository.save(instrument);
            ProjectInstrumentDTOs.InstrumentList response = convertToInstrumentListDTO(savedInstrument);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating instrument", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建仪器设备失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/project-instruments/{id}")
    public ResponseEntity<?> updateInstrument(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectInstrumentDTOs.UpdateInstrument updateInstrumentDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            ProjectInstrument instrument = projectInstrumentRepository.findById(id)
                    .orElse(null);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            // 管理员可操作任何项目；普通用户/子账号只能操作归属 effectiveUserId 的项目；子账号仅能操作进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(instrument.getProjectId()).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(instrument.getProjectId(), effectiveUserId).orElse(null);
            }
            if (project == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "无权访问此项目");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 检查仪器名称是否已存在（排除当前记录）
            if (projectInstrumentRepository.existsByProjectIdAndInstrumentNameAndIdNot(
                    instrument.getProjectId(), updateInstrumentDTO.getInstrumentName(), id)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "仪器名称已存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String experimentTypeCode = resolveExperimentTypeCode(
                    updateInstrumentDTO.getExperimentTypeCode(),
                    updateInstrumentDTO.getGlobalInstrumentId()
            );
            if (experimentTypeCode != null && !experimentTypeRepository.existsByCode(experimentTypeCode)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "检测类型无效: " + experimentTypeCode);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 如果设置为默认设备，需要取消同类型其他设备的默认状态
            if (Boolean.TRUE.equals(updateInstrumentDTO.getIsDefault()) && experimentTypeCode != null) {
                List<ProjectInstrument> existingDefaults = projectInstrumentRepository
                    .findByProjectIdAndExperimentTypeCodeAndIsDefault(
                        instrument.getProjectId(), experimentTypeCode, true);
                for (ProjectInstrument existing : existingDefaults) {
                    if (!existing.getId().equals(id)) { // 排除当前记录
                        existing.setIsDefault(false);
                        projectInstrumentRepository.save(existing);
                    }
                }
            }

            instrument.setInstrumentName(updateInstrumentDTO.getInstrumentName());
            instrument.setInstrumentModel(updateInstrumentDTO.getInstrumentModel());
            instrument.setInstrumentNumber(updateInstrumentDTO.getInstrumentNumber());
            instrument.setGlobalInstrumentId(updateInstrumentDTO.getGlobalInstrumentId());
            instrument.setIsDefault(updateInstrumentDTO.getIsDefault() != null ? updateInstrumentDTO.getIsDefault() : false);
            instrument.setExperimentTypeCode(experimentTypeCode);
            instrument.setUpdatedAt(LocalDateTime.now());

            ProjectInstrument updatedInstrument = projectInstrumentRepository.save(instrument);
            ProjectInstrumentDTOs.InstrumentList response = convertToInstrumentListDTO(updatedInstrument);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error updating instrument", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新仪器设备失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/project-instruments/{id}")
    public ResponseEntity<?> deleteInstrument(
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

            ProjectInstrument instrument = projectInstrumentRepository.findById(id)
                    .orElse(null);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            // 管理员可操作任何项目；普通用户/子账号只能操作归属 effectiveUserId 的项目；子账号仅能操作进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(instrument.getProjectId()).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(instrument.getProjectId(), effectiveUserId).orElse(null);
            }
            if (project == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "无权访问此项目");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            projectInstrumentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting instrument", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除仪器设备失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/projects/{projectId}/instruments/default/{experimentTypeCode}")
    public ResponseEntity<ProjectInstrumentDTOs.InstrumentList> getDefaultInstrument(
            @PathVariable Integer projectId,
            @PathVariable String experimentTypeCode,
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

            // 查找指定检测类型的默认设备
            List<ProjectInstrument> defaultInstruments = projectInstrumentRepository
                    .findByProjectIdAndExperimentTypeCodeAndIsDefault(projectId, experimentTypeCode, true);

            if (defaultInstruments.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 返回第一个默认设备（理论上应该只有一个）
            ProjectInstrumentDTOs.InstrumentList response = convertToInstrumentListDTO(defaultInstruments.get(0));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error getting default instrument", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ProjectInstrumentDTOs.InstrumentList convertToInstrumentListDTO(ProjectInstrument instrument) {
        ProjectInstrumentDTOs.InstrumentList dto = new ProjectInstrumentDTOs.InstrumentList();
        dto.setId(instrument.getId());
        dto.setProjectId(instrument.getProjectId());
        dto.setInstrumentName(instrument.getInstrumentName());
        dto.setInstrumentModel(instrument.getInstrumentModel());
        dto.setInstrumentNumber(instrument.getInstrumentNumber());
        dto.setGlobalInstrumentId(instrument.getGlobalInstrumentId());
        dto.setIsDefault(instrument.getIsDefault());
        dto.setExperimentTypeCode(instrument.getExperimentTypeCode());
        dto.setCreatedAt(instrument.getCreatedAt());
        dto.setUpdatedAt(instrument.getUpdatedAt());
        return dto;
    }

    private String resolveExperimentTypeCode(String dtoCode, Integer globalInstrumentId) {
        if (dtoCode != null) {
            String trimmed = dtoCode.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        if (globalInstrumentId == null) {
            return null;
        }
        return InstrumentTypeMappingService.getExperimentTypeCodeByInstrumentId(globalInstrumentId);
    }
}
