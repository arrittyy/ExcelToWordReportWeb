package com.reportweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "word_export_job")
@Data
public class WordExportJob {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "creator_user_id", nullable = false, length = 255)
    private String creatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private WordExportJobType type;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WordExportJobStatus status;

    @Column(name = "output_rel_path", length = 1000)
    private String outputRelPath;

    @Column(name = "suggested_file_name", nullable = false, length = 500)
    private String suggestedFileName;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
