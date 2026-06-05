package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "material_library_entry")
@Data
public class MaterialLibraryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "material_key", length = 100, nullable = false)
    private String materialKey;

    @Column(name = "primary_category", length = 32, nullable = false)
    private String primaryCategory;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "source", length = 20, nullable = false)
    private String source = "USER";

    @Column(name = "modification_type", length = 20, nullable = false)
    private String modificationType = "CREATE";

    @Column(name = "properties", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> properties = new HashMap<>();

    @Column(name = "approved_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> approvedSnapshot;

    @Column(name = "submitted_by_user_id", length = 450)
    private String submittedByUserId;

    @Column(name = "submitted_by_user_name", length = 200)
    private String submittedByUserName;

    @Column(name = "reviewed_by_user_id", length = 450)
    private String reviewedByUserId;

    @Column(name = "reviewed_by_user_name", length = 200)
    private String reviewedByUserName;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
