package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_attachments")
@Data
public class ImageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "report_id", nullable = false)
    private Integer reportId;

    @Column(name = "image_urls", columnDefinition = "text", nullable = false)
    private String imageUrls; // JSON格式存储图片URL列表

    @Column(name = "description", length = 500)
    private String description; // 附图描述

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // 显示顺序

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Navigation property
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", insertable = false, updatable = false)
    private Report report;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
