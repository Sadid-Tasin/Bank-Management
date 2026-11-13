package com.bank.bankmanagement.admin.service;

import com.bank.bankmanagement.admin.dto.StaffCreateRequest;
import com.bank.bankmanagement.admin.dto.StaffResponse;
import com.bank.bankmanagement.admin.dto.StaffRoleUpdateRequest;
import com.bank.bankmanagement.admin.dto.StaffStatusUpdateRequest;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StaffManagementServiceIntegrationTest {

    @Autowired private StaffManagementService staffService;
    @Autowired private BankUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String suffix;
    private String email;
    private Long staffId;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        email = "managed-staff-" + suffix + "@example.test";
    }

    @AfterEach
    void cleanUp() {
        if (email != null) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
        }
    }

    private StaffCreateRequest request(UserRole role) {
        return new StaffCreateRequest(
                "Managed Staff " + suffix, email, "StrongStaffPass123",
                "01720000000", "STAFF-NID-" + suffix, "Dhaka", role);
    }

    @Test
    void adminCanCreateStaffWithHashedPassword() {
        StaffResponse response = staffService.createStaff(request(UserRole.TELLER));
        staffId = response.id();

        BankUser saved = userRepository.findById(staffId).orElseThrow();
        assertEquals(UserRole.TELLER, saved.getRole());
        assertTrue(passwordEncoder.matches("StrongStaffPass123", saved.getPasswordHash()));
        assertNotEquals("StrongStaffPass123", saved.getPasswordHash());
        assertTrue(saved.isEnabled());
    }

    @Test
    void adminCannotCreateAnotherAdminOrCustomerThroughStaffEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> staffService.createStaff(request(UserRole.ADMIN)));
        assertThrows(IllegalArgumentException.class,
                () -> staffService.createStaff(request(UserRole.CUSTOMER)));
    }

    @Test
    void duplicateStaffEmailShouldBeRejected() {
        staffService.createStaff(request(UserRole.TELLER));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> staffService.createStaff(request(UserRole.MANAGER)));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void adminCanChangeStaffRole() {
        StaffResponse created = staffService.createStaff(request(UserRole.TELLER));
        staffId = created.id();

        StaffResponse updated = staffService.updateStaffRole(
                staffId, new StaffRoleUpdateRequest(UserRole.LOAN_OFFICER));
        assertEquals(UserRole.LOAN_OFFICER, updated.role());
    }

    @Test
    void adminCanDeactivateAndReactivateStaff() {
        StaffResponse created = staffService.createStaff(request(UserRole.MANAGER));
        staffId = created.id();

        StaffResponse deactivated = staffService.updateStaffStatus(
                staffId, "admin@example.test", new StaffStatusUpdateRequest(false));
        assertFalse(deactivated.enabled());

        StaffResponse reactivated = staffService.updateStaffStatus(
                staffId, "admin@example.test", new StaffStatusUpdateRequest(true));
        assertTrue(reactivated.enabled());
    }

    @Test
    void staffCannotBeChangedIntoAdminOrCustomer() {
        StaffResponse created = staffService.createStaff(request(UserRole.TELLER));
        staffId = created.id();

        assertThrows(IllegalArgumentException.class, () -> staffService.updateStaffRole(
                staffId, new StaffRoleUpdateRequest(UserRole.ADMIN)));
        assertThrows(IllegalArgumentException.class, () -> staffService.updateStaffRole(
                staffId, new StaffRoleUpdateRequest(UserRole.CUSTOMER)));
    }

    @Test
    void managedStaffCannotDeactivateOwnAccount() {
        StaffResponse created = staffService.createStaff(request(UserRole.TELLER));
        staffId = created.id();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> staffService.updateStaffStatus(staffId, email,
                        new StaffStatusUpdateRequest(false)));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
