package com.bank.bankmanagement.account.entity;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.account.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@DiscriminatorValue("FIXED_DEPOSIT")
public class FixedDepositAccount extends Account {

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    protected FixedDepositAccount() {
    }

    public FixedDepositAccount(String accountNumber, BankUser owner,
                               BigDecimal openingBalance,
                               BigDecimal annualInterestRate,
                               int tenureMonths) {

        super(accountNumber, owner, openingBalance,
                BigDecimal.ZERO, annualInterestRate);

        if (tenureMonths <= 0) {
            throw new IllegalArgumentException(
                    "Fixed deposit tenure must be greater than zero");
        }

        this.tenureMonths = tenureMonths;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.FIXED_DEPOSIT;
    }

    @Override
    public BigDecimal calculateInterest() {
        return getBalance()
                .multiply(getAnnualInterestRate())
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }
}
