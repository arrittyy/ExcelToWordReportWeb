package com.reportweb.repository;

import com.reportweb.entity.ProjectComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectComponentRepository extends JpaRepository<ProjectComponent, Integer> {
    
    /**
     * 根据项目ID查找所有部件
     */
    List<ProjectComponent> findByProjectId(Integer projectId);
    
    /**
     * 根据项目ID和部件名称查找部件
     */
    Optional<ProjectComponent> findByProjectIdAndComponentName(Integer projectId, String componentName);
    
    /**
     * 检查项目中是否存在指定名称的部件
     */
    boolean existsByProjectIdAndComponentName(Integer projectId, String componentName);
    
    /**
     * 检查项目中是否存在指定名称的部件（排除指定ID）
     */
    boolean existsByProjectIdAndComponentNameAndIdNot(Integer projectId, String componentName, Integer id);
}













