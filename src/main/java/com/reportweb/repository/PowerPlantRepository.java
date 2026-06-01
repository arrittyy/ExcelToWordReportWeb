package com.reportweb.repository;

import com.reportweb.entity.PowerPlant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PowerPlantRepository extends JpaRepository<PowerPlant, Integer> {
    
    /**
     * 根据名称查找电厂
     */
    Optional<PowerPlant> findByName(String name);
    
    /**
     * 检查是否存在指定名称的电厂
     */
    boolean existsByName(String name);
    
    /**
     * 根据区域查找电厂列表
     */
    List<PowerPlant> findByRegion(String region);
    
    /**
     * 根据省份查找电厂列表
     */
    List<PowerPlant> findByProvince(String province);
    
    /**
     * 根据城市查找电厂列表
     */
    List<PowerPlant> findByCity(String city);
    
    /**
     * 根据区域和省份查找电厂列表
     */
    List<PowerPlant> findByRegionAndProvince(String region, String province);
    
    /**
     * 检查是否存在指定名称的电厂（排除指定ID）
     */
    boolean existsByNameAndIdNot(String name, Integer id);
}
