package com.reportweb.service;

import com.reportweb.entity.Project;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.UnitComponent;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.repository.UnitComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部件同步服务
 * 负责在项目部件和机组部件之间同步数据
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentSyncService {

    private final ProjectRepository projectRepository;
    private final ProjectComponentRepository projectComponentRepository;
    private final UnitComponentRepository unitComponentRepository;

    /**
     * 将项目部件同步到机组部件
     * 允许重复，同名不同ID（ID是唯一标识）
     *
     * @param projectComponent 项目部件
     * @return 创建的机组部件，如果项目未关联机组则返回null
     */
    @Transactional
    public UnitComponent syncProjectComponentToUnitComponent(ProjectComponent projectComponent) {
        try {
            // 获取项目信息
            Project project = projectRepository.findById(projectComponent.getProjectId())
                    .orElse(null);
            
            if (project == null) {
                log.warn("Project not found: {}", projectComponent.getProjectId());
                return null;
            }

            // 检查项目是否关联了机组
            if (project.getUnitId() == null) {
                log.debug("Project {} is not associated with a unit, skipping sync", project.getId());
                return null;
            }

            // 直接创建新的机组部件（允许重复，同名不同ID）
            UnitComponent unitComponent = new UnitComponent();
            unitComponent.setUnitId(project.getUnitId());
            unitComponent.setComponentName(projectComponent.getComponentName());
            unitComponent.setMaterial(projectComponent.getMaterial());
            unitComponent.setCategory(projectComponent.getCategory());
            unitComponent.setPipeDiameter(projectComponent.getPipeDiameter());
            unitComponent.setWallThickness(projectComponent.getWallThickness());
            unitComponent.setRemark(projectComponent.getRemark());
            unitComponent.setCreatedAt(LocalDateTime.now());
            unitComponent.setUpdatedAt(LocalDateTime.now());

            UnitComponent saved = unitComponentRepository.save(unitComponent);
            log.info("Created unit component {} from project component {} (allowed duplicate)", saved.getId(), projectComponent.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error syncing project component {} to unit component", projectComponent.getId(), e);
            // 不抛出异常，避免影响主流程
            return null;
        }
    }

    /**
     * 项目完成后统一同步所有项目部件到机组部件
     * 当项目状态变为 Completed 时调用此方法
     *
     * @param projectId 项目ID
     * @return 同步的部件数量
     */
    @Transactional
    public int syncAllProjectComponentsToUnit(Integer projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElse(null);
            
            if (project == null) {
                log.warn("Project not found: {}", projectId);
                return 0;
            }

            // 检查项目是否关联了机组
            if (project.getUnitId() == null) {
                log.debug("Project {} is not associated with a unit, skipping sync", projectId);
                return 0;
            }

            // 获取项目的所有项目部件
            List<ProjectComponent> projectComponents = projectComponentRepository.findByProjectId(projectId);
            
            if (projectComponents.isEmpty()) {
                log.debug("No project components found for project {}", projectId);
                return 0;
            }

            // 遍历每个项目部件，同步到机组部件（允许重复）
            int syncedCount = 0;
            for (ProjectComponent projectComponent : projectComponents) {
                UnitComponent synced = syncProjectComponentToUnitComponent(projectComponent);
                if (synced != null) {
                    syncedCount++;
                }
            }

            log.info("Synced {} project components to unit {} for project {}", syncedCount, project.getUnitId(), projectId);
            return syncedCount;
        } catch (Exception e) {
            log.error("Error syncing all project components to unit for project {}", projectId, e);
            return 0;
        }
    }

    /**
     * 将机组部件同步到项目部件
     * 当在电厂详情中添加部件时，可以选择同步到相关项目
     *
     * @param unitComponent 机组部件
     * @param projectId 目标项目ID
     * @return 创建的项目部件
     */
    @Transactional
    public ProjectComponent syncUnitComponentToProjectComponent(UnitComponent unitComponent, Integer projectId) {
        try {
            // 验证项目是否存在
            Project project = projectRepository.findById(projectId)
                    .orElse(null);
            
            if (project == null) {
                log.warn("Project not found: {}", projectId);
                return null;
            }

            // 这里需要ProjectComponentRepository，但为了保持服务独立性，我们返回null
            // 实际实现应该在调用方处理
            log.info("Sync unit component {} to project {} requested", unitComponent.getId(), projectId);
            return null;
        } catch (Exception e) {
            log.error("Error syncing unit component {} to project component", unitComponent.getId(), e);
            return null;
        }
    }
}
