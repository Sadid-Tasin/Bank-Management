package com.bank.bankmanagement.transaction.repository;

import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByReference(String reference);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM bank_transactions " +
            "WHERE source_account_id IN (SELECT id FROM bank_accounts WHERE account_number IN (:accountNumbers)) " +
            "OR destination_account_id IN (SELECT id FROM bank_accounts WHERE account_number IN (:accountNumbers))",
            nativeQuery = true)
    void deleteAllForAccountNumbers(@Param("accountNumbers") Collection<String> accountNumbers);

    boolean existsByReference(String reference);

    @Query("SELECT COUNT(t) FROM BankTransaction t " +
            "LEFT JOIN t.sourceAccount src LEFT JOIN t.destinationAccount dst " +
            "WHERE (src.accountNumber = :accountNumber OR dst.accountNumber = :accountNumber) " +
            "AND t.createdAt >= :fromTime")
    long countRecentActivity(@Param("accountNumber") String accountNumber,
                             @Param("fromTime") LocalDateTime fromTime);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t " +
            "WHERE t.type = :type AND t.createdAt >= :fromTime")
    BigDecimal sumAmountByTypeSince(@Param("type") TransactionType type,
                                    @Param("fromTime") LocalDateTime fromTime);

    @Query("""
            SELECT t FROM BankTransaction t
            WHERE t.sourceAccount.id IN (
                SELECT a.id FROM Account a
                WHERE a.accountNumber = :accountNumber
                  AND a.owner.id = :ownerId
            )
            OR t.destinationAccount.id IN (
                SELECT a.id FROM Account a
                WHERE a.accountNumber = :accountNumber
                  AND a.owner.id = :ownerId
            )
            """)
    Page<BankTransaction> findHistoryForAccountAndOwner(
            @Param("accountNumber") String accountNumber,
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );
}
