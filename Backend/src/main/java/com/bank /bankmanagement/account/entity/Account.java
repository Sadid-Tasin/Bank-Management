package com.bank.bankmanagement.account.entity;

import com.bank.bankmanagement.account.enums.AccountStatus;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.user.entity.BankUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "bank_accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "account_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 30
)
public abstract class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private BankUser owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumBalance;

    @Column(name = "annual_interest_rate",
            nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    protected Account() {
    }

    protected Account(String accountNumber, BankUser owner,
                      BigDecimal openingBalance,
                      BigDecimal minimumBalance,
                      BigDecimal annualInterestRate) {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        this.owner = Objects.requireNonNull(owner, "Account owner is required");
        this.accountNumber = accountNumber;
        this.balance = requireNonNegative(openingBalance, "Opening balance");
        this.minimumBalance = requireNonNegative(
                minimumBalance, "Minimum balance");
        this.annualInterestRate = requireNonNegative(
                annualInterestRate, "Interest rate");

        if (this.balance.compareTo(this.minimumBalance) < 0) {
            throw new IllegalArgumentException(
                    "Opening balance cannot be less than minimum balance");
        }

        this.status = AccountStatus.ACTIVE;
    }

    public void deposit(BigDecimal amount) {
        ensureActive();
        validatePositiveAmount(amount);
        balance = balance.add(amount.setScale(2, RoundingMode.UNNECESSARY));
    }

    public void withdraw(BigDecimal amount) {
        ensureActive();
        validatePositiveAmount(amount);

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal remainingBalance = balance.subtract(normalizedAmount);

        if (remainingBalance.compareTo(minimumBalance) < 0) {
            throw new IllegalStateException(
                    "Insufficient balance or minimum balance requirement violated");
        }

        balance = remainingBalance;
    }

    public void freeze() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Closed account cannot be frozen");
        }
        status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new IllegalStateException("Account is not frozen");
        }
        status = AccountStatus.ACTIVE;
    }

    public void close() {
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException(
                    "Account balance must be zero before closing");
        }
        status = AccountStatus.CLOSED;
    }

    protected void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
    }

    protected static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places");
        }
    }

    private static BigDecimal requireNonNegative(
            BigDecimal value, String fieldName) {

        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative or null");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal calculateAnnualInterest() {
        return balance.multiply(annualInterestRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public abstract AccountType getAccountType();

    public abstract BigDecimal calculateInterest();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BankUser getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
