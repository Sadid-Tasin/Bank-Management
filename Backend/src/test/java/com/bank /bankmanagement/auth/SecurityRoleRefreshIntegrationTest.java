package com.bank.bankmanagement.auth;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityRoleRefreshIntegrationTest {

    @Autowired private BankUserRepository userRepository;
    @Autowired private JwtAuthenticationConverter jwtAuthenticationConverter;

    private String email;
    private BankUser user;

    @BeforeEach
    void setUp() {
        email = "security-refresh-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16) + "@example.test";
        user = userRepository.save(new BankUser(
                "Security Refresh Staff", email, "test-hash", UserRole.TELLER));
    }

    @AfterEach
    void cleanUp() {
        if (email != null) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
        }
    }

    private Jwt tokenWithStaleCustomerRole() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "CUSTOMER")
                .build();
    }

    @Test
    void currentDatabaseRoleOverridesStaleJwtRoleClaim() {
        user.changeStaffRole(UserRole.MANAGER);
        userRepository.save(user);

        var authentication = jwtAuthenticationConverter.convert(tokenWithStaleCustomerRole());

        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER")));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void deactivatedStaffCannotAuthenticateWithPreviouslyIssuedJwt() {
        user.deactivate();
        userRepository.save(user);

        assertThrows(DisabledException.class,
                () -> jwtAuthenticationConverter.convert(tokenWithStaleCustomerRole()));
    }
}
