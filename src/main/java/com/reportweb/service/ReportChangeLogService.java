package com.reportweb.service;

import com.reportweb.entity.ExperimentType;
import com.reportweb.entity.ProjectReportChangeLog;
import com.reportweb.entity.Report;
import com.reportweb.entity.User;
import com.reportweb.repository.ExperimentTypeRepository;
import com.reportweb.repository.ProjectReportChangeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportChangeLogService {

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_DELETED = "DELETED";

    public static final String SOURCE_USER_SAVE = "USER_SAVE";
    public static final String SOURCE_BATCH_DELETE = "BATCH_DELETE";
    public static final String SOURCE_BATCH_STATUS = "BATCH_STATUS";
    public static final String SOURCE_PROJECT_DELETE = "PROJECT_DELETE";

    private final ProjectReportChangeLogRepository changeLogRepository;
    private final ExperimentTypeRepository experimentTypeRepository;

    @Transactional
    public void recordCreated(Report report, User operator, String source) {
        if (report == null || report.getId() == null) {
            return;
        }
        changeLogRepository.save(buildEntry(report, operator, source, ACTION_CREATED, null));
    }

    @Transactional
    public void recordUpdated(Report before, Report after, User operator, String source) {
        if (after == null || after.getId() == null) {
            return;
        }
        Map<String, Object> summary = before != null ? diffMetadata(before, after) : null;
        changeLogRepository.save(buildEntry(after, operator, source, ACTION_UPDATED, summary));
    }

    @Transactional
    public void recordDeleted(Report report, User operator, String source) {
        if (report == null || report.getId() == null) {
            return;
        }
        changeLogRepository.save(buildEntry(report, operator, source, ACTION_DELETED, null));
    }

    @Transactional
    public void recordDeletedAll(List<Report> reports, User operator, String source) {
        if (reports == null) {
            return;
        }
        for (Report report : reports) {
            recordDeleted(report, operator, source);
        }
    }

    public ReportMetadataSnapshot captureMetadata(Report report) {
        return ReportMetadataSnapshot.from(report);
    }

    public void recordUpdatedFromSnapshot(ReportMetadataSnapshot before, Report after, User operator, String source) {
        if (after == null || after.getId() == null) {
            return;
        }
        Map<String, Object> summary = before != null ? before.diffTo(after) : null;
        changeLogRepository.save(buildEntry(after, operator, source, ACTION_UPDATED, summary));
    }

    private ProjectReportChangeLog buildEntry(
            Report report,
            User operator,
            String source,
            String action,
            Map<String, Object> changeSummary) {
        ExperimentType experimentType = experimentTypeRepository.findById(report.getExperimentTypeId()).orElse(null);
        String typeName = experimentType != null && experimentType.getName() != null
                ? experimentType.getName()
                : (report.getTestMethod() != null ? report.getTestMethod() : "未知类型");
        String typeCode = experimentType != null && experimentType.getCode() != null
                ? experimentType.getCode().trim()
                : "UNK";

        ProjectReportChangeLog entry = new ProjectReportChangeLog();
        entry.setProjectId(report.getProjectId());
        entry.setReportId(report.getId());
        entry.setAction(action);
        entry.setExperimentTypeId(report.getExperimentTypeId());
        entry.setExperimentTypeName(typeName);
        entry.setExperimentTypeCode(typeCode);
        entry.setReportNumber(report.getReportNumber());
        entry.setTestMethod(report.getTestMethod());
        entry.setStatus(report.getStatus());
        entry.setChangeSummary(changeSummary);
        if (operator != null) {
            entry.setOperatorUserId(operator.getId());
            entry.setOperatorUserName(operator.getFullName() != null ? operator.getFullName() : operator.getUserName());
        } else {
            entry.setOperatorUserId("system");
            entry.setOperatorUserName("系统");
        }
        entry.setSource(source != null ? source : SOURCE_USER_SAVE);
        return entry;
    }

    private static Map<String, Object> diffMetadata(Report before, Report after) {
        return ReportMetadataSnapshot.from(before).diffTo(after);
    }

    public static final class ReportMetadataSnapshot {
        private final Integer experimentTypeId;
        private final String title;
        private final String reportNumber;
        private final String testMethod;
        private final String status;
        private final String hasDefect;
        private final String inspector;
        private final java.time.LocalDate testDate;
        private final String location;
        private final String componentName;
        private final String equipmentName;
        private final String instrumentModel;
        private final String instrumentNumber;

        private ReportMetadataSnapshot(
                Integer experimentTypeId,
                String title,
                String reportNumber,
                String testMethod,
                String status,
                String hasDefect,
                String inspector,
                java.time.LocalDate testDate,
                String location,
                String componentName,
                String equipmentName,
                String instrumentModel,
                String instrumentNumber) {
            this.experimentTypeId = experimentTypeId;
            this.title = title;
            this.reportNumber = reportNumber;
            this.testMethod = testMethod;
            this.status = status;
            this.hasDefect = hasDefect;
            this.inspector = inspector;
            this.testDate = testDate;
            this.location = location;
            this.componentName = componentName;
            this.equipmentName = equipmentName;
            this.instrumentModel = instrumentModel;
            this.instrumentNumber = instrumentNumber;
        }

        static ReportMetadataSnapshot from(Report report) {
            if (report == null) {
                return null;
            }
            return new ReportMetadataSnapshot(
                    report.getExperimentTypeId(),
                    report.getTitle(),
                    report.getReportNumber(),
                    report.getTestMethod(),
                    report.getStatus(),
                    report.getHasDefect(),
                    report.getInspector(),
                    report.getTestDate(),
                    report.getLocation(),
                    report.getComponentName(),
                    report.getEquipmentName(),
                    report.getInstrumentModel(),
                    report.getInstrumentNumber());
        }

        Map<String, Object> diffTo(Report after) {
            if (after == null) {
                return null;
            }
            List<String> fields = new ArrayList<>();
            Map<String, Object> details = new LinkedHashMap<>();
            compareField(fields, details, "experimentTypeId", experimentTypeId, after.getExperimentTypeId());
            compareField(fields, details, "title", title, after.getTitle());
            compareField(fields, details, "reportNumber", reportNumber, after.getReportNumber());
            compareField(fields, details, "testMethod", testMethod, after.getTestMethod());
            compareField(fields, details, "status", status, after.getStatus());
            compareField(fields, details, "hasDefect", hasDefect, after.getHasDefect());
            compareField(fields, details, "inspector", inspector, after.getInspector());
            compareField(fields, details, "testDate", testDate, after.getTestDate());
            compareField(fields, details, "location", location, after.getLocation());
            compareField(fields, details, "componentName", componentName, after.getComponentName());
            compareField(fields, details, "equipmentName", equipmentName, after.getEquipmentName());
            compareField(fields, details, "instrumentModel", instrumentModel, after.getInstrumentModel());
            compareField(fields, details, "instrumentNumber", instrumentNumber, after.getInstrumentNumber());
            if (fields.isEmpty()) {
                return Map.of("fields", List.of());
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("fields", fields);
            summary.put("details", details);
            return summary;
        }

        private static void compareField(
                List<String> fields,
                Map<String, Object> details,
                String key,
                Object beforeVal,
                Object afterVal) {
            if (!Objects.equals(beforeVal, afterVal)) {
                fields.add(key);
                details.put(key, Map.of("before", stringify(beforeVal), "after", stringify(afterVal)));
            }
        }

        private static String stringify(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }
}
