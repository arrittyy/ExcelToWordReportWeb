package com.reportweb.repository;

import com.reportweb.entity.ExperimentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentTypeRepository extends JpaRepository<ExperimentType, Integer> {
    
    List<ExperimentType> findByIsActiveTrue();
    
    Optional<ExperimentType> findByCode(String code);
    
    boolean existsByCode(String code);
    
    boolean existsByCodeAndIdNot(String code, Integer id);
    
    ExperimentType findByName(String name);
}


