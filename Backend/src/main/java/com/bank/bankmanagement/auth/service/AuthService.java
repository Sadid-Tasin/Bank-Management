package com.bank.bankmanagement.auth.service;

import com.bank.bankmanagement.auth.dto.AuthResponse;
import com.bank.bankmanagement.auth.dto.LoginRequest;
import com.bank.bankmanagement.auth.dto.ProfileResponse;
import com.bank.bankmanagement.auth.dto.ProfileUpdateRequest;
import com.bank.bankmanagement.auth.dto.RegisterRequest;
import com.bank.bankmanagement.auth.dto.RegistrationResponse;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final BankUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            BankUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already registered");
        }

        String nationalId = normalizeOptional(request.nationalId());

        if (nationalId != null && userRepository.existsByNationalId(nationalId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "National ID is already registered");
        }

        BankUser user = new BankUser(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.CUSTOMER
        );

        user.updateContactDetails(
                request.phone(),
                nationalId,
                request.address()
        );

        BankUser saved = userRepository.save(user);

        return new RegistrationResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRole(),
                saved.getKycStatus()
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email, request.password())
        );

        BankUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found"));

        return new AuthResponse(
                "Bearer",
                jwtService.generateAccessToken(user),
                jwtService.getTokenTtlSeconds(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        BankUser user = findUser(email);
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(
            String email,
            ProfileUpdateRequest request
    ) {
        BankUser user = findUser(email);

        user.updateProfile(
                request.fullName(),
                request.phone(),
                request.address()
        );

        return toProfileResponse(user);
    }

    private BankUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse toProfileResponse(BankUser user) {
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getRole(),
                user.getKycStatus()
        );
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
