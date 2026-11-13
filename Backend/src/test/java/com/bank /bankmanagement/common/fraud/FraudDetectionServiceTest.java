package com.bank.bankmanagement.common.fraud;

import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.transaction.entity.BankTransaction;
import com.bank.bankmanagement.transaction.enums.TransactionType;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {
    @Mock private TransactionRepository transactions;
    @Mock private FraudAlertRepository alerts;
    private FraudDetectionService service;
    private SavingsAccount account;
    @BeforeEach void setUp(){service=new FraudDetectionService(transactions,alerts);BankUser u=new BankUser("Fraud Test","fraud@test.example","hash", UserRole.CUSTOMER);account=new SavingsAccount("FRAUD-001",u,new BigDecimal("5000"),new BigDecimal("500"),new BigDecimal("5"));}

    @Test void highValueTransactionCreatesAlert(){
        BankTransaction tx=new BankTransaction(TransactionType.WITHDRAWAL,account,null,new BigDecimal("100000.00"),"test");
        when(transactions.countRecentActivity(eq("FRAUD-001"),any(LocalDateTime.class))).thenReturn(1L);
        when(alerts.existsByTransaction_Reference(tx.getReference())).thenReturn(false);
        service.inspect(tx);
        verify(alerts).save(any(FraudAlert.class));
    }

    @Test void normalTransactionDoesNotCreateAlert(){
        BankTransaction tx=new BankTransaction(TransactionType.DEPOSIT,null,account,new BigDecimal("500.00"),"test");
        when(transactions.countRecentActivity(eq("FRAUD-001"),any(LocalDateTime.class))).thenReturn(2L);
        service.inspect(tx);
        verify(alerts,never()).save(any(FraudAlert.class));
    }

    @Test void rapidTransactionSequenceCreatesAlert(){
        BankTransaction tx=new BankTransaction(TransactionType.WITHDRAWAL,account,null,new BigDecimal("100.00"),"test");
        when(transactions.countRecentActivity(eq("FRAUD-001"),any(LocalDateTime.class))).thenReturn(5L);
        when(alerts.existsByTransaction_Reference(tx.getReference())).thenReturn(false);
        service.inspect(tx);
        verify(alerts).save(any(FraudAlert.class));
    }
}
