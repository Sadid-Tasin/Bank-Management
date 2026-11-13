package com.bank.bankmanagement.common.interest;
import com.bank.bankmanagement.account.entity.*;import com.bank.bankmanagement.account.enums.AccountStatus;import com.bank.bankmanagement.account.repository.AccountRepository;import com.bank.bankmanagement.common.audit.AuditLogService;import com.bank.bankmanagement.common.notification.NotificationService;import com.bank.bankmanagement.transaction.entity.BankTransaction;import com.bank.bankmanagement.transaction.enums.TransactionType;import com.bank.bankmanagement.transaction.repository.TransactionRepository;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.math.BigDecimal;import java.math.RoundingMode;import java.time.*;
@Service public class InterestPostingService{
 private final AccountRepository accounts;private final InterestPostingRepository postings;private final TransactionRepository transactions;private final AuditLogService audit;private final NotificationService notifications;
 public InterestPostingService(AccountRepository a,InterestPostingRepository p,TransactionRepository t,AuditLogService au,NotificationService n){accounts=a;postings=p;transactions=t;audit=au;notifications=n;}
 @Scheduled(cron="0 0 2 1 * *",zone="Asia/Dhaka") @Transactional public void scheduledMonthlyPosting(){postMonthlyInterest(YearMonth.now(ZoneId.of("Asia/Dhaka")).minusMonths(1));}
 @Transactional public int postMonthlyInterest(YearMonth month){
  YearMonth current=YearMonth.now(ZoneId.of("Asia/Dhaka"));
  if(month==null||month.isAfter(current)||month.isBefore(current.minusMonths(1)))
   throw new IllegalArgumentException("Interest period must be the current or previous month");
  int count=0;
  for(Account account:accounts.findAllByStatus(AccountStatus.ACTIVE)){
   String key;BigDecimal amount;
   if(account instanceof FixedDepositAccount fdr){LocalDate maturity=account.getCreatedAt().toLocalDate().plusMonths(fdr.getTenureMonths());if(LocalDate.now(ZoneId.of("Asia/Dhaka")).isBefore(maturity))continue;key="FDR_MATURITY";amount=fdr.calculateInterest();}
   else if(account instanceof SavingsAccount){key="SAVINGS_"+month;amount=account.calculateInterest().divide(new BigDecimal("12"),2,RoundingMode.HALF_UP);}
   else continue;
   if(amount.compareTo(BigDecimal.ZERO)<=0||postings.existsByAccount_IdAndPostingKey(account.getId(),key))continue;
   account.deposit(amount);transactions.save(new BankTransaction(TransactionType.DEPOSIT,null,account,amount,"Interest credit: "+key));postings.save(new InterestPosting(account,key,amount));
   audit.record("SYSTEM","INTEREST_POSTED","ACCOUNT",account.getAccountNumber(),"Period="+key+", amount="+amount);
   notifications.notifyUser(account.getOwner(),"Interest credited","BDT "+amount+" interest has been credited to account "+account.getAccountNumber());count++;
  }return count;
 }
}
