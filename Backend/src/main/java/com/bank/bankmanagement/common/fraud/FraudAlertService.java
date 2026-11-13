package com.bank.bankmanagement.common.fraud;
import com.bank.bankmanagement.common.audit.AuditLogService;import org.springframework.data.domain.*;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;
@Service public class FraudAlertService{
 private final FraudAlertRepository repo;private final AuditLogService audit;public FraudAlertService(FraudAlertRepository r,AuditLogService a){repo=r;audit=a;}
 @Transactional(readOnly=true) public Page<FraudAlertResponse> open(int page,int size){validate(page,size);return repo.findByStatusOrderByCreatedAtDesc("OPEN",PageRequest.of(page,size)).map(FraudAlertResponse::from);}
 @Transactional public FraudAlertResponse resolve(Long id,String email){FraudAlert a=repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Fraud alert not found"));a.resolve(email);audit.record(email,"FRAUD_ALERT_RESOLVED","FRAUD_ALERT",id.toString(),a.getReason());return FraudAlertResponse.from(a);}
 private void validate(int p,int s){if(p<0||s<1||s>100)throw new IllegalArgumentException("Page must be >= 0 and size 1 to 100");}
}
