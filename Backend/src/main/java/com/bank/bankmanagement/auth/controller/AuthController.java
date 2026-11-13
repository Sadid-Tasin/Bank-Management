package com.bank.bankmanagement.auth.controller;

import com.bank.bankmanagement.auth.dto.AuthResponse;
import com.bank.bankmanagement.auth.dto.LoginRequest;
import com.bank.bankmanagement.auth.dto.ProfileResponse;
import com.bank.bankmanagement.auth.dto.ProfileUpdateRequest;
import com.bank.bankmanagement.auth.dto.RegisterRequest;
import com.bank.bankmanagement.auth.dto.RegistrationResponse;
import com.bank.bankmanagement.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return authService.getProfile(jwt.getSubject());
    }

    @PutMapping("/me")
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return authService.updateProfile(jwt.getSubject(), request);
    }
}
