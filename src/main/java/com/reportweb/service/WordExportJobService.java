package com.reportweb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportweb.entity.Project;
import com.reportweb.entity.Report;
import com.reportweb.entity.WordExportJob;
import com.reportweb.entity.WordExportJobStatus;
import com.reportweb.entity.WordExportJobType;
import com.reportweb.repository.ProjectRepository;
import com.reportweb.repository.WordExportJobRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordExportJobService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WordExportJobRepository wordExportJobRepository;
    private final ProjectRepository projectRepository;
    private final WordGeneratorService wordGeneratorService;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional
    public WordExportJob createJob(Integer projectId, String creatorUserId, WordExportJobType type, List<Integer> reportIds, String projectNumber) {
        List<WordExportJob> activeJobs = wordExportJobRepository.findByProjectIdAndTypeAndStatusInOrderByCreatedAtDesc(
                projectId,
                type,
                List.of(WordExportJobStatus.PENDING, WordExportJobStatus.RUNNING)
        );
        if (!activeJobs.isEmpty()) {
            throw new IllegalArgumentException("正常生成，请勿重复点击");
        }

        WordExportJob job = new WordExportJob();
        job.setId(UUID.randomUUID().toString());
        job.setProjectId(projectId);
        job.setCreatorUserId(creatorUserId);
        job.setType(type);
        job.setStatus(WordExportJobStatus.PENDING);
        job.setSuggestedFileName(buildSuggestedFileName(projectNumber, type));
        job.setPayload(serializePayload(reportIds));
        return wordExportJobRepository.save(job);
    }

    private String serializePayload(List<Integer> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return null;
        }
        List<Integer> sorted = reportIds.stream().sorted().toList();
        try {
            return objectMapper.writeValueAsString(sorted);
        } catch (Exception ex) {
            throw new IllegalArgumentException("任务参数序列化失败", ex);
        }
    }

    @Transactional
    public void executeJob(String jobId) {
        WordExportJob job = wordExportJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("导出任务不存在"));
        if (job.getStatus() != WordExportJobStatus.PENDING) {
            return;
        }
        job.setStatus(WordExportJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        wordExportJobRepository.save(job);

        try {
            Project project = projectRepository.findById(job.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("项目不存在或已删除"));
            initializeProjectForWordGeneration(project);

            byte[] wordBytes = switch (job.getType()) {
                case SUMMARY -> wordGeneratorService.generateProjectSummaryAsync(project);
                case THIRD_PARTY -> wordGeneratorService.generateThirdPartyOnlyProjectWordAsync(project);
                case DETECTION_NOTIFICATION ->
                        wordGeneratorService.generateTechnicalSupervisionNotifications(project, parseReportIds(job.getPayload()));
            };

            String relPath = writeOutputFile(job.getId(), wordBytes);
            removePreviousSucceededOutput(project.getId(), job.getType(), job.getId());
            job.setOutputRelPath(relPath);
            job.setStatus(WordExportJobStatus.SUCCEEDED);
            job.setErrorMessage(null);
            job.setFinishedAt(LocalDateTime.now());
            wordExportJobRepository.save(job);
        } catch (Exception ex) {
            log.error("Failed to execute word export job {}", jobId, ex);
            job.setStatus(WordExportJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "导出失败");
            job.setFinishedAt(LocalDateTime.now());
            wordExportJobRepository.save(job);
        }
    }

    private List<Integer> parseReportIds(String payload) {
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<Integer>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("检测通知单参数解析失败", ex);
        }
    }

    private String writeOutputFile(String jobId, byte[] wordBytes) throws IOException {
        Path baseDir = Paths.get(uploadDir).resolve("word-export-jobs");
        Files.createDirectories(baseDir);
        String fileName = jobId + ".docx";
        Path output = baseDir.resolve(fileName);
        Files.write(output, wordBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return Paths.get("word-export-jobs").resolve(fileName).toString().replace('\\', '/');
    }

    private void removePreviousSucceededOutput(Integer projectId, WordExportJobType type, String currentJobId) {
        List<WordExportJob> succeededJobs = wordExportJobRepository.findLatestSucceededByProjectIdAndType(projectId, type);
        for (WordExportJob prev : succeededJobs) {
            if (prev.getId().equals(currentJobId)) {
                continue;
            }
            if (prev.getOutputRelPath() != null && !prev.getOutputRelPath().isBlank()) {
                try {
                    Path oldPath = Paths.get(uploadDir).resolve(prev.getOutputRelPath()).normalize();
                    Files.deleteIfExists(oldPath);
                } catch (IOException ex) {
                    log.warn("Failed to delete superseded word export file for job {}", prev.getId(), ex);
                }
                prev.setOutputRelPath(null);
                prev.setErrorMessage("已被同项目的新导出任务覆盖");
                wordExportJobRepository.save(prev);
            }
            break;
        }
    }

    private String buildSuggestedFileName(String projectNumber, WordExportJobType type) {
        String number = projectNumber == null || projectNumber.isBlank() ? "项目" : projectNumber;
        String suffix = switch (type) {
            case SUMMARY -> "总报告";
            case THIRD_PARTY -> "第三方报告";
            case DETECTION_NOTIFICATION -> "检测通知单";
        };
        return String.format("%s_%s_%s.docx", number, LocalDateTime.now().format(FILE_TIME_FORMATTER), suffix);
    }

    @Transactional(readOnly = true)
    public WordExportJob getJob(String jobId) {
        return wordExportJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("导出任务不存在"));
    }

    @Transactional(readOnly = true)
    public Optional<WordExportJob> getLatestJob(Integer projectId, WordExportJobType type) {
        return wordExportJobRepository.findFirstByProjectIdAndTypeOrderByCreatedAtDesc(projectId, type);
    }

    public Path resolveOutputPath(WordExportJob job) {
        if (job.getOutputRelPath() == null || job.getOutputRelPath().isBlank()) {
            throw new IllegalArgumentException("导出文件尚未生成");
        }
        Path path = Paths.get(uploadDir).resolve(job.getOutputRelPath()).normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("导出文件不存在或已被清理");
        }
        return path;
    }

    @Transactional
    public int cleanupFinishedJobsBefore(LocalDateTime threshold) {
        List<WordExportJob> expiredJobs = wordExportJobRepository.findFinishedBefore(threshold);
        for (WordExportJob job : expiredJobs) {
            if (job.getOutputRelPath() == null || job.getOutputRelPath().isBlank()) {
                continue;
            }
            try {
                Path outputPath = Paths.get(uploadDir).resolve(job.getOutputRelPath()).normalize();
                Files.deleteIfExists(outputPath);
            } catch (IOException ex) {
                log.warn("Failed to delete output file for expired word export job {}", job.getId(), ex);
            }
        }
        return wordExportJobRepository.deleteFinishedBefore(threshold);
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
    }
}
