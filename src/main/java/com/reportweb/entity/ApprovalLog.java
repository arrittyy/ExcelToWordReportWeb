package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_log")
@Data
public class ApprovalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "track", length = 20, nullable = false)
    private String track; // ndt | chem

    @Column(name = "action", length = 20, nullable = false)
    private String action; // submit | pass | reject | rollback

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
