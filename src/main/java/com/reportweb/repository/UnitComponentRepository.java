package com.reportweb.repository;

import com.reportweb.entity.UnitComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitComponentRepository extends JpaRepository<UnitComponent, Integer> {
    
    /**
     * 根据机组ID查找所有部件
     */
    List<UnitComponent> findByUnitId(Integer unitId);
    
    /**
     * 根据机组ID和部件名称查找部件
     */
    Optional<UnitComponent> findByUnitIdAndComponentName(Integer unitId, String componentName);
    
    /**
     * 检查机组中是否存在指定名称的部件
     */
    boolean existsByUnitIdAndComponentName(Integer unitId, String componentName);
    
    /**
     * 检查机组中是否存在指定名称的部件（排除指定ID）
     */
    boolean existsByUnitIdAndComponentNameAndIdNot(Integer unitId, String componentName, Integer id);
}
