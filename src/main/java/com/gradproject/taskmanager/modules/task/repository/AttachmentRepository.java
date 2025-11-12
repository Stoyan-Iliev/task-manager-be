package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    
    List<Attachment> findByTaskIdOrderByUploadedAtDesc(Long taskId);

    
    List<Attachment> findByUploadedByIdOrderByUploadedAtDesc(Integer userId);

    
    @Query("SELECT a FROM Attachment a WHERE a.task.id = :taskId AND a.mimeType LIKE 'image/%' ORDER BY a.uploadedAt DESC")
    List<Attachment> findImagesByTaskId(@Param("taskId") Long taskId);

    
    long countByTaskId(Long taskId);

    
    @Query("SELECT COALESCE(SUM(a.fileSizeBytes), 0) FROM Attachment a WHERE a.task.id = :taskId")
    Long sumFileSizeByTaskId(@Param("taskId") Long taskId);

    
    @Query("""
        SELECT COALESCE(SUM(a.fileSizeBytes), 0)
        FROM Attachment a
        WHERE a.task.organization.id = :organizationId
        """)
    Long sumFileSizeByOrganizationId(@Param("organizationId") Long organizationId);

    
    Attachment findByStoragePath(String storagePath);

    
    void deleteByTaskId(Long taskId);
}
