package com.reportweb.repository;

import com.reportweb.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {
    
    Optional<Instrument> findByInstrumentName(String instrumentName);
    
    Optional<Instrument> findByInstrumentNumber(String instrumentNumber);
    
    boolean existsByInstrumentName(String instrumentName);
    
    boolean existsByInstrumentNumber(String instrumentNumber);
    
    List<Instrument> findByInstrumentNameContainingIgnoreCase(String name);
    
    List<Instrument> findByInstrumentModelContainingIgnoreCase(String model);
    
    Optional<Instrument> findByInstrumentNameAndInstrumentModelAndInstrumentNumber(
        String instrumentName, String instrumentModel, String instrumentNumber);
}
