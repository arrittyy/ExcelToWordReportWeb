package com.reportweb.repository;

import com.reportweb.entity.ReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportItemRepository extends JpaRepository<ReportItem, Integer> {
    
    List<ReportItem> findByReportId(Integer reportId);
    
    List<ReportItem> findByExperimentTypeId(Integer experimentTypeId);
    
    void deleteByReportId(Integer reportId);
}


