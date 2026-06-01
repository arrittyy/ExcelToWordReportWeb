package com.reportweb.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Data
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    /**
     * 关联的报告ID (可空,刚上传的图片可能还未关联报告)
     */
    @Column(name = "report_id")
    private Integer reportId;

    // Navigation properties
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // 暂时注释掉Report关系,避免启动问题
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "report_id", insertable = false, updatable = false)
    // private Report report;

    // Manual getters and setters for critical fields
    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}


