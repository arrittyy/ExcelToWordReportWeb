package com.reportweb.dto;

import lombok.Data;

import java.time.LocalDateTime;

public class ImageDTOs {

    @Data
    public static class ImageInfo {
        private Integer id;
        private String fileName;
        private String storagePath;
        private Long fileSize;
        private String mimeType;
        private String userId;
        private LocalDateTime uploadedAt;
    }

    @Data
    public static class ImageUploadResponse {
        private Integer id;
        private String fileName;
        private String url;
        private Long fileSize;
        private String mimeType;
        private LocalDateTime uploadedAt;
    }
}


