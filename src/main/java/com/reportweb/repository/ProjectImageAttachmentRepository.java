package com.reportweb.repository;

import com.reportweb.entity.ProjectImageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectImageAttachmentRepository extends JpaRepository<ProjectImageAttachment, Integer> {

    List<ProjectImageAttachment> findByProjectIdOrderByDisplayOrder(Integer projectId);

    void deleteByProjectId(Integer projectId);
}
