package com.bank.bankmanagement.transaction.entity;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.transaction.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BankTransaction() {
    }

    public BankTransaction(
            TransactionType type,
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            String description
    ) {
        this.type = Objects.requireNonNull(type, "Transaction type is required");

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places");
        }

        if (type == TransactionType.DEPOSIT
                && (sourceAccount != null || destinationAccount == null)) {
            throw new IllegalArgumentException("Invalid deposit accounts");
        }

        if (type == TransactionType.WITHDRAWAL
                && (sourceAccount == null || destinationAccount != null)) {
            throw new IllegalArgumentException("Invalid withdrawal accounts");
        }

        if (type == TransactionType.TRANSFER
                && (sourceAccount == null || destinationAccount == null
                || sourceAccount.getAccountNumber()
                .equals(destinationAccount.getAccountNumber()))) {
            throw new IllegalArgumentException("Invalid transfer accounts");
        }

        this.reference = UUID.randomUUID().toString();
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        this.description = description == null || description.isBlank()
                ? null : description.trim();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public TransactionType getType() {
        return type;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
