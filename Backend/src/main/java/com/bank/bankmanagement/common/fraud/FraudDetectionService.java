package com.bank.bankmanagement.common.fraud;
import com.bank.bankmanagement.account.entity.Account;import com.bank.bankmanagement.transaction.entity.BankTransaction;import com.bank.bankmanagement.transaction.repository.TransactionRepository;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.math.BigDecimal;import java.time.LocalDateTime;
@Service public class FraudDetectionService{
 private static final BigDecimal LARGE_TRANSACTION=new BigDecimal("100000.00");private static final int VELOCITY_LIMIT=5;
 private final TransactionRepository transactions;private final FraudAlertRepository alerts;
 public FraudDetectionService(TransactionRepository t,FraudAlertRepository a){transactions=t;alerts=a;}
 @Transactional public void inspect(BankTransaction tx){Account account=tx.getSourceAccount()!=null?tx.getSourceAccount():tx.getDestinationAccount();if(account==null)return;
  long recent=transactions.countRecentActivity(account.getAccountNumber(),LocalDateTime.now().minusMinutes(10));
  boolean large=tx.getAmount().compareTo(LARGE_TRANSACTION)>=0;boolean rapid=recent>=VELOCITY_LIMIT;
  if((large||rapid)&&!alerts.existsByTransaction_Reference(tx.getReference())){String reason=large&&rapid?"High-value transaction and unusual transaction frequency":large?"Single transaction meets high-value monitoring threshold":"Five or more account transactions within ten minutes";alerts.save(new FraudAlert(tx,account,large?"HIGH":"MEDIUM",reason));}
 }
}
