package com.gradproject.taskmanager.infrastructure.storage;

import com.gradproject.taskmanager.infrastructure.config.properties.StorageProperties;
import com.gradproject.taskmanager.shared.exception.FileStorageException;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    
    public String uploadFile(MultipartFile file, String storagePath) {
        try {
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(storagePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Uploaded file to MinIO: {} ({} bytes)", storagePath, file.getSize());
            return storagePath;

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", storagePath, e);
            throw new FileStorageException("Could not upload file: " + e.getMessage(), e);
        }
    }

    
    public byte[] downloadFile(String storagePath) {
        try {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(storagePath)
                            .build()
            )) {
                byte[] data = stream.readAllBytes();
                log.info("Downloaded file from MinIO: {} ({} bytes)", storagePath, data.length);
                return data;
            }
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", storagePath, e);
            throw new FileStorageException("Could not download file: " + e.getMessage(), e);
        }
    }

    
    public void deleteFile(String storagePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(storagePath)
                            .build()
            );
            log.info("Deleted file from MinIO: {}", storagePath);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", storagePath, e);
            throw new FileStorageException("Could not delete file: " + e.getMessage(), e);
        }
    }

    
    public String generateThumbnail(MultipartFile imageFile, String baseStoragePath) {
        if (imageFile == null || imageFile.getContentType() == null || !imageFile.getContentType().startsWith("image/")) {
            return null;
        }

        try {
            
            BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());
            if (originalImage == null) {
                log.warn("Could not read image for thumbnail generation: {}", baseStoragePath);
                return null;
            }

            
            int thumbWidth = 200;
            int thumbHeight = 200;

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            
            double aspectRatio = (double) originalWidth / originalHeight;
            if (aspectRatio > 1) {
                
                thumbHeight = (int) (thumbWidth / aspectRatio);
            } else {
                
                thumbWidth = (int) (thumbHeight * aspectRatio);
            }

            
            BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            byte[] thumbnailData = baos.toByteArray();

            
            String thumbnailPath = baseStoragePath.replaceAll("\\.[^.]+$", "_thumb.jpg");
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(thumbnailPath)
                            .stream(new ByteArrayInputStream(thumbnailData), thumbnailData.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );

            log.info("Generated thumbnail: {} ({} bytes)", thumbnailPath, thumbnailData.length);
            return thumbnailPath;

        } catch (Exception e) {
            log.warn("Could not generate thumbnail for {}: {}", baseStoragePath, e.getMessage());
            return null;  
        }
    }

    
    public boolean fileExists(String storagePath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(storagePath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    
    public long getFileSize(String storagePath) {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucketName())
                            .object(storagePath)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            log.warn("Could not get file size for {}: {}", storagePath, e.getMessage());
            return 0;
        }
    }

    
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.bucketName())
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.bucketName())
                                .build()
                );
                log.info("Created MinIO bucket: {}", properties.bucketName());
            }
        } catch (Exception e) {
            log.error("Error checking/creating bucket: {}", properties.bucketName(), e);
            throw new FileStorageException("Could not ensure bucket exists: " + e.getMessage(), e);
        }
    }
}
