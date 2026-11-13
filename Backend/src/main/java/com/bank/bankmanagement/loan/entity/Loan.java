package com.bank.bankmanagement.loan.entity;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.loan.enums.LoanStatus;
import com.bank.bankmanagement.loan.enums.LoanType;
import com.bank.bankmanagement.user.entity.BankUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "bank_loans")
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=36)
    private String loanNumber;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="applicant_id", nullable=false)
    private BankUser applicant;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="linked_account_id", nullable=false)
    private Account linkedAccount;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private LoanType loanType;
    @Column(nullable=false, length=500)
    private String purpose;
    @Column(name="principal_amount", nullable=false, precision=19, scale=2)
    private BigDecimal principalAmount;
    @Column(name="annual_interest_rate", precision=5, scale=2)
    private BigDecimal annualInterestRate;
    @Column(name="term_months", nullable=false)
    private Integer termMonths;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private LoanStatus status;
    @Column(name="monthly_installment", precision=19, scale=2)
    private BigDecimal monthlyInstallment;
    @Column(name="total_payable", precision=19, scale=2)
    private BigDecimal totalPayable;
    @Column(name="outstanding_amount", precision=19, scale=2)
    private BigDecimal outstandingAmount;
    @Column(name="submitted_at", nullable=false, updatable=false)
    private LocalDateTime submittedAt;
    @Column(name="reviewed_at") private LocalDateTime reviewedAt;
    @Column(name="reviewed_by_email", length=150) private String reviewedByEmail;
    @Column(name="decision_note", length=500) private String decisionNote;
    @Column(name="disbursed_at") private LocalDateTime disbursedAt;
    @Column(name="next_due_date") private LocalDate nextDueDate;
    @Version private Long version;

    protected Loan() {}

    public Loan(BankUser applicant, Account linkedAccount, LoanType loanType,
                String purpose, BigDecimal principalAmount, int termMonths) {
        this.applicant = Objects.requireNonNull(applicant);
        this.linkedAccount = Objects.requireNonNull(linkedAccount);
        this.loanType = Objects.requireNonNull(loanType);
        if (purpose == null || purpose.isBlank() || purpose.trim().length() > 500)
            throw new IllegalArgumentException("Loan purpose is required (maximum 500 characters)");
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0
                || principalAmount.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Loan amount must be positive with at most two decimals");
        if (termMonths < 3 || termMonths > 84)
            throw new IllegalArgumentException("Loan term must be between 3 and 84 months");
        this.loanNumber = UUID.randomUUID().toString();
        this.purpose = purpose.trim();
        this.principalAmount = principalAmount.setScale(2, RoundingMode.UNNECESSARY);
        this.termMonths = termMonths;
        this.status = LoanStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
    }

    public void approve(BigDecimal annualRate, String reviewerEmail, String note) {
        ensurePending();
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) < 0
                || annualRate.compareTo(new BigDecimal("40.00")) > 0
                || annualRate.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Annual interest rate must be between 0 and 40 percent");
        if (reviewerEmail == null || reviewerEmail.isBlank())
            throw new IllegalArgumentException("Reviewer identity is required");
        this.annualInterestRate = annualRate.setScale(2, RoundingMode.HALF_UP);
        this.monthlyInstallment = calculateEmi(principalAmount, annualInterestRate, termMonths);
        this.totalPayable = annualInterestRate.compareTo(BigDecimal.ZERO) == 0
                ? principalAmount
                : monthlyInstallment.multiply(BigDecimal.valueOf(termMonths)).setScale(2, RoundingMode.HALF_UP);
        this.outstandingAmount = totalPayable;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedByEmail = reviewerEmail.trim().toLowerCase();
        this.decisionNote = normalize(note);
        this.status = LoanStatus.DISBURSED;
        this.disbursedAt = LocalDateTime.now();
        this.nextDueDate = LocalDate.now().plusMonths(1);
    }

    public void reject(String reviewerEmail, String note) {
        ensurePending();
        if (reviewerEmail == null || reviewerEmail.isBlank())
            throw new IllegalArgumentException("Reviewer identity is required");
        if (note == null || note.isBlank())
            throw new IllegalArgumentException("A rejection reason is required");
        this.reviewedAt = LocalDateTime.now();
        this.reviewedByEmail = reviewerEmail.trim().toLowerCase();
        this.decisionNote = normalize(note);
        this.status = LoanStatus.REJECTED;
    }

    public void recordRepayment(BigDecimal amount, int installmentNumber) {
        if (status != LoanStatus.DISBURSED)
            throw new IllegalStateException("Only disbursed loans can be repaid");
        if (installmentNumber < 1 || installmentNumber > termMonths)
            throw new IllegalArgumentException("No installments remain");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("Repayment amount must be positive with at most two decimals");
        BigDecimal expected = installmentNumber == termMonths ? outstandingAmount : monthlyInstallment;
        if (amount.setScale(2, RoundingMode.UNNECESSARY).compareTo(expected) != 0)
            throw new IllegalArgumentException("Repayment must equal the next installment: " + expected);
        outstandingAmount = outstandingAmount.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        if (outstandingAmount.compareTo(BigDecimal.ZERO) == 0) {
            status = LoanStatus.PAID_OFF;
            nextDueDate = null;
        } else {
            nextDueDate = (nextDueDate == null ? LocalDate.now() : nextDueDate).plusMonths(1);
        }
    }

    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal == null || annualRate == null || months <= 0)
            throw new IllegalArgumentException("Principal, rate and term are required");
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 16, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0)
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate, mc).pow(months, mc);
        BigDecimal numerator = principal.multiply(monthlyRate, mc).multiply(factor, mc);
        return numerator.divide(factor.subtract(BigDecimal.ONE, mc), mc)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void ensurePending() {
        if (status != LoanStatus.PENDING) throw new IllegalStateException("Loan application has already been reviewed");
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public Long getId(){return id;} public String getLoanNumber(){return loanNumber;}
    public BankUser getApplicant(){return applicant;} public Account getLinkedAccount(){return linkedAccount;}
    public LoanType getLoanType(){return loanType;} public String getPurpose(){return purpose;}
    public BigDecimal getPrincipalAmount(){return principalAmount;} public BigDecimal getAnnualInterestRate(){return annualInterestRate;}
    public Integer getTermMonths(){return termMonths;} public LoanStatus getStatus(){return status;}
    public BigDecimal getMonthlyInstallment(){return monthlyInstallment;} public BigDecimal getTotalPayable(){return totalPayable;}
    public BigDecimal getOutstandingAmount(){return outstandingAmount;} public LocalDateTime getSubmittedAt(){return submittedAt;}
    public LocalDateTime getReviewedAt(){return reviewedAt;} public String getReviewedByEmail(){return reviewedByEmail;}
    public String getDecisionNote(){return decisionNote;} public LocalDateTime getDisbursedAt(){return disbursedAt;}
    public LocalDate getNextDueDate(){return nextDueDate;}
}
