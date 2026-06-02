package com.reportweb.repository;

import com.reportweb.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    
    List<Report> findByUserIdOrderById(String userId);
    
    List<Report> findByUserIdAndProjectIdOrderById(String userId, Integer projectId);
    
    Optional<Report> findByIdAndUserId(Integer id, String userId);
    
    @Query("SELECT r FROM Report r WHERE r.userId = :userId AND (:projectId IS NULL OR r.projectId = :projectId) ORDER BY r.id")
    List<Report> findByUserIdAndOptionalProjectId(@Param("userId") String userId, @Param("projectId") Integer projectId);
    
    List<Report> findByProjectIdOrderById(Integer projectId);

    @Query("SELECT r.experimentTypeId, COUNT(r) FROM Report r WHERE r.projectId = :projectId GROUP BY r.experimentTypeId")
    List<Object[]> countByProjectIdGroupByExperimentTypeId(@Param("projectId") Integer projectId);
    
    /**
     * 查询所有报告（按ID排序）
     * 供管理员使用
     */
    @Query("SELECT r FROM Report r ORDER BY r.id")
    List<Report> findAllOrderById();
    
    boolean existsByReportNumber(String reportNumber);
    
    @Modifying
    @Query(value = "UPDATE \"Reports\" SET \"Location\" = '/' WHERE \"Location\" IS NULL OR \"Location\" = ''", nativeQuery = true)
    int fixEmptyLocationFields();
    
    @Modifying
    @Query(value = "UPDATE \"Reports\" SET \"Inspector\" = '/' WHERE \"Inspector\" IS NULL OR \"Inspector\" = ''", nativeQuery = true)
    int fixEmptyInspectorFields();
    
    /**
     * 查询所有报告，并关联ExperimentType和ProjectComponent
     * 使用EntityGraph优化查询，避免N+1问题
     */
    @EntityGraph(attributePaths = {"reportItems", "reportItems.experimentType"})
    @Query("SELECT r FROM Report r ORDER BY r.id")
    List<Report> findAllWithRelations();
    
    /**
     * 根据用户ID查询报告，并关联ExperimentType和ProjectComponent
     */
    @EntityGraph(attributePaths = {"reportItems", "reportItems.experimentType"})
    @Query("SELECT r FROM Report r WHERE r.userId = :userId ORDER BY r.id")
    List<Report> findByUserIdWithRelations(@Param("userId") String userId);
    
    /**
     * 根据项目ID查询报告，并关联ExperimentType和ProjectComponent
     */
    @EntityGraph(attributePaths = {"reportItems", "reportItems.experimentType"})
    @Query("SELECT r FROM Report r WHERE r.projectId = :projectId ORDER BY r.id")
    List<Report> findByProjectIdWithRelations(@Param("projectId") Integer projectId);

    /**
     * 按 ID 批量加载报告（合并导出 Word 等），含项目、检测项。
     * 注意：不能与 {@code imageAttachments} 同图一并 fetch（均为 List 时 Hibernate 报 MultipleBagFetchException），附图在只读事务内懒加载即可。
     */
    @EntityGraph(attributePaths = {"reportItems", "reportItems.experimentType", "project"})
    @Query("SELECT r FROM Report r WHERE r.id IN :ids")
    List<Report> findAllByIdWithRelations(@Param("ids") Collection<Integer> ids);

    /**
     * 主账号：所属项目 {@link com.reportweb.entity.Project#getUserId()} 为指定用户的全部报告（含子账号创建）
     */
    @Query("SELECT r FROM Report r JOIN r.project p WHERE p.userId = :ownerUserId ORDER BY r.id")
    List<Report> findByProjectOwnerUserIdOrderById(@Param("ownerUserId") String ownerUserId);

    /**
     * 同上，带 reportItems 关联（summaries 等）
     */
    @EntityGraph(attributePaths = {"reportItems", "reportItems.experimentType"})
    @Query("SELECT r FROM Report r JOIN r.project p WHERE p.userId = :ownerUserId ORDER BY r.id")
    List<Report> findByProjectOwnerUserIdWithRelations(@Param("ownerUserId") String ownerUserId);
}


