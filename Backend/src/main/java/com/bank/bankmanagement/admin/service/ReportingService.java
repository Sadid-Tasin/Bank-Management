package com.bank.bankmanagement.admin.service;
import com.bank.bankmanagement.account.enums.AccountStatus;import com.bank.bankmanagement.account.repository.AccountRepository;import com.bank.bankmanagement.admin.dto.*;import com.bank.bankmanagement.loan.enums.LoanStatus;import com.bank.bankmanagement.loan.repository.LoanRepository;import com.bank.bankmanagement.common.fraud.FraudAlertRepository;import com.bank.bankmanagement.transaction.entity.BankTransaction;import com.bank.bankmanagement.transaction.enums.TransactionType;import com.bank.bankmanagement.transaction.repository.TransactionRepository;import com.bank.bankmanagement.user.enums.UserRole;import com.bank.bankmanagement.user.repository.BankUserRepository;import org.springframework.data.domain.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.math.BigDecimal;import java.time.LocalDate;import java.time.LocalDateTime;import java.util.List;import java.util.Map;import java.util.TreeMap;
@Service public class ReportingService{
 private final BankUserRepository users;private final AccountRepository accounts;private final TransactionRepository transactions;private final LoanRepository loans;private final FraudAlertRepository fraud;
 public ReportingService(BankUserRepository u,AccountRepository a,TransactionRepository t,LoanRepository l,FraudAlertRepository f){users=u;accounts=a;transactions=t;loans=l;fraud=f;}
 @Transactional(readOnly=true) public SystemOverviewResponse overview(){LocalDateTime from=LocalDateTime.now().minusDays(30);return new SystemOverviewResponse(users.countByRole(UserRole.CUSTOMER),accounts.countByStatus(AccountStatus.ACTIVE),accounts.sumAllBalances(),loans.countByStatus(LoanStatus.PENDING),fraud.countByStatus("OPEN"),transactions.sumAmountByTypeSince(TransactionType.DEPOSIT,from),transactions.sumAmountByTypeSince(TransactionType.WITHDRAWAL,from));}
 @Transactional(readOnly=true) public Page<AdminTransactionResponse> transactions(int page,int size){if(page<0||size<1||size>100)throw new IllegalArgumentException("Page must be >= 0 and size 1 to 100");return transactions.findAll(PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))).map(AdminTransactionResponse::from);}

 /** Day-by-day deposits and withdrawals for the last {days} days (including today). Days with no activity are returned as zero. */
 @Transactional(readOnly=true) public List<DailyActivityResponse> dailyActivity(int days){
  if(days<1||days>90)throw new IllegalArgumentException("Days must be between 1 and 90");
  LocalDate today=LocalDate.now();LocalDate start=today.minusDays(days-1L);
  Map<LocalDate,BigDecimal[]> buckets=new TreeMap<>();
  for(int i=0;i<days;i++)buckets.put(start.plusDays(i),new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO});
  for(Object[] row:transactions.dailyTotalsSince(start.atStartOfDay())){
   LocalDate day=row[0] instanceof java.sql.Date d?d.toLocalDate():LocalDate.parse(String.valueOf(row[0]).substring(0,10));
   BigDecimal[] bucket=buckets.get(day);if(bucket==null)continue;
   BigDecimal total=new BigDecimal(String.valueOf(row[2]));
   if("DEPOSIT".equals(String.valueOf(row[1])))bucket[0]=total;else bucket[1]=total;
  }
  return buckets.entrySet().stream().map(e->new DailyActivityResponse(e.getKey().toString(),e.getValue()[0],e.getValue()[1])).toList();
 }
}
