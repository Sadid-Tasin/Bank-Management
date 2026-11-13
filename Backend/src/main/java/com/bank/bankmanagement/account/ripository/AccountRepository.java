package com.bank.bankmanagement.account.repository;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.enums.AccountStatus;
import java.math.BigDecimal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findAllByOwner_Id(Long ownerId);

    Optional<Account> findByAccountNumberAndOwner_Id(
            String accountNumber, Long ownerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Account a WHERE a.owner.id = :ownerId")
    void deleteAllByOwner_Id(@Param("ownerId") Long ownerId);

    List<Account> findAllByStatus(AccountStatus status);

    long countByStatus(AccountStatus status);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    BigDecimal sumAllBalances();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(
            @Param("accountNumber") String accountNumber
    );
}
