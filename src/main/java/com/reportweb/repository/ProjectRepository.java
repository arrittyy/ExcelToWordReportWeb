package com.reportweb.repository;

import com.reportweb.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    
    List<Project> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Project> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

    List<Project> findByStatusOrderByCreatedAtDesc(String status);

    Optional<Project> findByIdAndUserId(Integer id, String userId);
    
    boolean existsByProjectNumber(String projectNumber);
    
    boolean existsByProjectNumberAndIdNot(String projectNumber, Integer id);
    
    @Query("SELECT p FROM Project p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    List<Project> findProjectsByUser(@Param("userId") String userId);
    
    /**
     * 查询所有项目（按创建时间倒序）
     * 供管理员使用
     */
    @Query("SELECT p FROM Project p ORDER BY p.createdAt DESC")
    List<Project> findAllOrderByCreatedAtDesc();
}


