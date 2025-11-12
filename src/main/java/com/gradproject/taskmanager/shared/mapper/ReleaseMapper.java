package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.release.domain.Release;
import com.gradproject.taskmanager.modules.release.dto.ReleaseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReleaseMapper {

    
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "taskCount", ignore = true)
    @Mapping(target = "completedTaskCount", ignore = true)
    ReleaseResponse toResponse(Release release);
}
