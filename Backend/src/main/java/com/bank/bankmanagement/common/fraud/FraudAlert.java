package com.bank.bankmanagement.common.fraud;
import com.bank.bankmanagement.account.entity.Account;import com.bank.bankmanagement.transaction.entity.BankTransaction;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="fraud_alerts")
public class FraudAlert{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="transaction_id",nullable=false,unique=true) private BankTransaction transaction;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="account_id",nullable=false) private Account account;
 @Column(nullable=false,length=20) private String severity;
 @Column(nullable=false,length=500) private String reason;
 @Column(nullable=false,length=20) private String status="OPEN";
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="resolved_at") private LocalDateTime resolvedAt;
 @Column(name="resolved_by_email",length=150) private String resolvedByEmail;
 protected FraudAlert(){}
 public FraudAlert(BankTransaction tx,Account account,String severity,String reason){this.transaction=tx;this.account=account;this.severity=severity;this.reason=reason;this.createdAt=LocalDateTime.now();}
 public void resolve(String email){if(!"OPEN".equals(status))throw new IllegalStateException("Fraud alert is already resolved");if(email==null||email.isBlank())throw new IllegalArgumentException("Resolver identity is required");status="RESOLVED";resolvedAt=LocalDateTime.now();resolvedByEmail=email.trim().toLowerCase();}
 public Long getId(){return id;}public BankTransaction getTransaction(){return transaction;}public Account getAccount(){return account;}public String getSeverity(){return severity;}public String getReason(){return reason;}public String getStatus(){return status;}public LocalDateTime getCreatedAt(){return createdAt;}public LocalDateTime getResolvedAt(){return resolvedAt;}public String getResolvedByEmail(){return resolvedByEmail;}
}
