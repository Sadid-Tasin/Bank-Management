package com.bank.bankmanagement.account.factory;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.entity.CurrentAccount;
import com.bank.bankmanagement.account.entity.FixedDepositAccount;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.user.entity.BankUser;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountFactory {

    public Account createAccount(
            AccountType type,
            String accountNumber,
            BankUser owner,
            BigDecimal openingBalance,
            BigDecimal minimumBalance,
            BigDecimal annualInterestRate,
            Integer tenureMonths
    ) {

        if (type == null) {
            throw new IllegalArgumentException("Account type is required");
        }

        return switch (type) {

            case SAVINGS -> new SavingsAccount(
                    accountNumber,
                    owner,
                    openingBalance,
                    minimumBalance,
                    annualInterestRate
            );

            case CURRENT -> new CurrentAccount(
                    accountNumber,
                    owner,
                    openingBalance,
                    minimumBalance
            );

            case FIXED_DEPOSIT -> {
                if (tenureMonths == null || tenureMonths <= 0) {
                    throw new IllegalArgumentException(
                            "Valid fixed deposit tenure is required"
                    );
                }

                yield new FixedDepositAccount(
                        accountNumber,
                        owner,
                        openingBalance,
                        annualInterestRate,
                        tenureMonths
                );
            }
        };
    }
}
