package com.reportweb.controller;

import com.reportweb.dto.ImageDTOs;
import com.reportweb.entity.Image;
import com.reportweb.repository.ImageRepository;
import com.reportweb.repository.UserRepository;
import com.reportweb.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImagesController {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUser().getId();

            // 验证文件
            if (file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "只支持图片文件");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 验证文件大小 (10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "文件大小不能超过10MB");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 创建上传目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 保存图片信息到数据库
            Image image = new Image();
            image.setFileName(originalFilename != null ? originalFilename : "unknown");
            image.setStoragePath(filePath.toString());
            image.setFileSize(file.getSize());
            image.setMimeType(contentType);
            image.setUserId(userId);
            image.setUploadedAt(LocalDateTime.now());

            Image savedImage = imageRepository.save(image);

            // 返回响应
            ImageDTOs.ImageUploadResponse response = new ImageDTOs.ImageUploadResponse();
            response.setId(savedImage.getId());
            response.setFileName(savedImage.getFileName());
            response.setUrl("/api/images/" + savedImage.getId());
            response.setFileSize(savedImage.getFileSize());
            response.setMimeType(savedImage.getMimeType());
            response.setUploadedAt(savedImage.getUploadedAt());

            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            log.error("Error uploading image", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "文件上传失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception ex) {
            log.error("Error uploading image", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "文件上传失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Integer id) {
        try {
            // 查找图片
            Image image = imageRepository.findById(id).orElse(null);
            if (image == null) {
                log.warn("Image not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            // 返回图片文件
            Path filePath = Paths.get(image.getStoragePath());
            if (!Files.exists(filePath)) {
                log.error("Image file not found: {}", image.getStoragePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .body(resource);
        } catch (Exception ex) {
            log.error("Error getting image with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ImageDTOs.ImageInfo>> getUserImages(Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUser().getId();

            List<Image> images = imageRepository.findByUserIdOrderByUploadedAtDesc(userId);
            
            List<ImageDTOs.ImageInfo> imageList = images.stream()
                .map(this::convertToImageInfoDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(imageList);
        } catch (Exception ex) {
            log.error("Error getting user images", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Integer id, Authentication authentication) {
        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            String userId = userPrincipal.getUser().getId();

            Image image = imageRepository.findByIdAndUserId(id, userId)
                .orElse(null);

            if (image == null) {
                return ResponseEntity.notFound().build();
            }

            // 删除文件
            try {
                Path filePath = Paths.get(image.getStoragePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException ex) {
                log.warn("Error deleting image file: {}", image.getStoragePath(), ex);
            }

            // 删除数据库记录
            imageRepository.delete(image);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting image with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除图片失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private ImageDTOs.ImageInfo convertToImageInfoDTO(Image image) {
        ImageDTOs.ImageInfo dto = new ImageDTOs.ImageInfo();
        dto.setId(image.getId());
        dto.setFileName(image.getFileName());
        dto.setStoragePath(image.getStoragePath());
        dto.setFileSize(image.getFileSize());
        dto.setMimeType(image.getMimeType());
        dto.setUserId(image.getUserId());
        dto.setUploadedAt(image.getUploadedAt());
        return dto;
    }

}


