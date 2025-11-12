package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.search.domain.SavedSearch;
import com.gradproject.taskmanager.modules.search.dto.SavedSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface SavedSearchMapper {

    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    @Mapping(target = "organizationId", source = "organization.id")
    SavedSearchResponse toResponse(SavedSearch savedSearch);
}
