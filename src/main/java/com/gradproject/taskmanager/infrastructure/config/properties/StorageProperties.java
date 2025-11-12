package com.gradproject.taskmanager.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;


@ConfigurationProperties(prefix = "storage.s3")
@Validated
public record StorageProperties(
        @NotBlank(message = "S3 endpoint URL is required")
        String endpoint,

        @NotBlank(message = "S3 access key is required")
        String accessKey,

        @NotBlank(message = "S3 secret key is required")
        String secretKey,

        @NotBlank(message = "S3 bucket name is required")
        String bucketName,

        String region,

        @NotNull(message = "Max file size must be specified")
        Long maxFileSizeBytes,

        @NotNull(message = "Allowed MIME types must be specified")
        List<String> allowedMimeTypes
) {
    
    public StorageProperties {
        
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        if (maxFileSizeBytes == null) {
            maxFileSizeBytes = 50L * 1024 * 1024;  
        }
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            allowedMimeTypes = List.of(
                    
                    "image/jpeg",
                    "image/png",
                    "image/gif",
                    "image/webp",
                    "image/svg+xml",
                    
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    
                    "text/plain",
                    "text/csv",
                    "text/markdown",
                    
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/x-tar",
                    "application/gzip",
                    
                    "application/json",
                    "application/xml",
                    "text/xml"
            );
        }
    }

    
    public boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && allowedMimeTypes.contains(mimeType.toLowerCase());
    }

    
    public long getMaxFileSizeMB() {
        return maxFileSizeBytes / (1024 * 1024);
    }
}
