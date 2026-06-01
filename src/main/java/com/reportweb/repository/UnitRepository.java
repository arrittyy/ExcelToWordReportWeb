package com.reportweb.repository;

import com.reportweb.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Integer> {
    
    /**
     * 鏍规嵁鐢靛巶ID鏌ユ壘鎵€鏈夋満缁?     */
    List<Unit> findByPowerPlantId(Integer powerPlantId);
    
    /**
     * 鏍规嵁鐢靛巶ID鍜屾満缁勫悕绉版煡鎵炬満缁?     */
    Optional<Unit> findByPowerPlantIdAndUnitName(Integer powerPlantId, String unitName);
    
    /**
     * 妫€鏌ョ數鍘備腑鏄惁瀛樺湪鎸囧畾鍚嶇О鐨勬満缁?     */
    boolean existsByPowerPlantIdAndUnitName(Integer powerPlantId, String unitName);
    
    /**
     * 妫€鏌ョ數鍘備腑鏄惁瀛樺湪鎸囧畾鍚嶇О鐨勬満缁勶紙鎺掗櫎鎸囧畾ID锛?     */
    boolean existsByPowerPlantIdAndUnitNameAndIdNot(Integer powerPlantId, String unitName, Integer id);
    
    /**
     * 妫€鏌ョ數鍘備腑鏄惁瀛樺湪鎸囧畾缂栧彿鐨勬満缁?     */
    boolean existsByPowerPlantIdAndUnitNumber(Integer powerPlantId, String unitNumber);
    
    /**
     * 妫€鏌ョ數鍘備腑鏄惁瀛樺湪鎸囧畾缂栧彿鐨勬満缁勶紙鎺掗櫎鎸囧畾ID锛?     */
    boolean existsByPowerPlantIdAndUnitNumberAndIdNot(Integer powerPlantId, String unitNumber, Integer id);
}
