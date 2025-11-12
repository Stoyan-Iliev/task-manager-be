package com.gradproject.taskmanager.modules.task.repository;

import com.gradproject.taskmanager.modules.task.domain.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    
    List<Label> findByOrganizationIdOrderByNameAsc(Long organizationId);

    
    Optional<Label> findByOrganizationIdAndName(Long organizationId, String name);

    
    boolean existsByOrganizationIdAndName(Long organizationId, String name);

    
    long countByOrganizationId(Long organizationId);
}
