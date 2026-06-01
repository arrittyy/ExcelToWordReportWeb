package com.reportweb.repository;

import com.reportweb.entity.WordExportJob;
import com.reportweb.entity.WordExportJobStatus;
import com.reportweb.entity.WordExportJobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WordExportJobRepository extends JpaRepository<WordExportJob, String> {

    Optional<WordExportJob> findByIdAndProjectId(String id, Integer projectId);
    Optional<WordExportJob> findFirstByProjectIdAndTypeOrderByCreatedAtDesc(Integer projectId, WordExportJobType type);

    @Query("SELECT j FROM WordExportJob j WHERE j.projectId = :projectId AND j.creatorUserId = :creatorUserId ORDER BY j.createdAt DESC")
    List<WordExportJob> findByProjectAndCreatorDesc(@Param("projectId") Integer projectId, @Param("creatorUserId") String creatorUserId);

    @Query("SELECT j FROM WordExportJob j WHERE j.status = :status")
    List<WordExportJob> findByStatus(@Param("status") WordExportJobStatus status);

    @Query("""
            SELECT j FROM WordExportJob j
            WHERE j.projectId = :projectId
              AND j.type = :type
              AND j.status IN :statuses
            ORDER BY j.createdAt DESC
            """)
    List<WordExportJob> findByProjectIdAndTypeAndStatusInOrderByCreatedAtDesc(
            @Param("projectId") Integer projectId,
            @Param("type") com.reportweb.entity.WordExportJobType type,
            @Param("statuses") List<WordExportJobStatus> statuses
    );

    @Query("""
            SELECT j FROM WordExportJob j
            WHERE j.projectId = :projectId
              AND j.type = :type
              AND j.status = com.reportweb.entity.WordExportJobStatus.SUCCEEDED
            ORDER BY j.finishedAt DESC, j.createdAt DESC
            """)
    List<WordExportJob> findLatestSucceededByProjectIdAndType(
            @Param("projectId") Integer projectId,
            @Param("type") com.reportweb.entity.WordExportJobType type
    );

    @Query("SELECT j FROM WordExportJob j WHERE j.finishedAt IS NOT NULL AND j.finishedAt < :threshold")
    List<WordExportJob> findFinishedBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM WordExportJob j WHERE j.finishedAt IS NOT NULL AND j.finishedAt < :threshold")
    int deleteFinishedBefore(@Param("threshold") LocalDateTime threshold);
}
