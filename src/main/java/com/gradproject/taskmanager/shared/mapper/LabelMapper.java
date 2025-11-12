package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.task.domain.Label;
import com.gradproject.taskmanager.modules.task.dto.LabelRequest;
import com.gradproject.taskmanager.modules.task.dto.LabelResponse;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface LabelMapper {

    
    @Mapping(source = "organization.id", target = "organizationId")
    LabelResponse toResponse(Label label);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Label fromRequest(LabelRequest request);

    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateFromRequest(LabelRequest request, @MappingTarget Label label);
}
