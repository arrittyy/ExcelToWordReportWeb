package com.reportweb.repository;

import com.reportweb.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    
    List<Image> findByUserIdOrderByUploadedAtDesc(String userId);
    
    Optional<Image> findByIdAndUserId(Integer id, String userId);
    
    void deleteByUserId(String userId);
}


