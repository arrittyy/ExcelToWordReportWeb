package com.reportweb.repository;

import com.reportweb.entity.MaterialApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialApprovalLogRepository extends JpaRepository<MaterialApprovalLog, Long> {

    List<MaterialApprovalLog> findByEntryIdOrderByCreatedAtDesc(Long entryId);
}
