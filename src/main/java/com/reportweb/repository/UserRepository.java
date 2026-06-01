package com.reportweb.repository;

import com.reportweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByUserName(String userName);
    
    Optional<User> findByNormalizedUserName(String normalizedUserName);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByNormalizedEmail(String normalizedEmail);
    
    boolean existsByUserName(String userName);
    
    boolean existsByEmail(String email);
    
    /**
     * 查询所有用户（按创建时间倒序）
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();
    
    /**
     * 查询所有用户（管理员在前，普通用户在后，同角色内按创建时间倒序）
     */
    @Query("SELECT u FROM User u ORDER BY " +
           "CASE WHEN u.role = 'ADMIN' THEN 0 ELSE 1 END, " +
           "u.createdAt DESC")
    List<User> findAllOrderByRoleAndCreatedAtDesc();
    
    /**
     * 统计指定角色的用户数量
     */
    long countByRole(String role);
    
    /**
     * 根据角色查询用户
     */
    List<User> findByRole(String role);
    
    /**
     * 根据角色查询用户（按创建时间倒序）
     */
    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC")
    List<User> findByRoleOrderByCreatedAtDesc(@Param("role") String role);
}


