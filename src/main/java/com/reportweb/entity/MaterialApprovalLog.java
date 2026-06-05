package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_approval_log")
@Data
public class MaterialApprovalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "actor_user_id", length = 450, nullable = false)
    private String actorUserId;

    @Column(name = "actor_user_name", length = 200)
    private String actorUserName;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
