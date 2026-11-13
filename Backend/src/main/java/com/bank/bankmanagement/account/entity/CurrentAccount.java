package com.bank.bankmanagement.account.entity;

import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.account.enums.AccountType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("CURRENT")
public class CurrentAccount extends Account {

    protected CurrentAccount() {
    }

    public CurrentAccount(String accountNumber, BankUser owner,
                          BigDecimal openingBalance,
                          BigDecimal minimumBalance) {

        super(accountNumber, owner, openingBalance,
                minimumBalance, BigDecimal.ZERO);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }

    @Override
    public BigDecimal calculateInterest() {
        return BigDecimal.ZERO.setScale(2);
    }
}
