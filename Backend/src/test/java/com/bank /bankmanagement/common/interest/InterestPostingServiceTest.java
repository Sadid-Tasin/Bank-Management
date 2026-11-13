package com.bank.bankmanagement.common.interest;

import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.enums.AccountStatus;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.common.audit.AuditLogService;
import com.bank.bankmanagement.common.notification.NotificationService;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestPostingServiceTest {
    @Mock private AccountRepository accounts;
    @Mock private InterestPostingRepository postings;
    @Mock private TransactionRepository transactions;
    @Mock private AuditLogService audit;
    @Mock private NotificationService notifications;

    @Test void savingsInterestIsCalculatedAndDuplicatePeriodIsSkipped() {
        BankUser user=new BankUser("Interest Tester","interest@test.example","hash",UserRole.CUSTOMER);
        SavingsAccount account=new SavingsAccount("SAV-INTEREST-01",user,new BigDecimal("5000.00"),new BigDecimal("500.00"),new BigDecimal("5.00"));
        InterestPostingService service=new InterestPostingService(accounts,postings,transactions,audit,notifications);
        YearMonth month=YearMonth.now();
        when(accounts.findAllByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(postings.existsByAccount_IdAndPostingKey(null,"SAVINGS_"+month)).thenReturn(false,true);
        assertEquals(1,service.postMonthlyInterest(month));
        assertEquals(0,service.postMonthlyInterest(month));
        assertEquals(new BigDecimal("5020.83"),account.getBalance());
        verify(transactions,times(1)).save(any());
        verify(postings,times(1)).save(any());
    }
}
