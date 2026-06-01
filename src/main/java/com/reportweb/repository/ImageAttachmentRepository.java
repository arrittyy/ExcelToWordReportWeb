package com.reportweb.repository;

import com.reportweb.entity.ImageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageAttachmentRepository extends JpaRepository<ImageAttachment, Integer> {
    
    /**
     * 根据报告ID查找所有附图，按显示顺序排序
     */
    List<ImageAttachment> findByReportIdOrderByDisplayOrder(Integer reportId);
    
    /**
     * 根据报告ID删除所有附图
     */
    void deleteByReportId(Integer reportId);
}
