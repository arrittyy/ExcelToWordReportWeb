package com.reportweb.repository;

import com.reportweb.entity.MaterialLibraryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialLibraryEntryRepository extends JpaRepository<MaterialLibraryEntry, Long> {

    List<MaterialLibraryEntry> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT e FROM MaterialLibraryEntry e WHERE LOWER(e.materialKey) = LOWER(:materialKey)")
    Optional<MaterialLibraryEntry> findByMaterialKeyIgnoreCase(@Param("materialKey") String materialKey);

    @Query("SELECT e FROM MaterialLibraryEntry e WHERE e.status = 'APPROVED' ORDER BY e.materialKey ASC")
    List<MaterialLibraryEntry> findAllApproved();

    @Query("""
            SELECT e FROM MaterialLibraryEntry e
            WHERE e.status = 'APPROVED'
               OR (e.status = 'PENDING' AND e.modificationType IN ('UPDATE', 'DELETE'))
            ORDER BY e.materialKey ASC
            """)
    List<MaterialLibraryEntry> findAllForLibraryList();

    @Query("""
            SELECT e FROM MaterialLibraryEntry e
            WHERE e.status = 'APPROVED'
               OR (e.status = 'PENDING' AND e.modificationType IN ('UPDATE', 'DELETE') AND e.approvedSnapshot IS NOT NULL)
            """)
    List<MaterialLibraryEntry> findAllEffectiveForCache();

    long countByStatus(String status);

    long countBySource(String source);

    long countByStatusAndSubmittedByUserId(String status, String submittedByUserId);

    @Query("""
            SELECT COUNT(e) FROM MaterialLibraryEntry e
            WHERE e.submittedByUserId = :userId
              AND (
                  e.status = 'REJECTED'
                  OR (e.status = 'APPROVED' AND e.reviewComment IS NOT NULL AND TRIM(e.reviewComment) <> '')
              )
            """)
    long countRejectedSubmissionsByUser(@Param("userId") String userId);

    List<MaterialLibraryEntry> findBySubmittedByUserIdOrderByCreatedAtDesc(String submittedByUserId);

    @Query("SELECT COUNT(e) > 0 FROM MaterialLibraryEntry e WHERE LOWER(e.materialKey) = LOWER(:materialKey) AND e.status = 'PENDING'")
    boolean existsPendingByMaterialKeyIgnoreCase(@Param("materialKey") String materialKey);
}
