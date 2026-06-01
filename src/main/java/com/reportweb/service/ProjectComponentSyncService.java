package com.reportweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.util.TableDataMergeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 合金分析（AAT）多部件：将检测内容行同步到项目部件表并回写 {@code projectComponentIds}。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectComponentSyncService {

    private final ProjectComponentRepository projectComponentRepository;
    private final ReportComponentMergeHelper reportComponentMergeHelper;
    private final ComponentDetailFromDetectionResolver detailResolver;
    private final ObjectMapper objectMapper;

    /**
     * 多行检测内容或多块 perContentRow 时，按行 upsert 部件并更新报告关联 ID。
     *
     * @return 是否写入了新的 component ID 列表
     */
    @Transactional
    public boolean syncFromAatReportIfNeeded(Report report) {
        if (report == null || report.getProjectId() == null) {
            return false;
        }
        int rowCount = countSyncRows(report);
        if (rowCount <= 1) {
            return false;
        }

        JsonNode dcRows = detailResolver.detectionContentRowsNode(report);
        int target = Math.max(rowCount, dcRows.size());
        List<Integer> existingIds = reportComponentMergeHelper.resolveComponentIds(report);
        List<ProjectComponent> existingComps = reportComponentMergeHelper.loadOrdered(
                projectComponentRepository, existingIds);
        List<ProjectComponent> projectComponents = new ArrayList<>(
                projectComponentRepository.findByProjectId(report.getProjectId()));

        List<Integer> newIds = new ArrayList<>();
        boolean changed = false;

        for (int i = 0; i < target; i++) {
            JsonNode row = detailResolver.rowAt(dcRows, i);
            Integer preferredId = i < existingIds.size() ? existingIds.get(i) : null;
            ProjectComponent existingAtIndex = i < existingComps.size() ? existingComps.get(i) : null;

            if (!detailResolver.rowHasSyncableIdentity(row, report, existingComps, i)
                    && preferredId == null
                    && existingAtIndex == null) {
                continue;
            }

            ProjectComponent resolved = resolveOrCreateComponent(
                    report, row, existingComps, projectComponents, i, preferredId, existingAtIndex);
            if (resolved == null) {
                continue;
            }
            if (preferredId == null || !preferredId.equals(resolved.getId())) {
                changed = true;
            }
            newIds.add(resolved.getId());
        }

        if (newIds.isEmpty()) {
            return false;
        }

        if (!newIds.equals(existingIds)) {
            changed = true;
        }

        if (changed) {
            report.setProjectComponentId(newIds.get(0));
            if (newIds.size() > 1) {
                report.setProjectComponentIds(new ArrayList<>(newIds));
            } else {
                report.setProjectComponentIds(null);
            }
            log.info("AAT 报告 {} 同步项目部件: {}", report.getId(), newIds);
        }
        return changed;
    }

    private int countSyncRows(Report report) {
        JsonNode dcRows = detailResolver.detectionContentRowsNode(report);
        int dcCount = dcRows.size();
        int blockCount = 0;
        if (report.getReportItems() != null && !report.getReportItems().isEmpty()) {
            ReportItem item = report.getReportItems().get(0);
            if (item.getTableData() != null && !item.getTableData().isBlank()) {
                blockCount = TableDataMergeUtil.perContentRowBlocks(item.getTableData(), objectMapper).size();
            }
        }
        return Math.max(dcCount, blockCount);
    }

    private ProjectComponent resolveOrCreateComponent(
            Report report,
            JsonNode row,
            List<ProjectComponent> comps,
            List<ProjectComponent> projectPool,
            int rowIndex,
            Integer preferredId,
            ProjectComponent existingAtIndex) {

        String name = detailResolver.resolveName(row, report, comps, rowIndex);
        String material = detailResolver.resolveMaterial(row, report, comps, rowIndex);
        String specDisplay = detailResolver.resolveSpecDisplay(row, report, comps, rowIndex);

        if (name == null && material == null) {
            return null;
        }

        if (preferredId != null) {
            Optional<ProjectComponent> byId = projectComponentRepository.findById(preferredId);
            if (byId.isPresent() && Objects.equals(byId.get().getProjectId(), report.getProjectId())) {
                return updateComponentIfNeeded(byId.get(), name, material);
            }
        }

        if (existingAtIndex != null) {
            return updateComponentIfNeeded(existingAtIndex, name, material);
        }

        Optional<ProjectComponent> matched = findMatching(projectPool, name, material, specDisplay);
        if (matched.isPresent()) {
            return updateComponentIfNeeded(matched.get(), name, material);
        }

        if (name == null) {
            return null;
        }

        ProjectComponent created = new ProjectComponent();
        created.setProjectId(report.getProjectId());
        created.setComponentName(name);
        created.setMaterial(material);
        ProjectComponent saved = projectComponentRepository.save(created);
        projectPool.add(saved);
        log.debug("AAT 新建项目部件 id={} name={} material={}", saved.getId(), name, material);
        return saved;
    }

    private ProjectComponent updateComponentIfNeeded(ProjectComponent c, String name, String material) {
        boolean dirty = false;
        if (name != null && (c.getComponentName() == null || c.getComponentName().isBlank())) {
            c.setComponentName(name);
            dirty = true;
        }
        if (material != null && (c.getMaterial() == null || c.getMaterial().isBlank())) {
            c.setMaterial(material);
            dirty = true;
        }
        if (dirty) {
            return projectComponentRepository.save(c);
        }
        return c;
    }

    private Optional<ProjectComponent> findMatching(
            List<ProjectComponent> pool, String name, String material, String specDisplay) {
        if (name == null) {
            return Optional.empty();
        }
        for (ProjectComponent c : pool) {
            if (!name.equals(c.getComponentName())) {
                continue;
            }
            if (material != null && c.getMaterial() != null && !c.getMaterial().isBlank()
                    && !material.equals(c.getMaterial())) {
                continue;
            }
            if (specDisplay != null && !specDisplay.isEmpty()) {
                String unified = reportComponentMergeHelper.formatSpecUnified(c);
                if (!specDisplay.equals(unified)) {
                    continue;
                }
            }
            return Optional.of(c);
        }
        return Optional.empty();
    }
}
