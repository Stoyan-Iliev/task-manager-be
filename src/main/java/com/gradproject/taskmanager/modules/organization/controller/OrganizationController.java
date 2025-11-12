package com.gradproject.taskmanager.modules.organization.controller;

import com.gradproject.taskmanager.modules.organization.dto.OrganizationRequest;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationResponse;
import com.gradproject.taskmanager.modules.organization.service.OrganizationService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        OrganizationResponse response = organizationService.createOrganization(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> listMyOrganizations() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<OrganizationResponse> organizations = organizationService.listMyOrganizations(userId);
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        OrganizationResponse response = organizationService.getOrganization(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationBySlug(@PathVariable String slug) {
        Integer userId = SecurityUtils.getCurrentUserId();
        OrganizationResponse response = organizationService.getOrganizationBySlug(slug, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        OrganizationResponse response = organizationService.updateOrganization(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        organizationService.deleteOrganization(id, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
