package com.bank.bankmanagement.auth;

import com.bank.bankmanagement.auth.dto.AuthResponse;
import com.bank.bankmanagement.auth.dto.LoginRequest;
import com.bank.bankmanagement.auth.dto.ProfileResponse;
import com.bank.bankmanagement.auth.dto.ProfileUpdateRequest;
import com.bank.bankmanagement.auth.dto.RegisterRequest;
import com.bank.bankmanagement.auth.dto.RegistrationResponse;
import com.bank.bankmanagement.auth.service.AuthService;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private BankUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    private String testEmail;
    private String testNationalId;

    @BeforeEach
    void prepareTestIdentity() {
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 20);

        testEmail = "auth-" + suffix + "@example.test";
        testNationalId = "NID-" + suffix;
    }

    @AfterEach
    void cleanupTestUser() {
        if (testEmail != null) {
            userRepository.findByEmailIgnoreCase(testEmail)
                    .ifPresent(userRepository::delete);
        }
    }

    private RegisterRequest registrationRequest() {
        return new RegisterRequest(
                "Authentication Test Customer",
                testEmail,
                "StrongTestPassword123",
                "01700000000",
                testNationalId,
                "Dhaka, Bangladesh"
        );
    }

    @Test
    void registrationShouldCreateCustomerWithHashedPassword() {
        RegistrationResponse response =
                authService.register(registrationRequest());

        BankUser saved = userRepository
                .findByEmailIgnoreCase(testEmail)
                .orElseThrow();

        assertEquals(response.userId(), saved.getId());
        assertEquals(UserRole.CUSTOMER, response.role());
        assertEquals(KycStatus.PENDING, response.kycStatus());

        assertNotEquals("StrongTestPassword123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(
                "StrongTestPassword123", saved.getPasswordHash()));
    }

    @Test
    void publicRegistrationMustCreateCustomerOnly() {
        RegistrationResponse response =
                authService.register(registrationRequest());

        assertEquals(UserRole.CUSTOMER, response.role());
    }

    @Test
    void duplicateEmailShouldBeRejected() {
        authService.register(registrationRequest());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(registrationRequest())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void loginShouldReturnValidJwtToken() {
        authService.register(registrationRequest());

        AuthResponse response = authService.login(
                new LoginRequest(testEmail, "StrongTestPassword123")
        );

        assertEquals("Bearer", response.tokenType());
        assertEquals(testEmail, response.email());
        assertEquals(UserRole.CUSTOMER, response.role());
        assertEquals(3600, response.expiresInSeconds());

        Jwt decoded = jwtDecoder.decode(response.accessToken());

        assertEquals(testEmail, decoded.getSubject());
        assertEquals("CUSTOMER", decoded.getClaimAsString("role"));
        assertNotNull(decoded.getExpiresAt());
    }

    @Test
    void loginShouldRejectIncorrectPassword() {
        authService.register(registrationRequest());

        assertThrows(
                AuthenticationException.class,
                () -> authService.login(
                        new LoginRequest(testEmail, "WrongPassword123")
                )
        );
    }

    @Test
    void customerShouldViewAndUpdateOwnProfile() {
        authService.register(registrationRequest());

        ProfileResponse original = authService.getProfile(testEmail);

        assertEquals(testEmail, original.email());
        assertEquals(UserRole.CUSTOMER, original.role());

        ProfileResponse updated = authService.updateProfile(
                testEmail,
                new ProfileUpdateRequest(
                        "Updated Customer Name",
                        "01800000000",
                        "Chattogram, Bangladesh"
                )
        );

        assertEquals("Updated Customer Name", updated.fullName());
        assertEquals("01800000000", updated.phone());
        assertEquals("Chattogram, Bangladesh", updated.address());

        BankUser saved = userRepository
                .findByEmailIgnoreCase(testEmail)
                .orElseThrow();

        assertEquals(UserRole.CUSTOMER, saved.getRole());
        assertEquals(KycStatus.PENDING, saved.getKycStatus());
    }
}
