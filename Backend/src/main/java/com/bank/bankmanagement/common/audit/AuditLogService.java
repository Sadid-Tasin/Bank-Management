package com.bank.bankmanagement.common.audit;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class AuditLogService {
 private final AuditLogRepository repository; public AuditLogService(AuditLogRepository repository){this.repository=repository;}
 @Transactional public void record(String actor,String action,String type,String id,String details){repository.save(new AuditLog(actor,action,type,id,truncate(details,1000)));}
 @Transactional(readOnly=true) public Page<AuditLog> latest(int page,int size){if(page<0||size<1||size>100)throw new IllegalArgumentException("Page must be >= 0 and size 1 to 100");return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page,size));}
 private String truncate(String s,int n){return s==null?null:(s.length()>n?s.substring(0,n):s);}
}
