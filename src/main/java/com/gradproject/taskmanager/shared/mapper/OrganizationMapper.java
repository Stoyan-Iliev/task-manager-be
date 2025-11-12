package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.domain.OrganizationMember;
import com.gradproject.taskmanager.modules.organization.dto.MemberResponse;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationRequest;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationResponse;
import org.mapstruct.*;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrganizationMapper {

    
    @Mapping(target = "memberCount", expression = "java(organization.getMembers() != null ? (long) organization.getMembers().size() : 0L)")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    OrganizationResponse toResponse(Organization organization);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "members", ignore = true)
    Organization toEntity(OrganizationRequest request);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "settings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "members", ignore = true)
    void updateEntityFromRequest(OrganizationRequest request, @MappingTarget Organization organization);

    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "invitedByUsername", source = "invitedBy.username")
    MemberResponse toMemberResponse(OrganizationMember member);
}
