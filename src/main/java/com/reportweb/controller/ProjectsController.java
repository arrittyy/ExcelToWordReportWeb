package com.reportweb.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.dto.ProjectComponentDTOs;
import com.reportweb.dto.ProjectDTOs;
import com.reportweb.dto.ProjectInstrumentDTOs;
import com.reportweb.dto.ReportDTOs;
import com.reportweb.dto.ImageAttachmentDTO;
import com.reportweb.entity.ApprovalLog;
import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.Project;
import com.reportweb.entity.ProjectComponent;
import com.reportweb.entity.ProjectImageAttachment;
import com.reportweb.entity.ProjectInstrument;
import com.reportweb.entity.Report;
import com.reportweb.entity.WordExportJob;
import com.reportweb.entity.WordExportJobStatus;
import com.reportweb.entity.WordExportJobType;
import com.reportweb.entity.PowerPlant;
import com.reportweb.entity.Unit;
import com.reportweb.repository.ApprovalLogRepository;
import com.reportweb.repository.ExperimentTypeRepository;
import com.reportweb.repository.ProjectComponentRepository;
import com.reportweb.repository.ProjectImageAttachmentRepository;
import com.reportweb.repository.ProjectInstrumentRepository;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.repository.ReportRepository;
import com.reportweb.repository.PowerPlantRepository;
import com.reportweb.repository.UnitRepository;
import com.reportweb.security.CustomUserPrincipal;
import com.reportweb.security.UserRoleUtils;
import com.reportweb.security.WordExportJobAccess;
import com.reportweb.util.ThirdPartyPlaceholders;
import com.reportweb.service.ReportComponentMergeHelper;
import com.reportweb.service.DetectionContentNarrativeService;
import com.reportweb.service.WordExportJobRunner;
import com.reportweb.service.WordExportJobService;
import com.reportweb.service.WordGeneratorService;
import com.reportweb.service.ReportChangeLogService;
import com.reportweb.entity.ProjectReportChangeLog;
import com.reportweb.repository.ProjectReportChangeLogRepository;
import com.reportweb.service.ndt.NdtQualificationRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectsController {
    private static final long SUMMARY_ATTACHMENT_MAX_BYTES = 50L * 1024 * 1024;

    private final ProjectRepository projectRepository;
    private final ProjectComponentRepository projectComponentRepository;
    private final ProjectImageAttachmentRepository projectImageAttachmentRepository;
    private final ProjectInstrumentRepository projectInstrumentRepository;
    private final ReportRepository reportRepository;
    private final ApprovalLogRepository approvalLogRepository;
    private final ProjectReportChangeLogRepository projectReportChangeLogRepository;
    private final ReportChangeLogService reportChangeLogService;
    private final PowerPlantRepository powerPlantRepository;
    private final UnitRepository unitRepository;
    private final ExperimentTypeRepository experimentTypeRepository;
    private final WordGeneratorService wordGeneratorService;
    private final WordExportJobService wordExportJobService;
    private final WordExportJobRunner wordExportJobRunner;
    private final ReportComponentMergeHelper reportComponentMergeHelper;
    private final DetectionContentNarrativeService detectionContentNarrativeService;
    private final NdtQualificationRegistry ndtQualificationRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final String PROJECT_STATUS_IN_PROGRESS = "InProgress";
    private static final int STEP_WRITER = 0;
    private static final int STEP_PENDING_REVIEW = 1;
    private static final int STEP_PENDING_APPROVAL = 2;
    private static final int STEP_APPROVED = 3;

    private static boolean eqName(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private static boolean matchesAnyName(String roleName, String... principalNames) {
        if (roleName == null || roleName.isBlank() || principalNames == null || principalNames.length == 0) {
            return false;
        }
        for (String n : principalNames) {
            if (eqName(roleName, n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignedApprovalRole(Project project, String... principalNames) {
        if (project == null || principalNames == null || principalNames.length == 0) {
            return false;
        }
        return matchesAnyName(project.getWriterNdt(), principalNames)
                || matchesAnyName(project.getReviewerNdt(), principalNames)
                || matchesAnyName(project.getApproverNdt(), principalNames)
                || matchesAnyName(project.getWriterChem(), principalNames)
                || matchesAnyName(project.getReviewerChem(), principalNames)
                || matchesAnyName(project.getApproverChem(), principalNames)
                || matchesAnyName(project.getResponsiblePerson(), principalNames);
    }

    private static boolean isProjectResponsible(Project project, String... principalNames) {
        return project != null && matchesAnyName(project.getResponsiblePerson(), principalNames);
    }

    private static boolean canRollbackApproval(Project project,
                                              com.reportweb.entity.User currentUser,
                                              String... principalNames) {
        if (project == null || currentUser == null) {
            return false;
        }
        if (UserRoleUtils.isSubUser(currentUser)) {
            return false;
        }
        String userId = currentUser.getId();
        if (userId != null && userId.equals(project.getUserId())) {
            return true;
        }
        return isProjectResponsible(project, principalNames);
    }

    private static boolean hasApprovalPersonOrDateText(String name) {
        return name != null && !name.trim().isEmpty();
    }

    private static boolean trackNeedsRollback(Project project, String track) {
        if (project == null || track == null) {
            return false;
        }
        if ("ndt".equals(track)) {
            int step = project.getApprovalStepNdt() != null ? project.getApprovalStepNdt() : STEP_WRITER;
            if (step > STEP_WRITER) {
                return true;
            }
            if (project.getRejectionStepNdt() != null) {
                return true;
            }
            return hasApprovalPersonOrDateText(project.getWriterNdt())
                    || hasApprovalPersonOrDateText(project.getReviewerNdt())
                    || hasApprovalPersonOrDateText(project.getApproverNdt())
                    || project.getWriterDateNdt() != null
                    || project.getReviewDateNdt() != null
                    || project.getApprovalDateNdt() != null;
        }
        if ("chem".equals(track)) {
            int step = project.getApprovalStepChem() != null ? project.getApprovalStepChem() : STEP_WRITER;
            if (step > STEP_WRITER) {
                return true;
            }
            if (project.getRejectionStepChem() != null) {
                return true;
            }
            return hasApprovalPersonOrDateText(project.getWriterChem())
                    || hasApprovalPersonOrDateText(project.getReviewerChem())
                    || hasApprovalPersonOrDateText(project.getApproverChem())
                    || project.getWriterDateChem() != null
                    || project.getReviewDateChem() != null
                    || project.getApprovalDateChem() != null;
        }
        return false;
    }

    private static void resetApprovalTrack(Project project, String track) {
        if (project == null || track == null) {
            return;
        }
        if ("ndt".equals(track)) {
            project.setApprovalStepNdt(STEP_WRITER);
            project.setRejectionStepNdt(null);
            project.setWriterNdt(null);
            project.setReviewerNdt(null);
            project.setApproverNdt(null);
            project.setWriterDateNdt(null);
            project.setReviewDateNdt(null);
            project.setApprovalDateNdt(null);
        } else if ("chem".equals(track)) {
            project.setApprovalStepChem(STEP_WRITER);
            project.setRejectionStepChem(null);
            project.setWriterChem(null);
            project.setReviewerChem(null);
            project.setApproverChem(null);
            project.setWriterDateChem(null);
            project.setReviewDateChem(null);
            project.setApprovalDateChem(null);
        }
        if ("Completed".equals(project.getStatus())) {
            project.setStatus(PROJECT_STATUS_IN_PROGRESS);
        }
    }

    private static boolean canSubmitAtWriterStep(Project project, String track, String... principalNames) {
        if (project == null) {
            return false;
        }
        boolean isNdt = "ndt".equalsIgnoreCase(track);
        boolean atWriterStep = isNdt
                ? (project.getApprovalStepNdt() == null || project.getApprovalStepNdt() == STEP_WRITER)
                : (project.getApprovalStepChem() == null || project.getApprovalStepChem() == STEP_WRITER);
        if (!atWriterStep) {
            return false;
        }
        boolean isWriter = isNdt
                ? matchesAnyName(project.getWriterNdt(), principalNames)
                : matchesAnyName(project.getWriterChem(), principalNames);
        return isWriter || isProjectResponsible(project, principalNames);
    }

    private static String[] principalNames(com.reportweb.entity.User currentUser) {
        if (currentUser == null) {
            return new String[0];
        }
        List<String> names = new ArrayList<>(2);
        String full = currentUser.getFullName() != null ? currentUser.getFullName().trim() : "";
        if (!full.isEmpty()) {
            names.add(full);
        }
        String userName = currentUser.getUserName() != null ? currentUser.getUserName().trim() : "";
        if (!userName.isEmpty() && names.stream().noneMatch(n -> eqName(n, userName))) {
            names.add(userName);
        }
        return names.toArray(new String[0]);
    }

    private boolean canAccessProject(Project project,
                                     boolean isAdmin,
                                     boolean isSubUser,
                                     String effectiveUserId,
                                     String... principalNames) {
        if (project == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        if (effectiveUserId != null && effectiveUserId.equals(project.getUserId())) {
            if (!isSubUser) {
                return true;
            }
            return PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus());
        }
        return PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus()) && isAssignedApprovalRole(project, principalNames);
    }

    private Project findAccessibleProject(Integer id,
                                          boolean isAdmin,
                                          boolean isSubUser,
                                          String effectiveUserId,
                                          String... principalNames) {
        Project project = projectRepository.findById(id).orElse(null);
        if (!canAccessProject(project, isAdmin, isSubUser, effectiveUserId, principalNames)) {
            return null;
        }
        return project;
    }

    private List<Project> listVisibleProjectsForUser(boolean isAdmin,
                                                     boolean isSubUser,
                                                     String userId,
                                                     String effectiveUserId,
                                                     String... principalNames) {
        if (isAdmin) {
            return projectRepository.findAllOrderByCreatedAtDesc();
        }
        return isSubUser
                ? projectRepository.findByUserIdAndStatusOrderByCreatedAtDesc(effectiveUserId, PROJECT_STATUS_IN_PROGRESS)
                : projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private List<Project> listInProgressProjectsForTodos(boolean isAdmin, String... principalNames) {
        List<Project> inProgress = projectRepository.findByStatusOrderByCreatedAtDesc(PROJECT_STATUS_IN_PROGRESS);
        if (isAdmin) {
            return inProgress;
        }
        if (principalNames == null || principalNames.length == 0) {
            return List.of();
        }
        return inProgress.stream()
                .filter(p -> isAssignedApprovalRole(p, principalNames))
                .collect(Collectors.toList());
    }
    @GetMapping("/my-todos")
    @Transactional
    public ResponseEntity<List<ProjectDTOs.TodoItem>> getMyTodos(Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String[] principalNames = principalNames(currentUser);
            if (principalNames.length == 0) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);

            List<Project> projects = listInProgressProjectsForTodos(isAdmin, principalNames);

            List<ProjectDTOs.TodoItem> todos = new ArrayList<>();
            for (Project p : projects) {
                int ndt = p.getApprovalStepNdt() != null ? p.getApprovalStepNdt() : 0;
                int chem = p.getApprovalStepChem() != null ? p.getApprovalStepChem() : 0;
                // NDT: 步骤0且我是编写人；步骤1且我是审核人；步骤2且我是批准人
                if (ndt < STEP_APPROVED && matchesAnyName(p.getWriterNdt(), principalNames) && ndt == STEP_WRITER) {
                    todos.add(buildTodoItem(p, "ndt", "writer", ndt));
                } else if (ndt == STEP_PENDING_REVIEW && matchesAnyName(p.getReviewerNdt(), principalNames)) {
                    todos.add(buildTodoItem(p, "ndt", "reviewer", ndt));
                } else if (ndt == STEP_PENDING_APPROVAL && matchesAnyName(p.getApproverNdt(), principalNames)) {
                    todos.add(buildTodoItem(p, "ndt", "approver", ndt));
                }
                if (chem < STEP_APPROVED && matchesAnyName(p.getWriterChem(), principalNames) && chem == STEP_WRITER) {
                    todos.add(buildTodoItem(p, "chem", "writer", chem));
                } else if (chem == STEP_PENDING_REVIEW && matchesAnyName(p.getReviewerChem(), principalNames)) {
                    todos.add(buildTodoItem(p, "chem", "reviewer", chem));
                } else if (chem == STEP_PENDING_APPROVAL && matchesAnyName(p.getApproverChem(), principalNames)) {
                    todos.add(buildTodoItem(p, "chem", "approver", chem));
                }
            }
            return ResponseEntity.ok(todos);
        } catch (Exception ex) {
            log.error("Error getting my todos", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static String stepLabel(int step) {
        switch (step) {
            case 0: return "编写";
            case 1: return "待审核";
            case 2: return "待批准";
            case 3: return "已通过";
            default: return "未知";
        }
    }

    private static String roleLabel(String role) {
        if ("writer".equals(role)) return "编写人";
        if ("reviewer".equals(role)) return "审核人";
        if ("approver".equals(role)) return "批准人";
        return role;
    }

    private ProjectDTOs.TodoItem buildTodoItem(Project p, String track, String role, int step) {
        ProjectDTOs.TodoItem item = new ProjectDTOs.TodoItem();
        item.setProjectId(p.getId());
        item.setProjectNumber(p.getProjectNumber());
        item.setProjectName(p.getProjectName());
        item.setCustomer(p.getCustomer());
        item.setTrack(track);
        item.setRole(role);
        item.setStep(step);
        item.setStepLabel(stepLabel(step) + "（" + ("ndt".equals(track) ? "无损" : "理化") + "·" + roleLabel(role) + "）");
        return item;
    }

    @PostMapping("/{id}/submit-approval")
    @Transactional
    public ResponseEntity<?> submitApproval(
            @PathVariable Integer id,
            @RequestBody(required = false) ProjectDTOs.SubmitApprovalRequest body,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String[] principalNames = principalNames(currentUser);
            if (principalNames.length == 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "用户姓名为空，无法提交审批"));
            }
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames);
            if (project == null || !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String track = body != null && body.getTrack() != null ? body.getTrack().toLowerCase() : "both";
            boolean doNdt = ("ndt".equals(track) || "both".equals(track))
                    && canSubmitAtWriterStep(project, "ndt", principalNames);
            boolean doChem = ("chem".equals(track) || "both".equals(track))
                    && canSubmitAtWriterStep(project, "chem", principalNames);

            if (!doNdt && !doChem) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "仅编写人可提交审批，且当前步骤须为编写"));
            }
            if (doNdt) project.setApprovalStepNdt(STEP_PENDING_REVIEW);
            if (doChem) project.setApprovalStepChem(STEP_PENDING_REVIEW);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);
            if (doNdt) {
                ApprovalLog logNdt = new ApprovalLog();
                logNdt.setProjectId(id);
                logNdt.setTrack("ndt");
                logNdt.setAction("submit");
                logNdt.setActorName(principalNames[0]);
                approvalLogRepository.save(logNdt);
            }
            if (doChem) {
                ApprovalLog logChem = new ApprovalLog();
                logChem.setProjectId(id);
                logChem.setTrack("chem");
                logChem.setAction("submit");
                logChem.setActorName(principalNames[0]);
                approvalLogRepository.save(logChem);
            }
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error submit approval project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "提交审批失败"));
        }
    }

    @PostMapping("/{id}/approval/pass")
    @Transactional
    public ResponseEntity<?> approvalPass(
            @PathVariable Integer id,
            @RequestBody ProjectDTOs.ApprovalActionRequest body,
            Authentication authentication) {
        try {
            if (body == null || body.getTrack() == null || body.getTrack().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "请指定 track: ndt 或 chem"));
            }
            String track = body.getTrack().toLowerCase();
            if (!"ndt".equals(track) && !"chem".equals(track)) {
                return ResponseEntity.badRequest().body(Map.of("message", "track 须为 ndt 或 chem"));
            }
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String[] principalNames = principalNames(currentUser);
            if (principalNames.length == 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "用户姓名为空"));
            }
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames);
            if (project == null || !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            if ("ndt".equals(track)) {
                int step = project.getApprovalStepNdt() != null ? project.getApprovalStepNdt() : 0;
                if (step == STEP_PENDING_REVIEW && matchesAnyName(project.getReviewerNdt(), principalNames)) {
                    project.setApprovalStepNdt(STEP_PENDING_APPROVAL);
                    project.setReviewDateNdt(LocalDate.now());
                } else if (step == STEP_PENDING_APPROVAL && matchesAnyName(project.getApproverNdt(), principalNames)) {
                    project.setApprovalStepNdt(STEP_APPROVED);
                    project.setApprovalDateNdt(LocalDate.now());
                    project.setRejectionStepNdt(null);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前节点无权通过或状态不正确"));
                }
            } else {
                int step = project.getApprovalStepChem() != null ? project.getApprovalStepChem() : 0;
                if (step == STEP_PENDING_REVIEW && matchesAnyName(project.getReviewerChem(), principalNames)) {
                    project.setApprovalStepChem(STEP_PENDING_APPROVAL);
                    project.setReviewDateChem(LocalDate.now());
                } else if (step == STEP_PENDING_APPROVAL && matchesAnyName(project.getApproverChem(), principalNames)) {
                    project.setApprovalStepChem(STEP_APPROVED);
                    project.setApprovalDateChem(LocalDate.now());
                    project.setRejectionStepChem(null);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前节点无权通过或状态不正确"));
                }
            }
            int ndt = project.getApprovalStepNdt() != null ? project.getApprovalStepNdt() : 0;
            int chem = project.getApprovalStepChem() != null ? project.getApprovalStepChem() : 0;
            if (ndt == STEP_APPROVED && chem == STEP_APPROVED) {
                project.setStatus("Completed");
            }
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);
            ApprovalLog logEntry = new ApprovalLog();
            logEntry.setProjectId(id);
            logEntry.setTrack(track);
            logEntry.setAction("pass");
            logEntry.setActorName(principalNames[0]);
            approvalLogRepository.save(logEntry);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error approval pass project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "操作失败"));
        }
    }

    @PostMapping("/{id}/approval/reject")
    @Transactional
    public ResponseEntity<?> approvalReject(
            @PathVariable Integer id,
            @RequestBody ProjectDTOs.ApprovalActionRequest body,
            Authentication authentication) {
        try {
            if (body == null || body.getTrack() == null || body.getTrack().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "请指定 track: ndt 或 chem"));
            }
            String track = body.getTrack().toLowerCase();
            if (!"ndt".equals(track) && !"chem".equals(track)) {
                return ResponseEntity.badRequest().body(Map.of("message", "track 须为 ndt 或 chem"));
            }
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String[] principalNames = principalNames(currentUser);
            if (principalNames.length == 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "用户姓名为空"));
            }
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames);
            if (project == null || !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            if ("ndt".equals(track)) {
                int step = project.getApprovalStepNdt() != null ? project.getApprovalStepNdt() : 0;
                if ((step == STEP_PENDING_REVIEW && matchesAnyName(project.getReviewerNdt(), principalNames))
                        || (step == STEP_PENDING_APPROVAL && matchesAnyName(project.getApproverNdt(), principalNames))) {
                    project.setApprovalStepNdt(STEP_WRITER);
                    project.setRejectionStepNdt(step);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前节点无权不通过或状态不正确"));
                }
            } else {
                int step = project.getApprovalStepChem() != null ? project.getApprovalStepChem() : 0;
                if ((step == STEP_PENDING_REVIEW && matchesAnyName(project.getReviewerChem(), principalNames))
                        || (step == STEP_PENDING_APPROVAL && matchesAnyName(project.getApproverChem(), principalNames))) {
                    project.setApprovalStepChem(STEP_WRITER);
                    project.setRejectionStepChem(step);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前节点无权不通过或状态不正确"));
                }
            }
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);
            ApprovalLog logEntry = new ApprovalLog();
            logEntry.setProjectId(id);
            logEntry.setTrack(track);
            logEntry.setAction("reject");
            logEntry.setActorName(principalNames[0]);
            approvalLogRepository.save(logEntry);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error approval reject project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "操作失败"));
        }
    }

    @PostMapping("/{id}/approval/rollback")
    @Transactional
    public ResponseEntity<?> approvalRollback(
            @PathVariable Integer id,
            @RequestBody ProjectDTOs.ApprovalActionRequest body,
            Authentication authentication) {
        try {
            if (body == null || body.getTrack() == null || body.getTrack().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "请指定 track: ndt 或 chem"));
            }
            String track = body.getTrack().toLowerCase();
            if (!"ndt".equals(track) && !"chem".equals(track)) {
                return ResponseEntity.badRequest().body(Map.of("message", "track 须为 ndt 或 chem"));
            }
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String[] principalNames = principalNames(currentUser);

            Project project = projectRepository.findById(id).orElse(null);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (!canRollbackApproval(project, currentUser, principalNames)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "仅项目主账号或项目负责人可回退审批流程"));
            }
            if (!trackNeedsRollback(project, track)) {
                return ResponseEntity.badRequest().body(Map.of("message", "当前轨道无需回退"));
            }

            resetApprovalTrack(project, track);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);

            ApprovalLog logEntry = new ApprovalLog();
            logEntry.setProjectId(id);
            logEntry.setTrack(track);
            logEntry.setAction("rollback");
            logEntry.setActorName(principalNames.length > 0 ? principalNames[0] : currentUser.getUserName());
            approvalLogRepository.save(logEntry);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error approval rollback project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "回退失败"));
        }
    }

    @GetMapping("/{id}/approval-logs")
    public ResponseEntity<List<ProjectDTOs.ApprovalLogItem>> getApprovalLogs(
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
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(id).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
            }
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            List<ApprovalLog> logs = approvalLogRepository.findByProjectIdOrderByCreatedAtDesc(id);
            List<ProjectDTOs.ApprovalLogItem> items = logs.stream().map(log -> {
                ProjectDTOs.ApprovalLogItem item = new ProjectDTOs.ApprovalLogItem();
                item.setId(log.getId());
                item.setProjectId(log.getProjectId());
                item.setTrack(log.getTrack());
                item.setAction(log.getAction());
                item.setActorName(log.getActorName());
                item.setCreatedAt(log.getCreatedAt());
                return item;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(items);
        } catch (Exception ex) {
            log.error("Error getting approval logs for project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/report-change-logs")
    public ResponseEntity<List<ProjectDTOs.ReportChangeLogItem>> getReportChangeLogs(
            @PathVariable Integer id,
            @RequestParam(name = "limit", defaultValue = "500") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            Authentication authentication) {
        try {
            Project project = resolveProjectForRead(id, authentication);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            int safeLimit = Math.min(Math.max(limit, 1), 2000);
            int safeOffset = Math.max(offset, 0);
            List<ProjectReportChangeLog> logs = projectReportChangeLogRepository.findByProjectIdOrderByCreatedAtDesc(
                    id, org.springframework.data.domain.PageRequest.of(safeOffset / safeLimit, safeLimit));
            Set<Integer> existingReportIds = reportRepository.findByProjectIdOrderById(id).stream()
                    .map(Report::getId)
                    .collect(Collectors.toSet());
            List<ProjectDTOs.ReportChangeLogItem> items = logs.stream()
                    .map(log -> toReportChangeLogItem(log, existingReportIds))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(items);
        } catch (Exception ex) {
            log.error("Error getting report change logs for project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/report-change-summary")
    public ResponseEntity<ProjectDTOs.ReportChangeLogSummaryResponse> getReportChangeSummary(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            Project project = resolveProjectForRead(id, authentication);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            Map<Integer, Long> currentCounts = new HashMap<>();
            for (Object[] row : reportRepository.countByProjectIdGroupByExperimentTypeId(id)) {
                currentCounts.put((Integer) row[0], (Long) row[1]);
            }
            List<ProjectDTOs.ReportChangeLogSummaryRow> rows = projectReportChangeLogRepository
                    .aggregateByExperimentType(id).stream()
                    .map(p -> {
                        ProjectDTOs.ReportChangeLogSummaryRow row = new ProjectDTOs.ReportChangeLogSummaryRow();
                        row.setExperimentTypeId(p.getExperimentTypeId());
                        row.setExperimentTypeName(p.getExperimentTypeName());
                        row.setExperimentTypeCode(p.getExperimentTypeCode());
                        row.setCreatedCount(p.getCreatedCount() != null ? p.getCreatedCount() : 0L);
                        row.setUpdatedCount(p.getUpdatedCount() != null ? p.getUpdatedCount() : 0L);
                        row.setDeletedCount(p.getDeletedCount() != null ? p.getDeletedCount() : 0L);
                        row.setCurrentReportCount(currentCounts.getOrDefault(p.getExperimentTypeId(), 0L));
                        return row;
                    })
                    .collect(Collectors.toList());
            ProjectDTOs.ReportChangeLogSummaryResponse response = new ProjectDTOs.ReportChangeLogSummaryResponse();
            response.setByExperimentType(rows);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error getting report change summary for project id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Project resolveProjectForRead(Integer id, Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        com.reportweb.entity.User currentUser = userPrincipal.getUser();
        String userId = currentUser.getId();
        boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
        boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
        String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                ? currentUser.getParentUserId() : userId;
        Project project;
        if (isAdmin) {
            project = projectRepository.findById(id).orElse(null);
        } else {
            project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
        }
        if (project == null) {
            return null;
        }
        if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
            return null;
        }
        return project;
    }

    private static ProjectDTOs.ReportChangeLogItem toReportChangeLogItem(
            ProjectReportChangeLog log, Set<Integer> existingReportIds) {
        ProjectDTOs.ReportChangeLogItem item = new ProjectDTOs.ReportChangeLogItem();
        item.setId(log.getId());
        item.setProjectId(log.getProjectId());
        item.setReportId(log.getReportId());
        item.setAction(log.getAction());
        item.setExperimentTypeId(log.getExperimentTypeId());
        item.setExperimentTypeName(log.getExperimentTypeName());
        item.setExperimentTypeCode(log.getExperimentTypeCode());
        item.setReportNumber(log.getReportNumber());
        item.setTestMethod(log.getTestMethod());
        item.setStatus(log.getStatus());
        item.setChangeSummary(log.getChangeSummary());
        item.setOperatorUserId(log.getOperatorUserId());
        item.setOperatorUserName(log.getOperatorUserName());
        item.setSource(log.getSource());
        item.setCreatedAt(log.getCreatedAt());
        boolean deleted = !existingReportIds.contains(log.getReportId());
        item.setReportDeleted(deleted);
        return item;
    }

    @GetMapping
    @Transactional
    public ResponseEntity<List<ProjectDTOs.ProjectList>> getAllProjects(Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = userId;
            if (isSubUser && currentUser.getParentUserId() != null) {
                effectiveUserId = currentUser.getParentUserId();
            }

            // 管理员看全量；普通/子账号看归属项目 + 自身被分配审批角色的进行中项目
            List<Project> projects = listVisibleProjectsForUser(
                    isAdmin, isSubUser, userId, effectiveUserId, principalNames(currentUser));

            // 触发懒加载，避免LazyInitializationException
            for (Project project : projects) {
                if (project.getReports() != null) {
                    project.getReports().size();
                }
                if (project.getUser() != null) {
                    project.getUser().getFullName();
                }
            }

            List<ProjectDTOs.ProjectList> projectList = projects.stream()
                    .map(this::convertToProjectListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(projectList);
        } catch (Exception ex) {
            log.error("Error getting all projects", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}")
    @Transactional
    public ResponseEntity<List<ProjectDTOs.ProjectList>> getProjectsByUserId(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String currentUserId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);

            // 管理员可查看任何用户；普通用户只能查看自己；子账号只能请求主账号 ID
            if (isSubUser) {
                if (currentUser.getParentUserId() == null || !currentUser.getParentUserId().equals(userId)) {
                    log.warn("Sub-user {} attempted to access projects of user {}", currentUserId, userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            } else if (!isAdmin && !userId.equals(currentUserId)) {
                log.warn("User {} attempted to access projects of user {}", currentUserId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Project> projects = isSubUser
                    ? projectRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PROJECT_STATUS_IN_PROGRESS)
                    : projectRepository.findByUserIdOrderByCreatedAtDesc(userId);

            // 触发懒加载，避免LazyInitializationException
            for (Project project : projects) {
                if (project.getReports() != null) {
                    project.getReports().size();
                }
                if (project.getUser() != null) {
                    project.getUser().getFullName();
                }
            }

            List<ProjectDTOs.ProjectList> projectList = projects.stream()
                    .map(this::convertToProjectListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(projectList);
        } catch (Exception ex) {
            log.error("Error getting projects for user: {}", userId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<ProjectDTOs.ProjectDetail> getProject(
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

            // 管理员可查看任何项目；普通用户/子账号只能查看归属 effectiveUserId 的项目；子账号仅能看进行中
            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames(currentUser));

            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            // 触发懒加载，确保关联数据已加载
            if (project.getReports() != null) {
                project.getReports().size();
                for (Report report : project.getReports()) {
                    if (report.getReportItems() != null) {
                        report.getReportItems().size();
                    }
                }
            }
            if (project.getUser() != null) {
                project.getUser().getFullName();
            }

            ProjectDTOs.ProjectDetail projectDetail = convertToProjectDetailDTO(project);
            projectDetail.setCanRollbackApproval(
                    canRollbackApproval(project, currentUser, principalNames(currentUser)));
            return ResponseEntity.ok(projectDetail);
        } catch (Exception ex) {
            log.error("Error getting project with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/aggregate-detection-log-order")
    @Transactional
    public ResponseEntity<?> saveAggregateDetectionLogOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) ProjectDTOs.AggregateDetectionLogOrderUpdate body,
            Authentication authentication) {
        try {
            if (body == null || body.getComponentKeys() == null || body.getReportIdsByComponent() == null) {
                Map<String, String> err = new HashMap<>();
                err.put("message", "请求体无效");
                return ResponseEntity.badRequest().body(err);
            }
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;
            String[] principalNames = principalNames(currentUser);

            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            List<Report> projectReports = reportRepository.findByProjectIdOrderById(id);
            Set<Integer> allowedIds = projectReports.stream()
                    .map(Report::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (List<Integer> idList : body.getReportIdsByComponent().values()) {
                if (idList == null) {
                    continue;
                }
                for (Integer rid : idList) {
                    if (rid == null || !allowedIds.contains(rid)) {
                        Map<String, String> err = new HashMap<>();
                        err.put("message", "包含不属于本项目的报告 ID");
                        return ResponseEntity.badRequest().body(err);
                    }
                }
            }

            Map<String, Object> toStore = new HashMap<>();
            toStore.put("version", body.getVersion() != null ? body.getVersion() : 4);
            toStore.put("componentKeys", body.getComponentKeys());
            toStore.put("reportIdsByComponent", body.getReportIdsByComponent());
            if (body.getExperimentTypeOrder() != null && !body.getExperimentTypeOrder().isEmpty()) {
                List<String> eto = new java.util.ArrayList<>();
                for (String s : body.getExperimentTypeOrder()) {
                    if (s == null) {
                        continue;
                    }
                    String t = s.trim();
                    if (!t.isEmpty()) {
                        eto.add(t);
                    }
                }
                if (!eto.isEmpty()) {
                    toStore.put("experimentTypeOrder", eto);
                }
            }
            String json = objectMapper.writeValueAsString(toStore);
            project.setAggregateDetectionLogOrder(json);
            projectRepository.save(project);
            if (project.getReports() != null) {
                project.getReports().size();
            }
            if (project.getUser() != null) {
                project.getUser().getFullName();
            }
            return ResponseEntity.ok(convertToProjectDetailDTO(project));
        } catch (Exception ex) {
            log.error("Error saving aggregate detection log order for project {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 项目检测部件列表（与项目详情同域注册，避免多 Controller 下部分环境未匹配到 /api/projects/{id}/components）。
     */
    @GetMapping("/{id}/components")
    @Transactional
    public ResponseEntity<List<ProjectComponentDTOs.ComponentList>> getProjectComponentsNested(
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
            String[] principalNames = principalNames(currentUser);

            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            List<ProjectComponent> components = projectComponentRepository.findByProjectId(id);
            List<ProjectComponentDTOs.ComponentList> componentList = components.stream()
                    .map(this::convertProjectComponentToListDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(componentList);
        } catch (Exception ex) {
            log.error("Error getting project components for project {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 项目仪器设备列表（与项目详情同域注册）。
     */
    @GetMapping("/{id}/instruments")
    @Transactional
    public ResponseEntity<List<ProjectInstrumentDTOs.InstrumentList>> getProjectInstrumentsNested(
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
            String[] principalNames = principalNames(currentUser);

            Project project = findAccessibleProject(id, isAdmin, isSubUser, effectiveUserId, principalNames);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            List<ProjectInstrument> instruments = projectInstrumentRepository.findByProjectId(id);
            List<ProjectInstrumentDTOs.InstrumentList> instrumentList = instruments.stream()
                    .map(this::convertProjectInstrumentToListDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(instrumentList);
        } catch (Exception ex) {
            log.error("Error getting project instruments for project {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ProjectComponentDTOs.ComponentList convertProjectComponentToListDto(ProjectComponent component) {
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

    private ProjectInstrumentDTOs.InstrumentList convertProjectInstrumentToListDto(ProjectInstrument instrument) {
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

    @PostMapping
    public ResponseEntity<?> createProject(
            @Valid @RequestBody ProjectDTOs.CreateProject createProjectDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            // 子账号创建项目时归属主账号
            String userId = UserRoleUtils.isSubUser(currentUser) && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : currentUser.getId();

            // 检查项目编号是否已存在
            if (projectRepository.existsByProjectNumber(createProjectDTO.getProjectNumber())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "项目编号已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 验证电厂是否存在（如果提供了电厂ID）
            if (createProjectDTO.getPowerPlantId() != null) {
                PowerPlant powerPlant = powerPlantRepository.findById(createProjectDTO.getPowerPlantId())
                        .orElse(null);
                if (powerPlant == null) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "电厂不存在");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
            }

            Project project = new Project();
            project.setProjectNumber(createProjectDTO.getProjectNumber());
            project.setThirdPartyProjectNumber(ThirdPartyPlaceholders.blankToDefault(createProjectDTO.getThirdPartyProjectNumber()));
            project.setThirdPartyName(ThirdPartyPlaceholders.blankToDefault(createProjectDTO.getThirdPartyName()));
            project.setProjectName(createProjectDTO.getProjectName());
            project.setProjectType(createProjectDTO.getProjectType());
            project.setCustomer(createProjectDTO.getCustomer());
            // 同时设置customerName（数据库有NOT NULL约束）
            project.setCustomerName(createProjectDTO.getCustomer());
            project.setCustomerContact(createProjectDTO.getCustomerContact());
            project.setPowerPlantId(createProjectDTO.getPowerPlantId());
            project.setUnitId(createProjectDTO.getUnitId());
            project.setUserId(userId);
            project.setStartDate(createProjectDTO.getStartDate());
            project.setEndDate(createProjectDTO.getEndDate());
            project.setStatus("InProgress");
            project.setDescription(createProjectDTO.getDescription());
            project.setResponsiblePerson(createProjectDTO.getResponsiblePerson());
            project.setReviewerNdt(createProjectDTO.getReviewerNdt());
            project.setReviewDateNdt(createProjectDTO.getReviewDateNdt());
            project.setApproverNdt(createProjectDTO.getApproverNdt());
            project.setApprovalDateNdt(createProjectDTO.getApprovalDateNdt());
            project.setWriterNdt(createProjectDTO.getWriterNdt());
            project.setWriterDateNdt(createProjectDTO.getWriterDateNdt());
            project.setNdtSignatureLevels(createProjectDTO.getNdtSignatureLevels());
            if (createProjectDTO.getThirdPartyApprovalByExperimentType() != null) {
                project.setThirdPartyApprovalByExperimentType(createProjectDTO.getThirdPartyApprovalByExperimentType());
            }
            project.setReviewerChem(createProjectDTO.getReviewerChem());
            project.setReviewDateChem(createProjectDTO.getReviewDateChem());
            project.setApproverChem(createProjectDTO.getApproverChem());
            project.setApprovalDateChem(createProjectDTO.getApprovalDateChem());
            project.setWriterChem(createProjectDTO.getWriterChem());
            project.setWriterDateChem(createProjectDTO.getWriterDateChem());
            project.setStaff(createProjectDTO.getStaff());

            // 保存选中的检测类型ID列表（JSON格式）
            if (createProjectDTO.getSelectedExperimentTypeIds() != null && !createProjectDTO.getSelectedExperimentTypeIds().isEmpty()) {
                String selectedIdsJson = objectMapper.writeValueAsString(createProjectDTO.getSelectedExperimentTypeIds());
                project.setSelectedExperimentTypeIds(selectedIdsJson);
            }

            project.setCreatedAt(LocalDateTime.now());
            project.setUpdatedAt(LocalDateTime.now());

            Project savedProject = projectRepository.save(project);

            // 触发懒加载，确保关联数据已加载
            if (savedProject.getUser() != null) {
                savedProject.getUser().getFullName();
            }

            ProjectDTOs.ProjectDetail response = convertToProjectDetailDTO(savedProject);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating project", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建项目失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateProject(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectDTOs.UpdateProject updateProjectDTO,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            // 管理员可更新任何项目；普通用户/子账号只能更新归属 effectiveUserId 的项目；子账号仅能更新进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(id).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
            }

            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 检查项目编号是否已被其他项目使用
            if (!project.getProjectNumber().equals(updateProjectDTO.getProjectNumber()) &&
                    projectRepository.existsByProjectNumberAndIdNot(updateProjectDTO.getProjectNumber(), id)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "项目编号已被其他项目使用");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 验证电厂是否存在（如果提供了电厂ID）
            if (updateProjectDTO.getPowerPlantId() != null) {
                PowerPlant powerPlant = powerPlantRepository.findById(updateProjectDTO.getPowerPlantId())
                        .orElse(null);
                if (powerPlant == null) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "电厂不存在");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
            }

            project.setProjectNumber(updateProjectDTO.getProjectNumber());
            project.setThirdPartyProjectNumber(ThirdPartyPlaceholders.blankToDefault(updateProjectDTO.getThirdPartyProjectNumber()));
            project.setThirdPartyName(ThirdPartyPlaceholders.blankToDefault(updateProjectDTO.getThirdPartyName()));
            project.setProjectName(updateProjectDTO.getProjectName());
            project.setProjectType(updateProjectDTO.getProjectType());
            project.setCustomer(updateProjectDTO.getCustomer());
            // 同时设置customerName（数据库有NOT NULL约束）
            project.setCustomerName(updateProjectDTO.getCustomer());
            project.setCustomerContact(updateProjectDTO.getCustomerContact());
            project.setPowerPlantId(updateProjectDTO.getPowerPlantId());
            project.setUnitId(updateProjectDTO.getUnitId());
            project.setStartDate(updateProjectDTO.getStartDate());
            project.setEndDate(updateProjectDTO.getEndDate());
            project.setStatus(updateProjectDTO.getStatus());
            project.setDescription(updateProjectDTO.getDescription());
            project.setResponsiblePerson(updateProjectDTO.getResponsiblePerson());
            project.setReviewerNdt(updateProjectDTO.getReviewerNdt());
            project.setReviewDateNdt(updateProjectDTO.getReviewDateNdt());
            project.setApproverNdt(updateProjectDTO.getApproverNdt());
            project.setApprovalDateNdt(updateProjectDTO.getApprovalDateNdt());
            project.setWriterNdt(updateProjectDTO.getWriterNdt());
            project.setWriterDateNdt(updateProjectDTO.getWriterDateNdt());
            project.setNdtSignatureLevels(updateProjectDTO.getNdtSignatureLevels());
            if (updateProjectDTO.getThirdPartyApprovalByExperimentType() != null) {
                project.setThirdPartyApprovalByExperimentType(updateProjectDTO.getThirdPartyApprovalByExperimentType());
            }
            project.setReviewerChem(updateProjectDTO.getReviewerChem());
            project.setReviewDateChem(updateProjectDTO.getReviewDateChem());
            project.setApproverChem(updateProjectDTO.getApproverChem());
            project.setApprovalDateChem(updateProjectDTO.getApprovalDateChem());
            project.setWriterChem(updateProjectDTO.getWriterChem());
            project.setWriterDateChem(updateProjectDTO.getWriterDateChem());
            project.setStaff(updateProjectDTO.getStaff());

            // 保存选中的检测类型ID列表（JSON格式）
            if (updateProjectDTO.getSelectedExperimentTypeIds() != null && !updateProjectDTO.getSelectedExperimentTypeIds().isEmpty()) {
                String selectedIdsJson = objectMapper.writeValueAsString(updateProjectDTO.getSelectedExperimentTypeIds());
                project.setSelectedExperimentTypeIds(selectedIdsJson);
            } else {
                project.setSelectedExperimentTypeIds(null);
            }

            project.setUpdatedAt(LocalDateTime.now());

            projectRepository.save(project);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error updating project with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新项目失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteProject(
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

            // 管理员可删除任何项目；普通用户/子账号只能删除归属 effectiveUserId 的项目；子账号仅能删除进行中
            Project project;
            if (isAdmin) {
                project = projectRepository.findById(id).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
            }

            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 级联删除关联的报告
            List<Report> reports = reportRepository.findByProjectIdOrderById(id);
            if (!reports.isEmpty()) {
                reportChangeLogService.recordDeletedAll(
                        reports, currentUser, ReportChangeLogService.SOURCE_PROJECT_DELETE);
                reportRepository.deleteAll(reports);
            }

            projectRepository.delete(project);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting project with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除项目失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/summary-notification-signed")
    @Transactional
    public ResponseEntity<?> uploadSummaryNotificationSigned(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return uploadSummaryAttachment(id, file, authentication, true);
    }

    @DeleteMapping("/{id}/summary-notification-signed")
    @Transactional
    public ResponseEntity<?> deleteSummaryNotificationSigned(
            @PathVariable Integer id,
            Authentication authentication) {
        return deleteSummaryAttachment(id, authentication, true);
    }

    @PostMapping("/{id}/summary-third-party-full")
    @Transactional
    public ResponseEntity<?> uploadSummaryThirdPartyFull(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return uploadSummaryAttachment(id, file, authentication, false);
    }

    @DeleteMapping("/{id}/summary-third-party-full")
    @Transactional
    public ResponseEntity<?> deleteSummaryThirdPartyFull(
            @PathVariable Integer id,
            Authentication authentication) {
        return deleteSummaryAttachment(id, authentication, false);
    }

    @GetMapping("/{id}/report-figures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProjectReportFigures(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            List<ImageAttachmentDTO> data = projectImageAttachmentRepository
                    .findByProjectIdOrderByDisplayOrder(id)
                    .stream()
                    .map(this::convertProjectImageAttachmentToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(data);
        } catch (Exception ex) {
            log.error("Error getting report figures for project {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "加载报告附图失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/report-figures")
    @Transactional
    public ResponseEntity<?> saveProjectReportFigures(
            @PathVariable Integer id,
            @RequestBody(required = false) List<ImageAttachmentDTO> figures,
            Authentication authentication) {
        try {
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            List<ImageAttachmentDTO> payload = figures != null ? figures : Collections.emptyList();
            for (ImageAttachmentDTO dto : payload) {
                if (dto == null || dto.getImageUrls() == null || dto.getImageUrls().isEmpty()) {
                    Map<String, String> err = new HashMap<>();
                    err.put("message", "每行附图至少上传一张图片");
                    return ResponseEntity.badRequest().body(err);
                }
            }

            projectImageAttachmentRepository.deleteByProjectId(id);
            for (int i = 0; i < payload.size(); i++) {
                ImageAttachmentDTO dto = payload.get(i);
                ProjectImageAttachment entity = new ProjectImageAttachment();
                entity.setProjectId(id);
                entity.setImageUrls(objectMapper.writeValueAsString(dto.getImageUrls()));
                entity.setDescription(dto.getDescription());
                entity.setDisplayOrder(i);
                projectImageAttachmentRepository.save(entity);
            }

            List<ImageAttachmentDTO> saved = projectImageAttachmentRepository
                    .findByProjectIdOrderByDisplayOrder(id)
                    .stream()
                    .map(this::convertProjectImageAttachmentToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception ex) {
            log.error("Error saving report figures for project {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "保存报告附图失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/overview-preview")
    public ResponseEntity<?> getOverviewPreview(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            initializeProjectForWordGeneration(project);
            return ResponseEntity.ok(wordGeneratorService.buildProjectOverviewPreview(project));
        } catch (IllegalArgumentException ex) {
            log.warn("Validation failed overview preview for project {}: {}", id, ex.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "概述预览失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Error building overview preview for project {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "概述预览失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/generate-summary-word")
    public ResponseEntity<?> generateSummaryWord(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            initializeProjectForWordGeneration(project);

            byte[] wordBytes = wordGeneratorService.generateProjectSummaryAsync(project);
            String fileName = String.format("%s_%s_总报告.docx",
                    project.getProjectNumber(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(wordBytes);
        } catch (IllegalArgumentException ex) {
            log.warn("Validation failed generating summary word for project with id: {}, reason: {}", id, ex.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "生成项目总报告失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Error generating summary word for project with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "生成项目总报告失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/generate-third-party-word")
    public ResponseEntity<?> generateThirdPartyWord(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            // 与总报告导出一致：子账号按父账号 userId 解析项目归属
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            initializeProjectForWordGeneration(project);

            byte[] wordBytes = wordGeneratorService.generateThirdPartyOnlyProjectWordAsync(project);
            String fileName = String.format("%s_%s_第三方报告.docx",
                    project.getProjectNumber(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(wordBytes);
        } catch (IllegalArgumentException ex) {
            log.warn("Bad request generating third-party word for project {}: {}", id, ex.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "无法生成第三方报告");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Error generating third-party word for project with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "生成第三方报告失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 生成技术监督检测通知单 Word（按所选报告 ID） */
    @PostMapping("/{id}/generate-detection-notification-word")
    public ResponseEntity<?> generateDetectionNotificationWord(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectDTOs.DetectionNotificationRequest body,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();
            String userId = currentUser.getId();
            boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
            boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
            String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                    ? currentUser.getParentUserId() : userId;

            Project project;
            if (isAdmin) {
                project = projectRepository.findById(id).orElse(null);
            } else {
                project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
            }

            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            if (project.getReports() != null) {
                project.getReports().size();
                for (Report report : project.getReports()) {
                    if (report.getReportItems() != null) {
                        report.getReportItems().size();
                        report.getReportItems().forEach(ri -> {
                            if (ri.getExperimentType() != null) {
                                ri.getExperimentType().getName();
                            }
                        });
                    }
                    if (report.getImageAttachments() != null) {
                        report.getImageAttachments().size();
                    }
                }
            }

            byte[] wordBytes = wordGeneratorService.generateTechnicalSupervisionNotifications(project, body.getReportIds());
            String fileName = String.format("%s_%s_检测通知单.docx",
                    project.getProjectNumber(),
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(wordBytes);
        } catch (IllegalArgumentException ex) {
            log.warn("Bad request generating detection notification for project {}: {}", id, ex.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "无法生成检测通知单");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Error generating detection notification word for project with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "生成检测通知单失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/word-export-jobs")
    public ResponseEntity<?> createWordExportJob(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectDTOs.CreateWordExportJobRequest body,
            Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            com.reportweb.entity.User currentUser = userPrincipal.getUser();

            Project project;
            if (body.getType() == WordExportJobType.DETECTION_NOTIFICATION) {
                project = getAccessibleProjectForDetectionNotification(id, currentUser);
            } else {
                project = getAccessibleProject(id, authentication, true);
            }

            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            if (body.getType() == WordExportJobType.DETECTION_NOTIFICATION
                    && (body.getReportIds() == null || body.getReportIds().isEmpty())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "请至少选择一条报告");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            WordExportJob job = wordExportJobService.createJob(
                    id,
                    currentUser.getId(),
                    body.getType(),
                    body.getReportIds(),
                    project.getProjectNumber()
            );
            wordExportJobRunner.run(job.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(toWordExportJobResponse(job));
        } catch (IllegalArgumentException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "创建导出任务失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Failed to create word export job for project {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建导出任务失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/word-export-jobs/{jobId}")
    public ResponseEntity<?> getWordExportJob(
            @PathVariable Integer id,
            @PathVariable String jobId,
            Authentication authentication) {
        try {
            WordExportJob job = wordExportJobService.getJob(jobId);
            if (!Objects.equals(job.getProjectId(), id)) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessWordExportJob(authentication, job)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(toWordExportJobResponse(job));
        } catch (IllegalArgumentException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "查询导出任务失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Failed to get word export job {} for project {}", jobId, id, ex);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/word-export-jobs/latest")
    public ResponseEntity<?> getLatestWordExportJob(
            @PathVariable Integer id,
            @RequestParam("type") WordExportJobType type,
            Authentication authentication) {
        try {
            WordExportJob job = wordExportJobService.getLatestJob(id, type).orElse(null);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessWordExportJob(authentication, job)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(toWordExportJobResponse(job));
        } catch (Exception ex) {
            log.error("Failed to get latest word export job for project {} type {}", id, type, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/word-export-jobs/{jobId}/download")
    public ResponseEntity<?> downloadWordExportJob(
            @PathVariable Integer id,
            @PathVariable String jobId,
            Authentication authentication) {
        try {
            WordExportJob job = wordExportJobService.getJob(jobId);
            if (!Objects.equals(job.getProjectId(), id)) {
                return ResponseEntity.notFound().build();
            }
            if (!canAccessWordExportJob(authentication, job)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (job.getStatus() != WordExportJobStatus.SUCCEEDED) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "导出任务尚未完成");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            Path outputPath = wordExportJobService.resolveOutputPath(job);
            Resource resource = new FileSystemResource(outputPath);
            String fileName = job.getSuggestedFileName() != null ? job.getSuggestedFileName() : (jobId + ".docx");
            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(resource);
        } catch (IllegalArgumentException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "下载导出文件失败");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception ex) {
            log.error("Failed to download word export job {} for project {}", jobId, id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "下载导出文件失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private ProjectDTOs.WordExportJobResponse toWordExportJobResponse(WordExportJob job) {
        ProjectDTOs.WordExportJobResponse dto = new ProjectDTOs.WordExportJobResponse();
        dto.setJobId(job.getId());
        dto.setType(job.getType());
        dto.setStatus(job.getStatus());
        dto.setSuggestedFileName(job.getSuggestedFileName());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setStartedAt(job.getStartedAt());
        dto.setFinishedAt(job.getFinishedAt());
        return dto;
    }

    private boolean canAccessWordExportJob(Authentication authentication, WordExportJob job) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        com.reportweb.entity.User currentUser = userPrincipal.getUser();
        Project project = projectRepository.findById(job.getProjectId()).orElse(null);
        return WordExportJobAccess.canAccess(currentUser, job, project);
    }

    private Project getAccessibleProjectForDetectionNotification(Integer id, com.reportweb.entity.User currentUser) {
        String userId = currentUser.getId();
        boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
        boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
        String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                ? currentUser.getParentUserId() : userId;

        Project project;
        if (isAdmin) {
            project = projectRepository.findById(id).orElse(null);
        } else {
            project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
        }
        if (project == null) {
            return null;
        }
        if (isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
            return null;
        }
        return project;
    }

    private ResponseEntity<?> uploadSummaryAttachment(
            Integer id,
            MultipartFile file,
            Authentication authentication,
            boolean notificationAttachment) {
        try {
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            validateSummaryAttachment(file, notificationAttachment);

            String originalFileName = file.getOriginalFilename() != null
                    ? Paths.get(file.getOriginalFilename()).getFileName().toString()
                    : (notificationAttachment ? "notification.docx" : "third-party.docx");
            String extension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
            }

            Path attachmentDir = resolveProjectAttachmentDirectory(project);
            Files.createDirectories(attachmentDir);

            String prefix = notificationAttachment ? "summary_notification_signed_" : "summary_third_party_full_";
            String storedFileName = prefix + UUID.randomUUID() + extension;
            Path targetPath = attachmentDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            deleteProjectAttachmentFile(project, notificationAttachment);

            String relPath = "projects/" + project.getId() + "/" + storedFileName;
            if (notificationAttachment) {
                project.setSummaryNotificationSignedRelPath(relPath);
                project.setSummaryNotificationSignedOriginalName(originalFileName);
            } else {
                project.setSummaryThirdPartyFullRelPath(relPath);
                project.setSummaryThirdPartyFullOriginalName(originalFileName);
            }
            project.setUpdatedAt(LocalDateTime.now());

            Project savedProject = projectRepository.save(project);
            if (savedProject.getUser() != null) {
                savedProject.getUser().getFullName();
            }
            return ResponseEntity.ok(convertToProjectDetailDTO(savedProject));
        } catch (IllegalArgumentException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "附件上传失败");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IOException ex) {
            log.error("Error uploading summary attachment for project {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "附件上传失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private ResponseEntity<?> deleteSummaryAttachment(
            Integer id,
            Authentication authentication,
            boolean notificationAttachment) {
        try {
            Project project = getAccessibleProject(id, authentication, true);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }

            deleteProjectAttachmentFile(project, notificationAttachment);
            if (notificationAttachment) {
                project.setSummaryNotificationSignedRelPath(null);
                project.setSummaryNotificationSignedOriginalName(null);
            } else {
                project.setSummaryThirdPartyFullRelPath(null);
                project.setSummaryThirdPartyFullOriginalName(null);
            }
            project.setUpdatedAt(LocalDateTime.now());

            Project savedProject = projectRepository.save(project);
            if (savedProject.getUser() != null) {
                savedProject.getUser().getFullName();
            }
            return ResponseEntity.ok(convertToProjectDetailDTO(savedProject));
        } catch (IOException ex) {
            log.error("Error deleting summary attachment for project {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除附件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Project getAccessibleProject(Integer id, Authentication authentication, boolean requireInProgressForSubUser) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        com.reportweb.entity.User currentUser = userPrincipal.getUser();
        String userId = currentUser.getId();
        boolean isAdmin = UserRoleUtils.isAdmin(currentUser);
        boolean isSubUser = UserRoleUtils.isSubUser(currentUser);
        String effectiveUserId = isSubUser && currentUser.getParentUserId() != null
                ? currentUser.getParentUserId() : userId;

        Project project;
        if (isAdmin) {
            project = projectRepository.findById(id).orElse(null);
        } else {
            project = projectRepository.findByIdAndUserId(id, effectiveUserId).orElse(null);
        }

        if (project == null) {
            return null;
        }
        if (requireInProgressForSubUser && isSubUser && !PROJECT_STATUS_IN_PROGRESS.equals(project.getStatus())) {
            return null;
        }
        return project;
    }

    private void initializeProjectForWordGeneration(Project project) {
        if (project.getReports() == null) {
            return;
        }
        project.getReports().size();
        for (Report report : project.getReports()) {
            if (report.getReportItems() != null) {
                report.getReportItems().size();
                report.getReportItems().forEach(ri -> {
                    if (ri.getExperimentType() != null) {
                        ri.getExperimentType().getName();
                    }
                });
            }
            if (report.getImageAttachments() != null) {
                report.getImageAttachments().size();
            }
        }
        if (project.getProjectImageAttachments() != null) {
            project.getProjectImageAttachments().size();
        }
    }

    private ImageAttachmentDTO convertProjectImageAttachmentToDTO(ProjectImageAttachment attachment) {
        ImageAttachmentDTO dto = new ImageAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setDescription(attachment.getDescription());
        dto.setDisplayOrder(attachment.getDisplayOrder());
        try {
            dto.setImageUrls(objectMapper.readValue(attachment.getImageUrls(), new TypeReference<List<String>>() {
            }));
        } catch (Exception ex) {
            dto.setImageUrls(Collections.emptyList());
        }
        return dto;
    }

    private void validateSummaryAttachment(MultipartFile file, boolean notificationAttachment) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > SUMMARY_ATTACHMENT_MAX_BYTES) {
            throw new IllegalArgumentException("附件大小不能超过50MB");
        }
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
            }
        }
        if (notificationAttachment && !".docx".equals(extension) && !".pdf".equals(extension)) {
            throw new IllegalArgumentException("仅支持上传 docx、pdf 文件");
        }
        if (!notificationAttachment && !".pdf".equals(extension)) {
            throw new IllegalArgumentException("第三方报告完整版仅支持上传 PDF 文件，以保持原始版式和页眉页脚");
        }
    }

    private Path resolveProjectAttachmentDirectory(Project project) {
        return Paths.get(uploadDir).resolve("projects").resolve(String.valueOf(project.getId()));
    }

    private void deleteProjectAttachmentFile(Project project, boolean notificationAttachment) throws IOException {
        String relPath = notificationAttachment
                ? project.getSummaryNotificationSignedRelPath()
                : project.getSummaryThirdPartyFullRelPath();
        if (relPath == null || relPath.isBlank()) {
            return;
        }
        Path absolutePath = Paths.get(uploadDir).resolve(relPath).normalize();
        Files.deleteIfExists(absolutePath);
    }

    private ProjectDTOs.ProjectList convertToProjectListDTO(Project project) {
        ProjectDTOs.ProjectList dto = new ProjectDTOs.ProjectList();
        dto.setId(project.getId());
        dto.setProjectNumber(project.getProjectNumber());
        dto.setThirdPartyProjectNumber(project.getThirdPartyProjectNumber());
        dto.setThirdPartyName(project.getThirdPartyName());
        dto.setProjectName(project.getProjectName());
        dto.setProjectType(project.getProjectType());
        dto.setCustomer(project.getCustomer());
        dto.setCustomerContact(project.getCustomerContact());
        dto.setPowerPlantId(project.getPowerPlantId());
        dto.setUnitId(project.getUnitId());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setStatus(project.getStatus());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUserId(project.getUserId());
        dto.setResponsiblePerson(project.getResponsiblePerson());
        dto.setReviewerNdt(project.getReviewerNdt());
        dto.setReviewDateNdt(project.getReviewDateNdt());
        dto.setApproverNdt(project.getApproverNdt());
        dto.setApprovalDateNdt(project.getApprovalDateNdt());
        dto.setWriterNdt(project.getWriterNdt());
        dto.setWriterDateNdt(project.getWriterDateNdt());
        dto.setReviewerChem(project.getReviewerChem());
        dto.setReviewDateChem(project.getReviewDateChem());
        dto.setApproverChem(project.getApproverChem());
        dto.setApprovalDateChem(project.getApprovalDateChem());
        dto.setWriterChem(project.getWriterChem());
        dto.setWriterDateChem(project.getWriterDateChem());
        dto.setApprovalStepNdt(project.getApprovalStepNdt() != null ? project.getApprovalStepNdt() : 0);
        dto.setApprovalStepChem(project.getApprovalStepChem() != null ? project.getApprovalStepChem() : 0);
        dto.setRejectionStepNdt(project.getRejectionStepNdt());
        dto.setRejectionStepChem(project.getRejectionStepChem());
        dto.setStaff(project.getStaff());
        dto.setNdtSignatureLevels(project.getNdtSignatureLevels());
        dto.setThirdPartyApprovalByExperimentType(project.getThirdPartyApprovalByExperimentType());

        // 设置用户全名（如果已加载）
        dto.setUserFullName(project.getUser() != null && project.getUser().getFullName() != null
                ? project.getUser().getFullName() : "未知用户");

        // 设置报告数量（如果已加载）
        dto.setReportCount(project.getReports() != null ? project.getReports().size() : 0);

        return dto;
    }

    private String validateNdtSignatureLevels(String writerNdt,
                                              String reviewerNdt,
                                              Map<String, Map<String, String>> ndtSignatureLevels,
                                              List<Integer> selectedExperimentTypeIds) {
        if ((writerNdt == null || writerNdt.isBlank()) && (reviewerNdt == null || reviewerNdt.isBlank())) {
            return null;
        }
        if (selectedExperimentTypeIds == null || selectedExperimentTypeIds.isEmpty()) {
            return null;
        }

        Set<String> ndtCodes = experimentTypeRepository.findAllById(selectedExperimentTypeIds).stream()
                .map(ExperimentType::getCode)
                .filter(Objects::nonNull)
                .map(ndtQualificationRegistry::normalizeMethodCode)
                .filter(ndtQualificationRegistry::supportsMethod)
                .collect(Collectors.toSet());
        if (ndtCodes.isEmpty()) {
            return null;
        }

        String writerName = writerNdt != null ? writerNdt.trim() : "";
        String reviewerName = reviewerNdt != null ? reviewerNdt.trim() : "";

        for (String methodCode : ndtCodes) {
            String methodLabel = ndtQualificationRegistry.methodLabel(methodCode);

            if (!writerName.isEmpty()) {
                String writerLevel = getSignatureLevel(ndtSignatureLevels, "writer", methodCode);
                if (writerLevel == null) {
                    return methodLabel + "编制级别未设置";
                }
                if (!ndtQualificationRegistry.isQualified(methodCode, writerLevel, writerName)) {
                    return methodLabel + "编制人资质与所选级别不符";
                }
            }

            if (!reviewerName.isEmpty()) {
                String reviewerLevel = getSignatureLevel(ndtSignatureLevels, "reviewer", methodCode);
                if (reviewerLevel == null) {
                    return methodLabel + "审核级别未设置";
                }
                if (!ndtQualificationRegistry.isQualified(methodCode, reviewerLevel, reviewerName)) {
                    return methodLabel + "审核人资质与所选级别不符";
                }
            }
        }
        return null;
    }

    private String getSignatureLevel(Map<String, Map<String, String>> ndtSignatureLevels, String role, String methodCode) {
        if (ndtSignatureLevels == null) {
            return null;
        }
        Map<String, String> roleMap = ndtSignatureLevels.get(role);
        if (roleMap == null) {
            return null;
        }
        String level = roleMap.get(methodCode);
        return ndtQualificationRegistry.normalizeLevel(level);
    }

    private List<Integer> parseSelectedExperimentTypeIds(String selectedExperimentTypeIdsJson) {
        if (selectedExperimentTypeIdsJson == null || selectedExperimentTypeIdsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(selectedExperimentTypeIdsJson, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse selected experiment type ids: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private ProjectDTOs.ProjectDetail convertToProjectDetailDTO(Project project) {
        ProjectDTOs.ProjectDetail dto = new ProjectDTOs.ProjectDetail();
        dto.setId(project.getId());
        dto.setProjectNumber(project.getProjectNumber());
        dto.setThirdPartyProjectNumber(project.getThirdPartyProjectNumber());
        dto.setThirdPartyName(project.getThirdPartyName());
        dto.setProjectName(project.getProjectName());
        dto.setProjectType(project.getProjectType());
        dto.setCustomer(project.getCustomer());
        dto.setCustomerContact(project.getCustomerContact());
        dto.setPowerPlantId(project.getPowerPlantId());
        dto.setUnitId(project.getUnitId());
        // 查询机组编号
        if (project.getUnitId() != null) {
            Unit unit = unitRepository.findById(project.getUnitId()).orElse(null);
            if (unit != null) {
                dto.setUnitNumber(unit.getUnitNumber());
            }
        }
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setStatus(project.getStatus());
        dto.setDescription(project.getDescription());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        dto.setUserId(project.getUserId());
        dto.setResponsiblePerson(project.getResponsiblePerson());
        dto.setReviewerNdt(project.getReviewerNdt());
        dto.setReviewDateNdt(project.getReviewDateNdt());
        dto.setApproverNdt(project.getApproverNdt());
        dto.setApprovalDateNdt(project.getApprovalDateNdt());
        dto.setWriterNdt(project.getWriterNdt());
        dto.setWriterDateNdt(project.getWriterDateNdt());
        dto.setReviewerChem(project.getReviewerChem());
        dto.setReviewDateChem(project.getReviewDateChem());
        dto.setApproverChem(project.getApproverChem());
        dto.setApprovalDateChem(project.getApprovalDateChem());
        dto.setWriterChem(project.getWriterChem());
        dto.setWriterDateChem(project.getWriterDateChem());
        dto.setApprovalStepNdt(project.getApprovalStepNdt() != null ? project.getApprovalStepNdt() : 0);
        dto.setApprovalStepChem(project.getApprovalStepChem() != null ? project.getApprovalStepChem() : 0);
        dto.setRejectionStepNdt(project.getRejectionStepNdt());
        dto.setRejectionStepChem(project.getRejectionStepChem());
        dto.setStaff(project.getStaff());
        dto.setNdtSignatureLevels(project.getNdtSignatureLevels());
        dto.setThirdPartyApprovalByExperimentType(project.getThirdPartyApprovalByExperimentType());
        dto.setSummaryNotificationSignedRelPath(project.getSummaryNotificationSignedRelPath());
        dto.setSummaryNotificationSignedOriginalName(project.getSummaryNotificationSignedOriginalName());
        dto.setSummaryThirdPartyFullRelPath(project.getSummaryThirdPartyFullRelPath());
        dto.setSummaryThirdPartyFullOriginalName(project.getSummaryThirdPartyFullOriginalName());
        dto.setReportFigures(projectImageAttachmentRepository.findByProjectIdOrderByDisplayOrder(project.getId())
                .stream()
                .map(this::convertProjectImageAttachmentToDTO)
                .collect(Collectors.toList()));
        dto.setAggregateDetectionLogOrder(project.getAggregateDetectionLogOrder());

        // 设置用户全名（如果已加载）
        dto.setUserFullName(project.getUser() != null && project.getUser().getFullName() != null
                ? project.getUser().getFullName() : "未知用户");

        // 解析选中的检测类型ID列表
        if (project.getSelectedExperimentTypeIds() != null && !project.getSelectedExperimentTypeIds().trim().isEmpty()) {
            try {
                List<Integer> selectedIds = objectMapper.readValue(
                        project.getSelectedExperimentTypeIds(),
                        new TypeReference<List<Integer>>() {}
                );
                dto.setSelectedExperimentTypeIds(selectedIds);
            } catch (Exception e) {
                log.warn("Failed to parse selected experiment type IDs for project {}: {}", project.getId(), e.getMessage());
                dto.setSelectedExperimentTypeIds(new ArrayList<>());
            }
        } else {
            dto.setSelectedExperimentTypeIds(new ArrayList<>());
        }

        // 转换报告列表（按报告编号升序，与概述顺序一致）
        if (project.getReports() != null) {
            Map<Integer, ExperimentType> experimentTypeMap = new HashMap<>();
            for (Report r : project.getReports()) {
                if (r.getExperimentTypeId() != null) {
                    experimentTypeMap.putIfAbsent(r.getExperimentTypeId(), null);
                }
            }
            if (!experimentTypeMap.isEmpty()) {
                for (ExperimentType et : experimentTypeRepository.findAllById(new ArrayList<>(experimentTypeMap.keySet()))) {
                    experimentTypeMap.put(et.getId(), et);
                }
            }
            List<ReportDTOs.ReportList> reportList = project.getReports().stream()
                    .sorted(Comparator.comparing(Report::getReportNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(r -> convertToReportListDTO(r,
                            r.getExperimentTypeId() != null ? experimentTypeMap.get(r.getExperimentTypeId()) : null))
                    .collect(Collectors.toList());
            dto.setReports(reportList);
        } else {
            dto.setReports(new ArrayList<>());
        }

        return dto;
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
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        dto.setUserFullName(report.getUser() != null && report.getUser().getFullName() != null
                ? report.getUser().getFullName() : "未知用户");
        dto.setItemCount(report.getReportItems() != null ? report.getReportItems().size() : 0);
        dto.setExperimentTypeId(report.getExperimentTypeId());
        dto.setExperimentTypeName(experimentType != null ? experimentType.getName() : null);
        dto.setDetectionContentNarrative(detectionContentNarrativeService.getEffectiveNarrativeBody(report, experimentType));
        return dto;
    }
}
