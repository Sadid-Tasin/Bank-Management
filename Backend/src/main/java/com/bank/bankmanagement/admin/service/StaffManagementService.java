package com.bank.bankmanagement.admin.service;

import com.bank.bankmanagement.admin.dto.StaffCreateRequest;
import com.bank.bankmanagement.admin.dto.StaffResponse;
import com.bank.bankmanagement.admin.dto.StaffRoleUpdateRequest;
import com.bank.bankmanagement.admin.dto.StaffStatusUpdateRequest;
import com.bank.bankmanagement.common.audit.AuditLogService;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StaffManagementService {

    private static final List<UserRole> MANAGED_STAFF_ROLES =
            List.of(UserRole.TELLER, UserRole.LOAN_OFFICER, UserRole.MANAGER);
    private static final int MAX_PAGE_SIZE = 100;

    private final BankUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public StaffManagementService(
            BankUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StaffResponse createStaff(StaffCreateRequest request) {
        return createStaff(request, "ADMIN");
    }

    @Transactional
    public StaffResponse createStaff(StaffCreateRequest request, String actingAdminEmail) {
        if (request == null) {
            throw new IllegalArgumentException("Staff details are required");
        }
        validateManagedRole(request.role());
        if (request.fullName() == null || request.fullName().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Valid staff name, email and password are required");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        String nationalId = normalize(request.nationalId());
        if (nationalId != null && userRepository.existsByNationalId(nationalId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "National ID is already registered");
        }

        BankUser staff = new BankUser(
                request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), request.role());
        staff.updateContactDetails(request.phone(), nationalId, request.address());
        BankUser saved = userRepository.save(staff);
        auditLogService.record(actingAdminEmail, "STAFF_CREATED", "STAFF", saved.getId().toString(), "Role=" + saved.getRole());
        return StaffResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> listStaff(int page, int size) {
        Pageable pageable = pageable(page, size);
        return userRepository.findByRoleIn(MANAGED_STAFF_ROLES, pageable)
                .map(StaffResponse::from);
    }

    @Transactional
    public StaffResponse updateStaffRole(Long staffId, StaffRoleUpdateRequest request) {
        return updateStaffRole(staffId, request, "ADMIN");
    }

    @Transactional
    public StaffResponse updateStaffRole(Long staffId, StaffRoleUpdateRequest request, String actingAdminEmail) {
        if (request == null) {
            throw new IllegalArgumentException("New staff role is required");
        }
        validateManagedRole(request.role());
        BankUser staff = findManagedStaff(staffId);
        staff.changeStaffRole(request.role());
        userRepository.save(staff);
        auditLogService.record(actingAdminEmail, "STAFF_ROLE_CHANGED", "STAFF", staff.getId().toString(), "New role=" + staff.getRole());
        return StaffResponse.from(staff);
    }

    @Transactional
    public StaffResponse updateStaffStatus(
            Long staffId, String actingAdminEmail, StaffStatusUpdateRequest request
    ) {
        if (request == null || request.enabled() == null) {
            throw new IllegalArgumentException("Enabled status is required");
        }
        BankUser staff = findManagedStaff(staffId);
        if (staff.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An administrator cannot change their own staff status");
        }
        if (request.enabled()) {
            staff.activate();
        } else {
            staff.deactivate();
        }
        userRepository.save(staff);
        auditLogService.record(actingAdminEmail, request.enabled() ? "STAFF_ACTIVATED" : "STAFF_DEACTIVATED",
                "STAFF", staff.getId().toString(), "Staff account status changed");
        return StaffResponse.from(staff);
    }

    private BankUser findManagedStaff(Long id) {
        BankUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Staff member not found"));
        if (!MANAGED_STAFF_ROLES.contains(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Staff member not found");
        }
        return user;
    }

    private void validateManagedRole(UserRole role) {
        if (role == null || !MANAGED_STAFF_ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    "Only TELLER, LOAN_OFFICER or MANAGER roles can be managed here");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Pageable pageable(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        return PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
