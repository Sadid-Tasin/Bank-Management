package com.bank.bankmanagement.admin.controller;

import com.bank.bankmanagement.admin.dto.StaffCreateRequest;
import com.bank.bankmanagement.admin.dto.StaffResponse;
import com.bank.bankmanagement.admin.dto.StaffRoleUpdateRequest;
import com.bank.bankmanagement.admin.dto.StaffStatusUpdateRequest;
import com.bank.bankmanagement.admin.service.StaffManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final StaffManagementService staffManagementService;

    public AdminStaffController(StaffManagementService staffManagementService) {
        this.staffManagementService = staffManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse createStaff(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody StaffCreateRequest request) {
        return staffManagementService.createStaff(request, jwt.getSubject());
    }

    @GetMapping
    public Page<StaffResponse> listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return staffManagementService.listStaff(page, size);
    }

    @PatchMapping("/{staffId}/role")
    public StaffResponse updateRole(
            @PathVariable Long staffId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StaffRoleUpdateRequest request) {
        return staffManagementService.updateStaffRole(staffId, request, jwt.getSubject());
    }

    @PatchMapping("/{staffId}/status")
    public StaffResponse updateStatus(
            @PathVariable Long staffId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StaffStatusUpdateRequest request) {
        return staffManagementService.updateStaffStatus(
                staffId, jwt.getSubject(), request);
    }
}
