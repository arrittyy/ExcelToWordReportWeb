package com.reportweb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.dto.ExportTextOverridesPatchDTO;
import com.reportweb.dto.ExportTextPreviewDTO;
import com.reportweb.dto.ReportDTOs;
import com.reportweb.dto.ImageAttachmentDTO;
import com.reportweb.entity.Report;
import com.reportweb.entity.ReportItem;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Project;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.ProjectInstrument;
import com.reportweb.entity.ImageAttachment;
import com.reportweb.repository.ReportRepository;
import com.reportweb.repository.ReportItemRepository;
import com.reportweb.repository.ExperimentTypeRepository;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.repository.ProjectInstrumentRepository;
import com.reportweb.repository.ImageAttachmentRepository;
import com.reportweb.security.CustomUserPrincipal;
import com.reportweb.security.UserRoleUtils;
import com.reportweb.service.WordGeneratorService;
import com.reportweb.service.DefectDetectionService;
import com.reportweb.service.DataComparisonService;
import com.reportweb.service.LeebHardnessCategoryResolver;
import com.reportweb.service.DetectionContentAutoFillService;
import com.reportweb.service.DetectionContentNarrativeService;
import com.reportweb.service.ReportComponentMergeHelper;
import com.reportweb.service.ProjectComponentSyncService;
import com.reportweb.service.ReportChangeLogService;
import com.reportweb.util.ExportTextOverrides;
import com.reportweb.util.JsonScalarStringNormalizer;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private static final int BATCH_MERGE_WORD_MAX_IDS = 50;

    private final ReportRepository reportRepository;
    private final ReportItemRepository reportItemRepository;
    private final ExperimentTypeRepository experimentTypeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectComponentRepository projectComponentRepository;
    private final ProjectInstrumentRepository projectInstrumentRepository;
    private final ImageAttachmentRepository imageAttachmentRepository;
    private final WordGeneratorService wordGeneratorService;
    private final DefectDetectionService defectDetectionService;
    private final DetectionContentAutoFillService detectionContentAutoFillService;
    private final ReportComponentMergeHelper reportComponentMergeHelper;
    private final ProjectComponentSyncService projectComponentSyncService;
    private final DetectionContentNarrativeService detectionContentNarrativeService;
    private final ReportChangeLogService reportChangeLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    @Transactional
    public ResponseEntity<List<ReportDTOs.ReportList>> getReports(
            @RequestParam(required = false) Integer projectId,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                log.error("Authentication is null or principal is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            // 管理员可查看所有报告；主账号看本人项目下全部报告（含子账号创建）；子账号按 projectId 时需校验父项目归属且进行中
            List<Report> reports;
            if (isAdmin) {
                if (projectId != null) {
                    reports = reportRepository.findByProjectIdOrderById(projectId);
                } else {
                    reports = reportRepository.findAllOrderById();
                }
            } else {
                if (projectId != null) {
                    if (isSubUser && currentUser.getParentUserId() != null) {
                        Project project = projectRepository.findByIdAndUserId(projectId, currentUser.getParentUserId()).orElse(null);
                        if (project == null || !"InProgress".equals(project.getStatus())) {
                            reports = new ArrayList<>();
                        } else {
                            reports = reportRepository.findByProjectIdOrderById(projectId);
                        }
                    } else if (!isSubUser) {
                        if (projectRepository.findByIdAndUserId(projectId, userId).isEmpty()) {
                            reports = new ArrayList<>();
                        } else {
                            reports = reportRepository.findByProjectIdOrderById(projectId);
                        }
                    } else {
                        reports = reportRepository.findByUserIdAndProjectIdOrderById(userId, projectId);
                    }
                } else {
                    if (!isSubUser) {
                        reports = reportRepository.findByProjectOwnerUserIdOrderById(userId);
                    } else {
                        reports = reportRepository.findByUserIdOrderById(effectiveUserId);
                    }
                }
            }

            // 触发懒加载，避免LazyInitializationException
            for (Report report : reports) {
                if (report.getReportItems() != null) {
                    report.getReportItems().size();
                }
                if (report.getImageAttachments() != null) {
                    report.getImageAttachments().size();
                }
                // 触发user和project的懒加载
                if (report.getUser() != null) {
                    report.getUser().getFullName();
                }
                if (report.getProject() != null) {
                    report.getProject().getProjectNumber();
                }
            }

            Map<Integer, ExperimentType> experimentTypeMap = new HashMap<>();
            for (Report report : reports) {
                if (report.getExperimentTypeId() != null) {
                    experimentTypeMap.putIfAbsent(report.getExperimentTypeId(), null);
                }
            }
            List<Integer> experimentTypeIds = new ArrayList<>(experimentTypeMap.keySet());
            if (!experimentTypeIds.isEmpty()) {
                for (ExperimentType et : experimentTypeRepository.findAllById(experimentTypeIds)) {
                    experimentTypeMap.put(et.getId(), et);
                }
            }

            // 按报告编号升序排序，与概述顺序一致（总日志组内顺序由前端按检测类型默认序合并）
            List<ReportDTOs.ReportList> reportList = reports.stream()
                    .sorted(Comparator.comparing(Report::getReportNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(r -> convertToReportListDTO(r,
                            r.getExperimentTypeId() != null ? experimentTypeMap.get(r.getExperimentTypeId()) : null))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reportList);
        } catch (Exception ex) {
            log.error("Error getting reports", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取所有报告的摘要集合
     * 用于数据导出功能
     */
    @GetMapping("/summaries")
    @Transactional
    public ResponseEntity<List<ReportDTOs.ReportSummaryDTO>> getReportSummaries(
            @RequestParam(required = false) Integer projectId,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                log.error("Authentication is null or principal is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            // 管理员可查看所有报告；主账号看本人项目下全部报告（含子账号创建）；子账号按 projectId 时返回父进行中项目下全部报告
            List<Report> reports;
            if (isAdmin) {
                if (projectId != null) {
                    reports = reportRepository.findByProjectIdWithRelations(projectId);
                } else {
                    reports = reportRepository.findAllWithRelations();
                }
            } else {
                if (projectId != null) {
                    if (isSubUser && currentUser.getParentUserId() != null) {
                        Project project = projectRepository.findByIdAndUserId(projectId, currentUser.getParentUserId()).orElse(null);
                        if (project == null || !"InProgress".equals(project.getStatus())) {
                            reports = new ArrayList<>();
                        } else {
                            reports = reportRepository.findByProjectIdWithRelations(projectId);
                        }
                    } else if (!isSubUser) {
                        if (projectRepository.findByIdAndUserId(projectId, userId).isEmpty()) {
                            reports = new ArrayList<>();
                        } else {
                            reports = reportRepository.findByProjectIdWithRelations(projectId);
                        }
                    } else {
                        reports = reportRepository.findByProjectIdWithRelations(projectId);
                        reports = reports.stream()
                                .filter(r -> userId.equals(r.getUserId()))
                                .collect(Collectors.toList());
                    }
                } else {
                    if (!isSubUser) {
                        reports = reportRepository.findByProjectOwnerUserIdWithRelations(userId);
                    } else {
                        reports = reportRepository.findByUserIdWithRelations(effectiveUserId);
                    }
                }
            }

            // 批量加载ExperimentType和ProjectComponent，避免N+1问题
            Map<Integer, ExperimentType> experimentTypeMap = new HashMap<>();
            Map<Integer, ProjectComponent> componentMap = new HashMap<>();
            
            for (Report report : reports) {
                // 收集所有需要的experimentTypeId
                if (report.getExperimentTypeId() != null) {
                    experimentTypeMap.putIfAbsent(report.getExperimentTypeId(), null);
                }
                // 收集所有需要的 projectComponentId（含多选）
                for (Integer cid : reportComponentMergeHelper.resolveComponentIds(report)) {
                    componentMap.putIfAbsent(cid, null);
                }
            }
            
            // 批量查询ExperimentType
            List<Integer> experimentTypeIds = new ArrayList<>(experimentTypeMap.keySet());
            if (!experimentTypeIds.isEmpty()) {
                List<ExperimentType> experimentTypes = experimentTypeRepository.findAllById(experimentTypeIds);
                for (ExperimentType et : experimentTypes) {
                    experimentTypeMap.put(et.getId(), et);
                }
            }
            
            // 批量查询ProjectComponent
            List<Integer> componentIds = new ArrayList<>(componentMap.keySet());
            if (!componentIds.isEmpty()) {
                List<ProjectComponent> components = projectComponentRepository.findAllById(componentIds);
                for (ProjectComponent comp : components) {
                    componentMap.put(comp.getId(), comp);
                }
            }

            // 转换DTO
            List<ReportDTOs.ReportSummaryDTO> summaries = reports.stream()
                    .map(report -> {
                        ExperimentType experimentType = report.getExperimentTypeId() != null 
                                ? experimentTypeMap.get(report.getExperimentTypeId()) 
                                : null;
                        ProjectComponent component = report.getProjectComponentId() != null 
                                ? componentMap.get(report.getProjectComponentId()) 
                                : null;
                        return convertToReportSummaryDTO(report, experimentType, component);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(summaries);
        } catch (Exception ex) {
            log.error("Error getting report summaries", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "获取报告摘要失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<ReportDTOs.ReportDetail> getReport(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);

            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            // 触发懒加载，确保关联数据已加载
            if (report.getReportItems() != null) {
                report.getReportItems().size();
                for (ReportItem item : report.getReportItems()) {
                    if (item.getExperimentType() != null) {
                        item.getExperimentType().getName();
                    }
                }
            }
            if (report.getImageAttachments() != null) {
                report.getImageAttachments().size();
            }
            if (report.getUser() != null) {
                report.getUser().getFullName();
            }
            if (report.getProject() != null) {
                report.getProject().getProjectNumber();
            }

            ReportDTOs.ReportDetail reportDetail = convertToReportDetailDTO(report);
            return ResponseEntity.ok(reportDetail);
        } catch (Exception ex) {
            log.error("Error getting report with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<?> getReportItems(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            List<ReportItem> reportItems = reportItemRepository.findByReportId(id);

            if (!reportItems.isEmpty() && reportItems.get(0).getTableData() != null) {
                try {
                    Object detectionData = objectMapper.readValue(
                            reportItems.get(0).getTableData(),
                            Object.class
                    );
                    return ResponseEntity.ok(detectionData);
                } catch (Exception e) {
                    log.warn("Failed to parse detection data for report {}: {}", id, e.getMessage());
                    return ResponseEntity.ok(null);
                }
            }

            return ResponseEntity.ok(null);
        } catch (Exception ex) {
            log.error("Error getting report items for report id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/export-text-preview")
    @Transactional
    public ResponseEntity<ExportTextPreviewDTO> getExportTextPreview(
            @PathVariable Integer id,
            @RequestParam(name = "contentRowIndex", required = false) Integer contentRowIndex,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }
            if (report.getReportItems() != null) {
                report.getReportItems().size();
                for (ReportItem item : report.getReportItems()) {
                    if (item.getExperimentType() != null) {
                        item.getExperimentType().getCode();
                        item.getExperimentType().getName();
                    }
                }
            }
            if (report.getImageAttachments() != null) {
                report.getImageAttachments().size();
            }
            if (report.getProject() != null) {
                report.getProject().getProjectNumber();
                if (report.getProject().getReports() != null) {
                    report.getProject().getReports().size();
                }
            }
            int rowIdx = contentRowIndex != null ? Math.max(0, contentRowIndex) : 0;
            return ResponseEntity.ok(wordGeneratorService.buildExportTextPreview(report, rowIdx, report.getProject()));
        } catch (Exception ex) {
            log.error("Error export-text-preview report {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/export-text-overrides")
    @Transactional
    public ResponseEntity<Void> putExportTextOverrides(
            @PathVariable Integer id,
            @RequestBody(required = false) ExportTextOverridesPatchDTO patch,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, String> m = new LinkedHashMap<>();
            if (patch != null) {
                if (patch.getDetectionNarrativeBody() != null) {
                    m.put(ExportTextOverrides.DETECTION_NARRATIVE_BODY,
                            detectionContentNarrativeService.normalizeExportDetectionNarrativeBody(
                                    patch.getDetectionNarrativeBody()));
                }
                if (patch.getConclusionParagraph() != null) {
                    m.put(ExportTextOverrides.CONCLUSION_PARAGRAPH, patch.getConclusionParagraph());
                }
                if (patch.getOverviewWorkContentLine() != null) {
                    m.put(ExportTextOverrides.OVERVIEW_WORK_CONTENT_LINE,
                            DetectionContentNarrativeService.stripFigureReferencePhrases(
                                    patch.getOverviewWorkContentLine()));
                }
                if (patch.getOverviewDefectLine() != null) {
                    m.put(ExportTextOverrides.OVERVIEW_DEFECT_LINE, patch.getOverviewDefectLine());
                }
            }
            Integer contentRowIndex = patch != null ? patch.getContentRowIndex() : null;
            report.setCustomFields(ExportTextOverrides.mergeIntoCustomFields(report.getCustomFields(), m, contentRowIndex));
            report.setUpdatedAt(LocalDateTime.now());
            reportRepository.save(report);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error export-text-overrides report {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestBody ReportDTOs.CreateReport createReportDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUser().getId();

            log.info("开始创建报告 - 项目ID: {}, 检测类型ID: {}", 
                    createReportDTO.getProjectId(), createReportDTO.getExperimentTypeId());

            // 生成报告编号：项目编号 + "-001"（依次递增）
            Project project = projectRepository.findById(createReportDTO.getProjectId())
                    .orElseThrow(() -> new RuntimeException("项目不存在"));

            String projectNumber = project.getProjectNumber();
            if (projectNumber == null || projectNumber.trim().isEmpty()) {
                throw new RuntimeException("项目编号为空，无法生成报告编号");
            }

            // 查询同一项目下所有报告，提取序号
            List<Report> existingReports = reportRepository.findByProjectIdOrderById(createReportDTO.getProjectId());
            int maxSequence = 0;
            String prefix = projectNumber + "-";

            for (Report existingReport : existingReports) {
                String existingReportNumber = existingReport.getReportNumber();
                if (existingReportNumber != null && existingReportNumber.startsWith(prefix)) {
                    try {
                        String sequencePart = existingReportNumber.substring(prefix.length());
                        int sequence = Integer.parseInt(sequencePart);
                        if (sequence > maxSequence) {
                            maxSequence = sequence;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无法解析报告编号中的序号: {}", existingReportNumber);
                    }
                }
            }

            // 生成新报告编号
            int newSequence = maxSequence + 1;
            String reportNumber = String.format("%s-%03d", projectNumber, newSequence);

            // 确保报告编号唯一性
            int retryCount = 0;
            while (reportRepository.existsByReportNumber(reportNumber) && retryCount < 100) {
                newSequence++;
                reportNumber = String.format("%s-%03d", projectNumber, newSequence);
                retryCount++;
            }

            if (retryCount >= 100) {
                throw new RuntimeException("无法生成唯一的报告编号");
            }

            // 创建报告实体
            Report report = new Report();
            report.setProjectId(createReportDTO.getProjectId());
            report.setTitle(createReportDTO.getTitle());
            report.setReportNumber(reportNumber);
            report.setUserId(userId);
            report.setExperimentTypeId(createReportDTO.getExperimentTypeId());
            report.setInspector(createReportDTO.getInspector());

            // 自动设置检测类型名称
            ExperimentType expType = experimentTypeRepository.findById(createReportDTO.getExperimentTypeId())
                    .orElseThrow(() -> new RuntimeException("检测类型不存在"));
            report.setTestMethod(expType.getName());

            report.setEquipment(createReportDTO.getEquipment());
            report.setTestStandard(createReportDTO.getTestStandard());

            applyProjectComponentsFromDto(report, createReportDTO.getProjectId(),
                    createReportDTO.getProjectComponentIds(), createReportDTO.getProjectComponentId(),
                    createReportDTO.getComponentName(), createReportDTO.getComponentSpec());

            // 如果选择了仪器设备，自动填充仪器信息
            if (createReportDTO.getProjectInstrumentId() != null && createReportDTO.getProjectInstrumentId() > 0) {
                ProjectInstrument projectInstrument = projectInstrumentRepository
                        .findById(createReportDTO.getProjectInstrumentId())
                        .orElse(null);
                if (projectInstrument != null) {
                    report.setProjectInstrumentId(createReportDTO.getProjectInstrumentId());
                    report.setInstrumentModel(projectInstrument.getInstrumentModel());
                    report.setInstrumentNumber(projectInstrument.getInstrumentNumber());
                } else {
                    report.setProjectInstrumentId(null);
                    report.setInstrumentModel(createReportDTO.getInstrumentModel());
                    report.setInstrumentNumber(createReportDTO.getInstrumentNumber());
                }
            } else {
                report.setProjectInstrumentId(null);
                report.setInstrumentModel(createReportDTO.getInstrumentModel());
                report.setInstrumentNumber(createReportDTO.getInstrumentNumber());
            }

            report.setEquipmentCategory(createReportDTO.getEquipmentCategory());
            report.setEquipmentName(createReportDTO.getEquipmentName());
            report.setTestDate(createReportDTO.getTestDate());
            report.setLocation(createReportDTO.getLocation());
            report.setReportImage(createReportDTO.getReportImage());
            // hasDefect将在保存reportItems后自动判断
            report.setCustomFields(JsonScalarStringNormalizer.normalizeCustomFieldsMap(createReportDTO.getCustomFields()));
            report.setDetectionContent(createReportDTO.getDetectionContent());
            report.setStatus("Draft");
            report.setCreatedAt(LocalDateTime.now());
            report.setUpdatedAt(LocalDateTime.now());

            Report savedReport = reportRepository.save(report);
            log.info("报告保存成功 - ID: {}, 报告编号: {}", savedReport.getId(), savedReport.getReportNumber());

            // 添加报告项
            if (createReportDTO.getReportItems() != null) {
                for (ReportDTOs.CreateReportItemDTO itemDTO : createReportDTO.getReportItems()) {
                    ReportItem reportItem = new ReportItem();
                    reportItem.setReportId(savedReport.getId());
                    reportItem.setExperimentTypeId(itemDTO.getExperimentTypeId());
                    reportItem.setTableData(JsonScalarStringNormalizer.normalizeTableDataJson(itemDTO.getTableData(), objectMapper));
                    reportItem.setSummary(itemDTO.getSummary());
                    reportItem.setCreatedAt(LocalDateTime.now());
                    reportItemRepository.save(reportItem);
                }
                // 刷新EntityManager确保数据持久化
                reportItemRepository.flush();
            }

            // 显式查询 reportItems
            List<ReportItem> reportItems = reportItemRepository.findByReportId(savedReport.getId());
            savedReport.setReportItems(reportItems);

            ExperimentType experimentType = experimentTypeRepository.findById(savedReport.getExperimentTypeId()).orElse(null);

            // 自动填充 detectionContent（位置编号、总计等）
            if (experimentType != null && reportItems != null && !reportItems.isEmpty()
                    && detectionContentAutoFillService.isAutoFillType(experimentType.getCode())) {
                String tableData = reportItems.get(0).getTableData();
                Map<String, Object> newDetectionContent = detectionContentAutoFillService.generateFromTableData(
                        experimentType.getCode(),
                        experimentType.getName(),
                        tableData,
                        savedReport.getDetectionContent());
                if (newDetectionContent != null) {
                    savedReport.setDetectionContent(newDetectionContent);
                    reportRepository.save(savedReport);
                }
            }

            // 合金分析多部件：同步项目部件表
            syncAatProjectComponentsIfNeeded(savedReport, experimentType);

            List<ProjectComponent> defectComponents = reportComponentMergeHelper.loadOrdered(
                    projectComponentRepository, reportComponentMergeHelper.resolveComponentIds(savedReport));

            String autoDetectedHasDefect = defectDetectionService.hasDefectForComponents(
                    savedReport, experimentType, defectComponents);

            if (autoDetectedHasDefect != null) {
                savedReport.setHasDefect(autoDetectedHasDefect);
                log.info("自动判断缺陷结果 - 报告ID: {}, 结果: {}", savedReport.getId(), autoDetectedHasDefect);
            } else {
                savedReport.setHasDefect(createReportDTO.getHasDefect());
                log.info("使用用户输入的缺陷判断 - 报告ID: {}, 结果: {}", savedReport.getId(), createReportDTO.getHasDefect());
            }

            reportRepository.save(savedReport);
            reportChangeLogService.recordCreated(
                    savedReport, userPrincipal.getUser(), ReportChangeLogService.SOURCE_USER_SAVE);

            // 保存附图
            if (createReportDTO.getImageAttachments() != null && !createReportDTO.getImageAttachments().isEmpty()) {
                for (int i = 0; i < createReportDTO.getImageAttachments().size(); i++) {
                    ImageAttachmentDTO attachmentDTO = createReportDTO.getImageAttachments().get(i);
                    ImageAttachment attachment = new ImageAttachment();
                    attachment.setReportId(savedReport.getId());
                    attachment.setImageUrls(objectMapper.writeValueAsString(attachmentDTO.getImageUrls()));
                    attachment.setDescription(attachmentDTO.getDescription());
                    attachment.setDisplayOrder(i);
                    imageAttachmentRepository.save(attachment);
                }
            }

            // 触发懒加载，确保关联数据已加载
            if (savedReport.getReportItems() != null) {
                savedReport.getReportItems().size();
            }
            if (savedReport.getImageAttachments() != null) {
                savedReport.getImageAttachments().size();
            }
            if (savedReport.getUser() != null) {
                savedReport.getUser().getFullName();
            }
            if (savedReport.getProject() != null) {
                savedReport.getProject().getProjectNumber();
            }

            ReportDTOs.ReportDetail response = convertToReportDetailDTO(savedReport);
            log.info("报告创建完成 - ID: {}, 报告编号: {}", savedReport.getId(), savedReport.getReportNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            log.warn("创建报告参数错误: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Error creating report", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建报告失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateReport(
            @PathVariable Integer id,
            @Valid @RequestBody ReportDTOs.UpdateReport updateReportDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUser().getId();

            log.info("开始更新报告 - ID: {}, 用户: {}", id, userId);

            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);

            if (report == null) {
                log.error("报告未找到或用户无权限访问 reportId={}, userId={}", id, userId);
                return ResponseEntity.notFound().build();
            }

            ReportChangeLogService.ReportMetadataSnapshot metadataBefore =
                    reportChangeLogService.captureMetadata(report);

            // 更新基本信息
            report.setProjectId(updateReportDTO.getProjectId());
            report.setTitle(updateReportDTO.getTitle());
            report.setInspector(updateReportDTO.getInspector());

            // 如果更新了检测类型，自动更新检测类型名称
            if (updateReportDTO.getExperimentTypeId() != null) {
                report.setExperimentTypeId(updateReportDTO.getExperimentTypeId());
                ExperimentType expType = experimentTypeRepository.findById(updateReportDTO.getExperimentTypeId())
                        .orElseThrow(() -> new RuntimeException("检测类型不存在"));
                report.setTestMethod(expType.getName());
            }

            report.setEquipment(updateReportDTO.getEquipment());
            report.setTestStandard(updateReportDTO.getTestStandard());

            applyProjectComponentsFromDto(report, updateReportDTO.getProjectId(),
                    updateReportDTO.getProjectComponentIds(), updateReportDTO.getProjectComponentId(),
                    updateReportDTO.getComponentName(), updateReportDTO.getComponentSpec());

            // 更新仪器设备关联
            if (updateReportDTO.getProjectInstrumentId() != null && updateReportDTO.getProjectInstrumentId() > 0) {
                ProjectInstrument projectInstrument = projectInstrumentRepository
                        .findById(updateReportDTO.getProjectInstrumentId())
                        .orElse(null);
                if (projectInstrument != null) {
                    report.setProjectInstrumentId(updateReportDTO.getProjectInstrumentId());
                    report.setInstrumentModel(projectInstrument.getInstrumentModel());
                    report.setInstrumentNumber(projectInstrument.getInstrumentNumber());
                } else {
                    report.setProjectInstrumentId(null);
                    report.setInstrumentModel(updateReportDTO.getInstrumentModel());
                    report.setInstrumentNumber(updateReportDTO.getInstrumentNumber());
                }
            } else {
                report.setProjectInstrumentId(null);
                report.setInstrumentModel(updateReportDTO.getInstrumentModel());
                report.setInstrumentNumber(updateReportDTO.getInstrumentNumber());
            }

            report.setEquipmentCategory(updateReportDTO.getEquipmentCategory());
            report.setEquipmentName(updateReportDTO.getEquipmentName());
            report.setTestDate(updateReportDTO.getTestDate());
            report.setLocation(updateReportDTO.getLocation());
            report.setReportImage(updateReportDTO.getReportImage());
            // hasDefect将在更新reportItems后自动判断
            report.setCustomFields(JsonScalarStringNormalizer.normalizeCustomFieldsMap(
                    ExportTextOverrides.mergeReplacePreservingExportOverrides(
                            report.getCustomFields(), updateReportDTO.getCustomFields())));
            report.setDetectionContent(updateReportDTO.getDetectionContent());
            report.setStatus(updateReportDTO.getStatus());
            report.setUpdatedAt(LocalDateTime.now());

            reportRepository.save(report);
            reportRepository.flush();
            log.info("报告基本信息更新完成 - ID: {}", id);

            // 更新报告项（先删除现有的，再添加新的）
            if (updateReportDTO.getReportItems() != null && !updateReportDTO.getReportItems().isEmpty()) {
                List<ReportItem> existingItems = reportItemRepository.findByReportId(id);
                for (ReportItem item : existingItems) {
                    reportItemRepository.delete(item);
                }

                for (ReportDTOs.CreateReportItemDTO itemDTO : updateReportDTO.getReportItems()) {
                    ReportItem reportItem = new ReportItem();
                    reportItem.setReportId(id);
                    reportItem.setExperimentTypeId(itemDTO.getExperimentTypeId());
                    reportItem.setTableData(JsonScalarStringNormalizer.normalizeTableDataJson(itemDTO.getTableData(), objectMapper));
                    reportItem.setSummary(itemDTO.getSummary());
                    reportItem.setCreatedAt(LocalDateTime.now());
                    reportItemRepository.save(reportItem);
                }
                // 刷新EntityManager确保数据持久化
                reportItemRepository.flush();
            }

            List<ReportItem> reportItems = reportItemRepository.findByReportId(id);
            report.setReportItems(reportItems);

            ExperimentType experimentType = experimentTypeRepository.findById(report.getExperimentTypeId()).orElse(null);

            if (experimentType != null && reportItems != null && !reportItems.isEmpty()
                    && detectionContentAutoFillService.isAutoFillType(experimentType.getCode())) {
                String tableData = reportItems.get(0).getTableData();
                Map<String, Object> newDetectionContent = detectionContentAutoFillService.generateFromTableData(
                        experimentType.getCode(),
                        experimentType.getName(),
                        tableData,
                        report.getDetectionContent());
                if (newDetectionContent != null) {
                    report.setDetectionContent(newDetectionContent);
                    reportRepository.save(report);
                }
            }

            syncAatProjectComponentsIfNeeded(report, experimentType);

            List<ProjectComponent> defectComponentsUpdate = reportComponentMergeHelper.loadOrdered(
                    projectComponentRepository, reportComponentMergeHelper.resolveComponentIds(report));

            String autoDetectedHasDefect = defectDetectionService.hasDefectForComponents(
                    report, experimentType, defectComponentsUpdate);

            if (autoDetectedHasDefect != null) {
                report.setHasDefect(autoDetectedHasDefect);
                log.info("自动判断缺陷结果 - 报告ID: {}, 结果: {}", id, autoDetectedHasDefect);
            } else {
                report.setHasDefect(updateReportDTO.getHasDefect());
                log.info("使用用户输入的缺陷判断 - 报告ID: {}, 结果: {}", id, updateReportDTO.getHasDefect());
            }

            reportRepository.save(report);

            // 更新附图（先删除现有的，再添加新的）
            if (updateReportDTO.getImageAttachments() != null) {
                imageAttachmentRepository.deleteByReportId(id);

                if (!updateReportDTO.getImageAttachments().isEmpty()) {
                    for (int i = 0; i < updateReportDTO.getImageAttachments().size(); i++) {
                        ImageAttachmentDTO attachmentDTO = updateReportDTO.getImageAttachments().get(i);
                        ImageAttachment attachment = new ImageAttachment();
                        attachment.setReportId(id);
                        attachment.setImageUrls(objectMapper.writeValueAsString(attachmentDTO.getImageUrls()));
                        attachment.setDescription(attachmentDTO.getDescription());
                        attachment.setDisplayOrder(i);
                        imageAttachmentRepository.save(attachment);
                    }
                }
            }

            reportChangeLogService.recordUpdatedFromSnapshot(
                    metadataBefore, report, currentUser, ReportChangeLogService.SOURCE_USER_SAVE);

            log.info("报告更新完成 - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            log.warn("更新报告部件校验失败: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("更新报告失败 ID: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新报告失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);

            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            reportChangeLogService.recordDeleted(report, currentUser, ReportChangeLogService.SOURCE_USER_SAVE);
            reportRepository.delete(report);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting report with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除报告失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/generate-word")
    public ResponseEntity<?> generateWord(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            Report report = findReportAccessibleToUser(currentUser, id, isAdmin);

            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            if (report.getProject() != null) {
                report.getProject().getProjectNumber();
                if (report.getProject().getReports() != null) {
                    report.getProject().getReports().size();
                    for (Report r : report.getProject().getReports()) {
                        if (r.getReportItems() != null) {
                            r.getReportItems().size();
                            for (ReportItem ri : r.getReportItems()) {
                                if (ri.getExperimentType() != null) {
                                    ri.getExperimentType().getName();
                                }
                            }
                        }
                        if (r.getImageAttachments() != null) {
                            r.getImageAttachments().size();
                        }
                    }
                }
            }

            byte[] wordBytes = wordGeneratorService.generateReportAsync(report);
            String fileName = String.format("%s_%s.docx",
                    report.getReportNumber(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String contentDisposition = String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                    fileName, encodedFileName);

            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(wordBytes);
        } catch (IllegalArgumentException ex) {
            log.warn("Validation failed generating word for report {}: {}", id, ex.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "无法生成Word文档");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Error generating word for report with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "生成Word文档失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/batch-generate-word-merged")
    @Transactional(readOnly = true)
    public ResponseEntity<?> batchGenerateWordMerged(
            @RequestBody List<Integer> ids,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);

            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "报告ID列表不能为空"));
            }

            LinkedHashSet<Integer> orderedUniqueSet = new LinkedHashSet<>();
            for (Integer id : ids) {
                if (id != null) {
                    orderedUniqueSet.add(id);
                }
            }
            List<Integer> orderedUnique = new ArrayList<>(orderedUniqueSet);
            if (orderedUnique.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "报告ID列表不能为空"));
            }
            if (orderedUnique.size() > BATCH_MERGE_WORD_MAX_IDS) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "单次最多合并 " + BATCH_MERGE_WORD_MAX_IDS + " 条报告"));
            }

            for (Integer id : orderedUnique) {
                if (findReportAccessibleToUser(currentUser, id, isAdmin) == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "无权访问报告ID: " + id));
                }
            }

            List<Report> loaded = reportRepository.findAllByIdWithRelations(orderedUnique);
            if (loaded.size() != orderedUnique.size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "存在无效的报告ID"));
            }
            Map<Integer, Report> byId = loaded.stream()
                    .collect(Collectors.toMap(Report::getId, r -> r));

            List<Report> ordered = new ArrayList<>(orderedUnique.size());
            for (Integer id : orderedUnique) {
                ordered.add(byId.get(id));
            }

            for (Report report : ordered) {
                if (report.getReportItems() != null) {
                    report.getReportItems().size();
                    for (ReportItem item : report.getReportItems()) {
                        if (item.getExperimentType() != null) {
                            item.getExperimentType().getName();
                        }
                    }
                }
                if (report.getImageAttachments() != null) {
                    report.getImageAttachments().size();
                }
                if (report.getProject() != null) {
                    report.getProject().getProjectNumber();
                }
            }

            byte[] wordBytes = wordGeneratorService.generateMergedFormalReportsWord(ordered);
            String fileName = String.format("合并单项报告_%s.docx",
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String contentDisposition = String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                    fileName, encodedFileName);

            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(wordBytes);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("batch generate word merged failed", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "合并生成Word失败"));
        }
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<?> batchDelete(
            @RequestBody List<Integer> ids,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);

            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "报告ID列表不能为空"));
            }

            List<Report> reports = reportRepository.findAllById(ids);
            for (Report report : reports) {
                if (!canUserMutateReport(currentUser, report, isAdmin)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "无权删除报告ID: " + report.getId()));
                }
            }

            reportChangeLogService.recordDeletedAll(reports, currentUser, ReportChangeLogService.SOURCE_BATCH_DELETE);

            // 批量删除
            reportRepository.deleteAllById(ids);

            log.info("批量删除报告成功: {}, 用户: {}", ids, userId);
            return ResponseEntity.ok()
                    .body(Map.of("message", "成功删除 " + ids.size() + " 个报告"));
        } catch (Exception ex) {
            log.error("批量删除报告失败", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "批量删除失败"));
        }
    }

    @PostMapping("/batch-update-status")
    public ResponseEntity<?> batchUpdateStatus(
            @RequestBody BatchUpdateStatusRequest request,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUser().getId();

            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "报告ID列表不能为空"));
            }

            if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "状态不能为空"));
            }

            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);

            List<Report> reports = reportRepository.findAllById(request.getIds());
            for (Report report : reports) {
                if (!canUserMutateReport(currentUser, report, isAdmin)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "无权修改报告ID: " + report.getId()));
                }
                ReportChangeLogService.ReportMetadataSnapshot before =
                        reportChangeLogService.captureMetadata(report);
                report.setStatus(request.getStatus());
                report.setUpdatedAt(LocalDateTime.now());
                reportChangeLogService.recordUpdatedFromSnapshot(
                        before, report, currentUser, ReportChangeLogService.SOURCE_BATCH_STATUS);
            }

            // 批量更新
            reportRepository.saveAll(reports);

            log.info("批量更新报告状态成功: {} -> {}, 用户: {}", request.getIds(), request.getStatus(), userId);
            return ResponseEntity.ok()
                    .body(Map.of("message", "成功更新 " + reports.size() + " 个报告"));
        } catch (Exception ex) {
            log.error("批量更新报告状态失败", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "批量更新失败"));
        }
    }

    /**
     * 单条报告读/改/删权限：管理员任意；本人 userId；子账号还可访问父账号在「父账号进行中项目」下的报告；
     * 主账号还可访问本人项目下任意报告（含子账号创建，与列表一致）。
     */
    private Report findReportAccessibleToUser(com.reportweb.entity.User currentUser, Integer reportId, boolean isAdmin) {
        if (isAdmin) {
            return reportRepository.findById(reportId).orElse(null);
        }
        String userId = currentUser.getId();
        Optional<Report> own = reportRepository.findByIdAndUserId(reportId, userId);
        if (own.isPresent()) {
            return own.get();
        }
        Report sub = reportAccessibleToSubUserOnParentInProgressProject(currentUser, reportId);
        if (sub != null) {
            return sub;
        }
        return reportAccessibleToProjectOwner(currentUser, reportId);
    }

    /** 主账号：报告所属项目的 owner 为当前用户 */
    private Report reportAccessibleToProjectOwner(com.reportweb.entity.User currentUser, Integer reportId) {
        if (UserRoleUtils.isSubUser(currentUser)) {
            return null;
        }
        Report r = reportRepository.findById(reportId).orElse(null);
        if (r == null || r.getProjectId() == null) {
            return null;
        }
        if (projectRepository.findByIdAndUserId(r.getProjectId(), currentUser.getId()).isEmpty()) {
            return null;
        }
        return r;
    }

    private Report reportAccessibleToSubUserOnParentInProgressProject(com.reportweb.entity.User currentUser, Integer reportId) {
        if (!UserRoleUtils.isSubUser(currentUser) || currentUser.getParentUserId() == null) {
            return null;
        }
        String parentId = currentUser.getParentUserId();
        Report r = reportRepository.findByIdAndUserId(reportId, parentId).orElse(null);
        if (r == null) {
            return null;
        }
        Integer pid = r.getProjectId();
        if (pid == null) {
            return null;
        }
        Project project = projectRepository.findByIdAndUserId(pid, parentId).orElse(null);
        if (project == null || !"InProgress".equals(project.getStatus())) {
            return null;
        }
        return r;
    }

    private boolean canUserMutateReport(com.reportweb.entity.User currentUser, Report report, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        if (report.getUserId().equals(currentUser.getId())) {
            return true;
        }
        if (canSubUserMutateParentOwnedReport(currentUser, report)) {
            return true;
        }
        return canProjectOwnerMutateReport(currentUser, report);
    }

    private boolean canProjectOwnerMutateReport(com.reportweb.entity.User currentUser, Report report) {
        if (UserRoleUtils.isSubUser(currentUser)) {
            return false;
        }
        Integer pid = report.getProjectId();
        if (pid == null) {
            return false;
        }
        return projectRepository.findByIdAndUserId(pid, currentUser.getId()).isPresent();
    }

    private boolean canSubUserMutateParentOwnedReport(com.reportweb.entity.User currentUser, Report report) {
        if (!UserRoleUtils.isSubUser(currentUser) || currentUser.getParentUserId() == null) {
            return false;
        }
        if (!currentUser.getParentUserId().equals(report.getUserId())) {
            return false;
        }
        Integer pid = report.getProjectId();
        if (pid == null) {
            return false;
        }
        Project project = projectRepository.findByIdAndUserId(pid, currentUser.getParentUserId()).orElse(null);
        return project != null && "InProgress".equals(project.getStatus());
    }

    private void syncAatProjectComponentsIfNeeded(Report report, ExperimentType experimentType) {
        if (experimentType == null || experimentType.getCode() == null
                || !("PMI".equalsIgnoreCase(experimentType.getCode())
                || "AAT".equalsIgnoreCase(experimentType.getCode()))) {
            return;
        }
        if (projectComponentSyncService.syncFromAatReportIfNeeded(report)) {
            reportRepository.save(report);
            reportRepository.flush();
        }
    }

    private void applyProjectComponentsFromDto(
            Report report,
            Integer projectId,
            List<Integer> projectComponentIds,
            Integer projectComponentId,
            String dtoComponentName,
            String dtoComponentSpec) {
        List<Integer> ids = reportComponentMergeHelper.resolveIdsFromDto(projectComponentIds, projectComponentId);
        if (ids.isEmpty()) {
            report.setProjectComponentId(null);
            report.setProjectComponentIds(null);
            report.setComponentName(dtoComponentName);
            report.setComponentSpec(dtoComponentSpec);
            return;
        }
        List<ProjectComponent> comps = reportComponentMergeHelper.validateAndLoadOrdered(projectId, ids, projectComponentRepository);
        report.setProjectComponentId(ids.get(0));
        if (ids.size() > 1) {
            report.setProjectComponentIds(new ArrayList<>(ids));
        } else {
            report.setProjectComponentIds(null);
        }
        report.setComponentName(comps.get(0).getComponentName());
        String merged = reportComponentMergeHelper.mergeSpecsUnified(comps);
        if (!merged.isEmpty()) {
            report.setComponentSpec(merged);
        } else {
            report.setComponentSpec(dtoComponentSpec);
        }
    }

    /** API 响应：无部件时 projectComponentIds 用 null 而非空列表 */
    private static List<Integer> emptyToNullIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids;
    }

    /**
     * 深拷贝 customFields 并合并实时计算的不符合记录（仅 API 响应，不写库）。
     */
    private Map<String, Object> customFieldsWithNonCompliance(Report report) {
        Map<String, Object> out = report.getCustomFields() != null
                ? new HashMap<>(report.getCustomFields())
                : new HashMap<>();
        Integer expId = report.getExperimentTypeId();
        if (expId == null) {
            return out;
        }
        ExperimentType experimentType = experimentTypeRepository.findById(expId).orElse(null);
        ProjectComponent component = null;
        if (report.getProjectComponentId() != null) {
            component = projectComponentRepository.findById(report.getProjectComponentId()).orElse(null);
        }
        List<DataComparisonService.NonComplianceRecord> records =
                defectDetectionService.listNonComplianceForMaterialComparison(report, experimentType, component);
        if (records == null || records.isEmpty()) {
            return out;
        }
        List<Map<String, String>> serialized = records.stream()
                .map(r -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("number", r.getNumber());
                    m.put("itemName", r.getItemName());
                    m.put("standardValue", r.getStandardValue());
                    m.put("actualValue", r.getActualValue());
                    m.put("result", r.getResult());
                    return m;
                })
                .collect(Collectors.toList());
        out.put("nonComplianceRecords", serialized);
        return out;
    }

    private ReportDTOs.ReportList convertToReportListDTO(Report report, ExperimentType experimentType) {
        ReportDTOs.ReportList dto = new ReportDTOs.ReportList();
        dto.setId(report.getId());
        dto.setProjectId(report.getProjectId());
        dto.setProjectNumber(report.getProject() != null ? report.getProject().getProjectNumber() : null);
        dto.setProjectName(report.getProject() != null ? report.getProject().getProjectName() : null);
        dto.setTitle(report.getTitle());
        dto.setReportNumber(report.getReportNumber());
        dto.setTestMethod(report.getTestMethod());
        dto.setTestDate(report.getTestDate());
        dto.setLocation(report.getLocation());
        dto.setStatus(report.getStatus());
        dto.setInspector(report.getInspector());
        dto.setEquipmentCategory(report.getEquipmentCategory());
        dto.setEquipmentName(report.getEquipmentName());
        dto.setComponentSpec(report.getComponentSpec());
        dto.setInstrumentModel(report.getInstrumentModel());
        dto.setInstrumentNumber(report.getInstrumentNumber());
        dto.setProjectComponentId(report.getProjectComponentId());
        dto.setProjectComponentIds(emptyToNullIds(reportComponentMergeHelper.resolveComponentIds(report)));
        dto.setProjectInstrumentId(report.getProjectInstrumentId());
        dto.setExperimentTypeId(report.getExperimentTypeId());
        dto.setExperimentTypeName(experimentType != null ? experimentType.getName() : null);
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        // 设置用户全名（如果已加载）
        dto.setUserFullName(report.getUser() != null && report.getUser().getFullName() != null 
                ? report.getUser().getFullName() : "未知用户");
        // 设置报告项数量（如果已加载，否则设为0避免懒加载问题）
        dto.setItemCount(report.getReportItems() != null ? report.getReportItems().size() : 0);
        dto.setReportImage(report.getReportImage());
        dto.setHasDefect(report.getHasDefect());
        dto.setCustomFields(customFieldsWithNonCompliance(report));
        dto.setDetectionContent(report.getDetectionContent());
        dto.setDetectionContentNarrative(detectionContentNarrativeService.getEffectiveNarrativeBody(report, experimentType));

        // 转换报告项列表
        List<ReportDTOs.ReportItemDTO> itemDTOs = new ArrayList<>();
        if (report.getReportItems() != null) {
            for (ReportItem item : report.getReportItems()) {
                ReportDTOs.ReportItemDTO itemDTO = new ReportDTOs.ReportItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setExperimentTypeId(item.getExperimentTypeId());
                itemDTO.setExperimentTypeName(null);
                itemDTO.setExperimentTypeCode(null);
                itemDTO.setTableData(item.getTableData());
                itemDTO.setSummary(item.getSummary());
                itemDTOs.add(itemDTO);
            }
        }
        dto.setReportItems(itemDTOs);

        // 添加附图列表
        try {
            List<ImageAttachment> attachments = imageAttachmentRepository
                    .findByReportIdOrderByDisplayOrder(report.getId());
            List<ImageAttachmentDTO> attachmentDTOs = attachments.stream()
                    .map(this::convertToImageAttachmentDTO)
                    .collect(Collectors.toList());
            dto.setImageAttachments(attachmentDTOs);
        } catch (Exception e) {
            log.warn("加载报告 {} 的附图失败: {}", report.getId(), e.getMessage());
            dto.setImageAttachments(new ArrayList<>());
        }

        return dto;
    }

    private ReportDTOs.ReportDetail convertToReportDetailDTO(Report report) {
        ReportDTOs.ReportDetail dto = new ReportDTOs.ReportDetail();
        dto.setId(report.getId());
        dto.setProjectId(report.getProjectId());
        dto.setProjectNumber(report.getProject() != null ? report.getProject().getProjectNumber() : null);
        dto.setProjectName(report.getProject() != null ? report.getProject().getProjectName() : null);
        dto.setTitle(report.getTitle());
        dto.setReportNumber(report.getReportNumber());
        dto.setInspector(report.getInspector());
        dto.setTestMethod(report.getTestMethod());
        dto.setEquipment(report.getEquipment());
        dto.setTestStandard(report.getTestStandard());
        dto.setComponentName(report.getComponentName());
        dto.setEquipmentCategory(report.getEquipmentCategory());
        dto.setEquipmentName(report.getEquipmentName());
        dto.setComponentSpec(report.getComponentSpec());
        dto.setInstrumentModel(report.getInstrumentModel());
        dto.setInstrumentNumber(report.getInstrumentNumber());
        dto.setProjectComponentId(report.getProjectComponentId());
        dto.setProjectComponentIds(emptyToNullIds(reportComponentMergeHelper.resolveComponentIds(report)));
        dto.setProjectInstrumentId(report.getProjectInstrumentId());
        dto.setTestDate(report.getTestDate());
        dto.setLocation(report.getLocation());
        dto.setStatus(report.getStatus());
        dto.setReportImage(report.getReportImage() != null ? report.getReportImage() : "");
        dto.setHasDefect(report.getHasDefect() != null ? report.getHasDefect() : "");
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        dto.setCustomFields(customFieldsWithNonCompliance(report));
        dto.setDetectionContent(report.getDetectionContent());

        // 转换报告项列表
        if (report.getReportItems() != null) {
            dto.setReportItems(report.getReportItems().stream()
                    .map(this::convertToReportItemDTO)
                    .collect(Collectors.toList()));
        }

        // 添加附图列表
        try {
            List<ImageAttachment> attachments = imageAttachmentRepository
                    .findByReportIdOrderByDisplayOrder(report.getId());
            List<ImageAttachmentDTO> attachmentDTOs = attachments.stream()
                    .map(this::convertToImageAttachmentDTO)
                    .collect(Collectors.toList());
            dto.setImageAttachments(attachmentDTOs);
        } catch (Exception e) {
            log.warn("加载报告 {} 的附图失败: {}", report.getId(), e.getMessage());
            dto.setImageAttachments(new ArrayList<>());
        }

        return dto;
    }

    private ReportDTOs.ReportItemDTO convertToReportItemDTO(ReportItem reportItem) {
        ReportDTOs.ReportItemDTO dto = new ReportDTOs.ReportItemDTO();
        dto.setId(reportItem.getId());
        dto.setExperimentTypeId(reportItem.getExperimentTypeId());
        dto.setExperimentTypeName(reportItem.getExperimentType() != null ? reportItem.getExperimentType().getName() : null);
        dto.setExperimentTypeCode(reportItem.getExperimentType() != null ? reportItem.getExperimentType().getCode() : null);
        dto.setTableData(reportItem.getTableData());
        dto.setSummary(reportItem.getSummary());
        return dto;
    }

    private ImageAttachmentDTO convertToImageAttachmentDTO(ImageAttachment attachment) {
        ImageAttachmentDTO dto = new ImageAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setDescription(attachment.getDescription());
        dto.setDisplayOrder(attachment.getDisplayOrder());

        // 解析JSON格式的图片URL列表
        try {
            List<String> imageUrls = objectMapper.readValue(
                    attachment.getImageUrls(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            dto.setImageUrls(imageUrls);
        } catch (Exception e) {
            log.error("解析附图URL列表失败: {}", e.getMessage());
            dto.setImageUrls(new ArrayList<>());
        }

        return dto;
    }

    /**
     * 转换Report为ReportSummaryDTO
     * @param report 报告对象
     * @param experimentType 检测类型（可选，如果为null会从数据库查询）
     * @param component 部件对象（可选，如果为null会从数据库查询）
     * @return ReportSummaryDTO
     */
    private ReportDTOs.ReportSummaryDTO convertToReportSummaryDTO(
            Report report, 
            ExperimentType experimentType, 
            ProjectComponent component) {
        
        ReportDTOs.ReportSummaryDTO dto = new ReportDTOs.ReportSummaryDTO();
        dto.setReportNumber(report.getReportNumber());
        dto.setComponentName(report.getComponentName());
        
        // 获取检测类型名称
        if (experimentType == null) {
            experimentType = experimentTypeRepository.findById(report.getExperimentTypeId()).orElse(null);
        }
        if (experimentType != null) {
            dto.setExperimentTypeName(experimentType.getName());
        } else {
            dto.setExperimentTypeName(null);
        }
        
        // 获取部件类别：优先从ProjectComponent获取，否则从Report.componentName或设为null
        if (component == null && report.getProjectComponentId() != null) {
            component = projectComponentRepository.findById(report.getProjectComponentId()).orElse(null);
        }
        if (component != null && component.getCategory() != null && !component.getCategory().isEmpty()) {
            dto.setCategory(component.getCategory());
        } else {
            // 如果ProjectComponent没有category，使用Report.componentName或设为null
            dto.setCategory(report.getComponentName());
        }
        
        // 判断是否存在缺陷：自动判定为 null 时使用用户保存的 hasDefect（VIS/SOD/MET 等）
        String hasDefect = defectDetectionService.hasDefect(report, experimentType, component);
        if (hasDefect == null) {
            hasDefect = report.getHasDefect();
        }
        dto.setHasDefect(hasDefect);
        
        return dto;
    }

    /**
     * 里氏硬度保存弹窗：按编号返回系统建议分类（管件/钢管/焊缝）。
     */
    @PostMapping("/leeb-classify-suggestions")
    public ResponseEntity<List<Map<String, String>>> leebClassifySuggestions(
            @RequestBody(required = false) LeebClassifyNumbersRequest body) {
        List<Map<String, String>> out = new ArrayList<>();
        if (body == null || body.getNumbers() == null) {
            return ResponseEntity.ok(out);
        }
        for (String num : body.getNumbers()) {
            LeebHardnessCategoryResolver.Category c =
                    LeebHardnessCategoryResolver.inferFromNumber(num);
            Map<String, String> row = new LinkedHashMap<>();
            row.put("number", num != null ? num : "");
            row.put("suggestedCategory", c.getStoredLabel());
            out.add(row);
        }
        return ResponseEntity.ok(out);
    }

    @Data
    public static class LeebClassifyNumbersRequest {
        private List<String> numbers;
    }

    @Data
    public static class BatchUpdateStatusRequest {
        private List<Integer> ids;
        private String status;
    }
}
