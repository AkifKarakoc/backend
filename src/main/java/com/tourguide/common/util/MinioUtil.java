package com.tourguide.common.util;

import com.tourguide.common.exception.FileValidationException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String upload(String bucket, MultipartFile file) {
        validateFile(file);

        String fileName = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded file {} to bucket {}", fileName, bucket);
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public void delete(String bucket, String fileNameOrUrl) {
        try {
            // Extract file name from URL if a full URL is provided
            String objectName = fileNameOrUrl;
            if (fileNameOrUrl.contains("/")) {
                objectName = fileNameOrUrl.substring(fileNameOrUrl.lastIndexOf('/') + 1);
                // Remove query parameters if present (presigned URLs have ?signature=...)
                if (objectName.contains("?")) {
                    objectName = objectName.substring(0, objectName.indexOf('?'));
                }
            }
            
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            log.info("Deleted file {} from bucket {}", objectName, bucket);
        } catch (Exception e) {
            log.warn("Failed to delete file {} from bucket {}: {}", fileNameOrUrl, bucket, e.getMessage());
        }
    }

    public String getPresignedUrl(String bucket, String fileName) {
        try {
            // MinIO allows maximum 7 days for presigned URLs
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(fileName)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}: {}", bucket, fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("File size exceeds maximum allowed size of 10MB");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new FileValidationException("File type not allowed. Allowed types: JPEG, PNG, WebP");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
