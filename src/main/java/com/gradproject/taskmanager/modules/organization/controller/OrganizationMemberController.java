package com.gradproject.taskmanager.modules.organization.controller;

import com.gradproject.taskmanager.modules.organization.dto.MemberInviteRequest;
import com.gradproject.taskmanager.modules.organization.dto.MemberResponse;
import com.gradproject.taskmanager.modules.organization.dto.UpdateMemberRoleRequest;
import com.gradproject.taskmanager.modules.organization.service.OrganizationMemberService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure/organizations/{organizationId}/members")
@RequiredArgsConstructor
public class OrganizationMemberController {

    private final OrganizationMemberService memberService;

    
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @PathVariable Long organizationId,
            @Valid @RequestBody MemberInviteRequest request) {
        Integer invitedBy = SecurityUtils.getCurrentUserId();
        MemberResponse response = memberService.addMember(
                organizationId,
                request.email(),
                request.role(),
                invitedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> listMembers(@PathVariable Long organizationId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<MemberResponse> members = memberService.listMembers(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @PathVariable Long organizationId,
            @PathVariable Integer userId) {
        Integer requestingUser = SecurityUtils.getCurrentUserId();
        MemberResponse response = memberService.getMember(organizationId, userId, requestingUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMemberRole(
            @PathVariable Long organizationId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        Integer updatedBy = SecurityUtils.getCurrentUserId();
        MemberResponse response = memberService.updateMemberRole(
                organizationId,
                userId,
                request.role(),
                updatedBy
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long organizationId,
            @PathVariable Integer userId) {
        Integer removedBy = SecurityUtils.getCurrentUserId();
        memberService.removeMember(organizationId, userId, removedBy);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
