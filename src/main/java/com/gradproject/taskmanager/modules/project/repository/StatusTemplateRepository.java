package com.gradproject.taskmanager.modules.project.repository;

import com.gradproject.taskmanager.modules.project.domain.StatusTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface StatusTemplateRepository extends JpaRepository<StatusTemplate, Long> {

    
    Optional<StatusTemplate> findByName(String name);

    
    @Query("SELECT st FROM StatusTemplate st ORDER BY st.name ASC")
    List<StatusTemplate> findAllOrderByName();

    
    boolean existsByName(String name);
}
