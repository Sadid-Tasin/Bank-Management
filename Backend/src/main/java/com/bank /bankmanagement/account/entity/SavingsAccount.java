package com.bank.bankmanagement.account.entity;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.account.enums.AccountType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("SAVINGS")
public class SavingsAccount extends Account {

    protected SavingsAccount() {
    }

    public SavingsAccount(String accountNumber, BankUser owner,
                          BigDecimal openingBalance,
                          BigDecimal minimumBalance,
                          BigDecimal annualInterestRate) {

        super(accountNumber, owner, openingBalance,
                minimumBalance, annualInterestRate);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }

    @Override
    public BigDecimal calculateInterest() {
        return calculateAnnualInterest();
    }
}
