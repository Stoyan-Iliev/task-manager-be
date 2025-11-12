package com.gradproject.taskmanager.infrastructure.config;

import com.gradproject.taskmanager.infrastructure.config.properties.StorageProperties;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@Slf4j
public class MinioConfig {

    
    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        log.info("Configuring MinIO client with endpoint: {}", properties.endpoint());
        log.info("Using bucket: {}", properties.bucketName());
        log.info("Max file size: {} MB", properties.getMaxFileSizeMB());
        log.info("Allowed MIME types: {}", properties.allowedMimeTypes().size());

        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .region(properties.region())
                .build();
    }
}
