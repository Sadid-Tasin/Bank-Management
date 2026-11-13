package com.bank.bankmanagement.loan.entity;

import com.bank.bankmanagement.account.entity.Account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="loan_repayments")
public class LoanRepayment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=36) private String reference;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="loan_id", nullable=false) private Loan loan;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="paying_account_id", nullable=false) private Account payingAccount;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal amount;
    @Column(name="installment_number", nullable=false) private Integer installmentNumber;
    @Column(name="paid_at", nullable=false, updatable=false) private LocalDateTime paidAt;
    protected LoanRepayment() {}
    public LoanRepayment(Loan loan, Account payingAccount, BigDecimal amount, int installmentNumber) {
        this.reference=UUID.randomUUID().toString(); this.loan=loan; this.payingAccount=payingAccount;
        this.amount=amount; this.installmentNumber=installmentNumber; this.paidAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public String getReference(){return reference;} public Loan getLoan(){return loan;}
    public Account getPayingAccount(){return payingAccount;} public BigDecimal getAmount(){return amount;}
    public Integer getInstallmentNumber(){return installmentNumber;} public LocalDateTime getPaidAt(){return paidAt;}
}
