package com.reportweb.repository;

import com.reportweb.entity.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {

    @Query("SELECT a FROM ApprovalLog a WHERE a.projectId = :projectId ORDER BY a.createdAt DESC")
    List<ApprovalLog> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Integer projectId);
}
