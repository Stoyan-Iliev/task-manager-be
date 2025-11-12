package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    
    @Query("SELECT c FROM Comment c WHERE c.task.id = :taskId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsByTaskId(@Param("taskId") Long taskId);

    
    long countByTaskId(Long taskId);

    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.task.id = :taskId AND c.parentComment IS NULL")
    long countTopLevelCommentsByTaskId(@Param("taskId") Long taskId);

    
    long countByParentCommentId(Long parentCommentId);

    
    List<Comment> findByUserIdOrderByCreatedAtDesc(Integer userId);

    
    void deleteByTaskId(Long taskId);

    
    boolean existsByIdAndUserId(Long commentId, Integer userId);
}
