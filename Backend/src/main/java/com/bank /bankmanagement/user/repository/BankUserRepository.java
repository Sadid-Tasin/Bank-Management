package com.bank.bankmanagement.user.repository;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface BankUserRepository extends JpaRepository<BankUser, Long> {

    Optional<BankUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNationalId(String nationalId);

    Page<BankUser> findByRole(UserRole role, Pageable pageable);

    long countByRole(UserRole role);

    Page<BankUser> findByRoleAndKycStatus(UserRole role, KycStatus kycStatus, Pageable pageable);

    Page<BankUser> findByRoleIn(Collection<UserRole> roles, Pageable pageable);
}
