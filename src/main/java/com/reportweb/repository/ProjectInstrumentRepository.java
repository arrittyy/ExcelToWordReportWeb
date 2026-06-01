package com.reportweb.repository;

import com.reportweb.entity.ProjectInstrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectInstrumentRepository extends JpaRepository<ProjectInstrument, Integer> {
    
    /**
     * 根据项目ID查找所有仪器设备
     */
    List<ProjectInstrument> findByProjectId(Integer projectId);
    
    /**
     * 根据项目ID和仪器名称查找仪器设备
     */
    Optional<ProjectInstrument> findByProjectIdAndInstrumentName(Integer projectId, String instrumentName);
    
    /**
     * 检查项目中是否存在指定名称的仪器设备
     */
    boolean existsByProjectIdAndInstrumentName(Integer projectId, String instrumentName);
    
    /**
     * 检查项目中是否存在指定名称的仪器设备（排除指定ID）
     */
    boolean existsByProjectIdAndInstrumentNameAndIdNot(Integer projectId, String instrumentName, Integer id);
    
    /**
     * 根据项目ID、检测类型代码和是否默认查找仪器设备
     */
    List<ProjectInstrument> findByProjectIdAndExperimentTypeCodeAndIsDefault(
            Integer projectId, String experimentTypeCode, Boolean isDefault);
}
