package com.reportweb.repository;

import com.reportweb.entity.ProjectReportChangeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectReportChangeLogRepository extends JpaRepository<ProjectReportChangeLog, Long> {

    @Query("SELECT l FROM ProjectReportChangeLog l WHERE l.projectId = :projectId ORDER BY l.createdAt DESC, l.id DESC")
    List<ProjectReportChangeLog> findByProjectIdOrderByCreatedAtDesc(
            @Param("projectId") Integer projectId,
            Pageable pageable);

    @Query("SELECT l FROM ProjectReportChangeLog l WHERE l.projectId = :projectId ORDER BY l.createdAt DESC, l.id DESC")
    List<ProjectReportChangeLog> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Integer projectId);

    @Query(value = """
            SELECT experiment_type_id AS experimentTypeId,
                   experiment_type_name AS experimentTypeName,
                   experiment_type_code AS experimentTypeCode,
                   SUM(CASE WHEN action = 'CREATED' THEN 1 ELSE 0 END) AS createdCount,
                   SUM(CASE WHEN action = 'UPDATED' THEN 1 ELSE 0 END) AS updatedCount,
                   SUM(CASE WHEN action = 'DELETED' THEN 1 ELSE 0 END) AS deletedCount
            FROM project_report_change_log
            WHERE project_id = :projectId
            GROUP BY experiment_type_id, experiment_type_name, experiment_type_code
            ORDER BY experiment_type_name
            """, nativeQuery = true)
    List<ReportChangeSummaryProjection> aggregateByExperimentType(@Param("projectId") Integer projectId);

    interface ReportChangeSummaryProjection {
        Integer getExperimentTypeId();

        String getExperimentTypeName();

        String getExperimentTypeCode();

        Long getCreatedCount();

        Long getUpdatedCount();

        Long getDeletedCount();
    }
}
