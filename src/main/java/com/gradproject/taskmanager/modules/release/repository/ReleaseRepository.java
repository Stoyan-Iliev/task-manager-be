package com.gradproject.taskmanager.modules.release.repository;

import com.gradproject.taskmanager.modules.release.domain.Release;
import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    
    @Query("SELECT r FROM Release r WHERE r.project.id = :projectId ORDER BY r.releaseDate DESC, r.createdAt DESC")
    List<Release> findByProjectId(@Param("projectId") Long projectId);

    
    @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.status = :status ORDER BY r.releaseDate DESC")
    List<Release> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") ReleaseStatus status);

    
    @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.name = :name")
    Optional<Release> findByProjectIdAndName(@Param("projectId") Long projectId, @Param("name") String name);

    
    @Query("SELECT COUNT(r) > 0 FROM Release r WHERE r.project.id = :projectId AND r.name = :name")
    boolean existsByProjectIdAndName(@Param("projectId") Long projectId, @Param("name") String name);

    
    @Query("SELECT COUNT(r) > 0 FROM Release r WHERE r.project.id = :projectId AND r.name = :name AND r.id != :excludeId")
    boolean existsByProjectIdAndNameExcludingId(@Param("projectId") Long projectId, @Param("name") String name, @Param("excludeId") Long excludeId);
}
