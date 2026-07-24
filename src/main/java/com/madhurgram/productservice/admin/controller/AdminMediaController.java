package com.madhurgram.productservice.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/media")
@Tag(name = "Admin Media Manager", description = "Secure endpoints for admin media uploads")
public class AdminMediaController {

    private static final String UPLOAD_DIR_PATH = "uploads/banners";
    private static final String UPLOAD_SUBPATH = "/uploads/banners/";

    @Value("${madhurgram.app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload marketing banner", description = "Uploads a banner or promotional image securely.")
    public ResponseEntity<Map<String, String>> uploadAdminMedia(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("Admin media upload failed: file is empty");
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            File uploadDir = new File(UPLOAD_DIR_PATH);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File destination = new File(uploadDir.getAbsolutePath(), fileName);
            Files.copy(
                file.getInputStream(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );

            String fileUrl = backendUrl.trim() + UPLOAD_SUBPATH + fileName;
            log.info("Admin media successfully uploaded: '{}'", fileUrl);
            return ResponseEntity.ok(Map.of("url", fileUrl));

        } catch (Exception e) {
            log.error("Failed to upload admin media", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file"));
        }
    }
}
