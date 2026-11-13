package com.bank.bankmanagement.user.entity;

import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "bank_users")
public class BankUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(unique = true, length = 30)
    private String nationalId;

    @Column(length = 255)
    private String address;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 500)
    private String kycReviewNote;

    private LocalDateTime kycReviewedAt;

    @Column(length = 150)
    private String kycReviewedByEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BankUser() {
    }

    public BankUser(String fullName, String email,
                    String passwordHash, UserRole role) {
        this.fullName = Objects.requireNonNull(fullName);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.kycStatus = KycStatus.PENDING;
        this.enabled = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void updateKycStatus(KycStatus status) {
        this.kycStatus = Objects.requireNonNull(status);
    }

    public void reviewKycStatus(KycStatus status, String reviewerEmail, String note) {
        if (this.role != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only customer KYC can be reviewed");
        }
        if (this.kycStatus != KycStatus.PENDING) {
            throw new IllegalStateException("Only pending KYC applications can be reviewed");
        }
        if (status == null || status == KycStatus.PENDING) {
            throw new IllegalArgumentException("KYC decision must be VERIFIED or REJECTED");
        }
        if (reviewerEmail == null || reviewerEmail.isBlank()) {
            throw new IllegalArgumentException("Reviewer identity is required");
        }
        this.kycStatus = status;
        this.kycReviewedAt = LocalDateTime.now();
        this.kycReviewedByEmail = reviewerEmail.trim().toLowerCase();
        this.kycReviewNote = normalizeOptional(note);
    }

    public void changeStaffRole(UserRole role) {
        if (this.role == UserRole.CUSTOMER || this.role == UserRole.ADMIN) {
            throw new IllegalStateException("Customer or administrator role cannot be changed here");
        }
        if (role == null || role == UserRole.CUSTOMER || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Only TELLER, LOAN_OFFICER or MANAGER roles are assignable");
        }
        this.role = role;
    }

    public void deactivate() {
        this.enabled = false;
    }

    public void activate() {
        this.enabled = true;
    }

    public void updateContactDetails(
            String phone, String nationalId, String address) {
        this.phone = normalizeOptional(phone);
        this.nationalId = normalizeOptional(nationalId);
        this.address = normalizeOptional(address);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public void updateProfile(String fullName, String phone, String address) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        this.fullName = fullName.trim();
        this.phone = normalizeOptional(phone);
        this.address = normalizeOptional(address);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getAddress() {
        return address;
    }

    public UserRole getRole() {
        return role;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getKycReviewNote() {
        return kycReviewNote;
    }

    public LocalDateTime getKycReviewedAt() {
        return kycReviewedAt;
    }

    public String getKycReviewedByEmail() {
        return kycReviewedByEmail;
    }
}
