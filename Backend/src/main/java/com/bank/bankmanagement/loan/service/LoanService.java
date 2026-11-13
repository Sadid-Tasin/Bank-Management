package com.bank.bankmanagement.loan.service;

import com.bank.bankmanagement.account.entity.Account;
import com.bank.bankmanagement.account.enums.AccountStatus;
import com.bank.bankmanagement.account.enums.AccountType;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.banking.service.BankingService;
import com.bank.bankmanagement.common.audit.AuditLogService;
import com.bank.bankmanagement.common.notification.NotificationService;
import com.bank.bankmanagement.loan.dto.*;
import com.bank.bankmanagement.loan.entity.Loan;
import com.bank.bankmanagement.loan.entity.LoanRepayment;
import com.bank.bankmanagement.loan.enums.LoanStatus;
import com.bank.bankmanagement.loan.repository.LoanRepository;
import com.bank.bankmanagement.loan.repository.LoanRepaymentRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class LoanService {
    private static final int MAX_PAGE_SIZE=100;
    private static final BigDecimal MAX_LOAN_AMOUNT=new BigDecimal("5000000.00");
    private final LoanRepository loanRepository; private final LoanRepaymentRepository repaymentRepository;
    private final BankUserRepository userRepository; private final AccountRepository accountRepository;
    private final BankingService bankingService; private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    public LoanService(LoanRepository loanRepository, LoanRepaymentRepository repaymentRepository,
        BankUserRepository userRepository, AccountRepository accountRepository, BankingService bankingService,
        AuditLogService auditLogService, NotificationService notificationService) {
        this.loanRepository=loanRepository; this.repaymentRepository=repaymentRepository; this.userRepository=userRepository;
        this.accountRepository=accountRepository; this.bankingService=bankingService; this.auditLogService=auditLogService;
        this.notificationService=notificationService;
    }

    @Transactional
    public LoanResponse apply(String email, LoanApplicationRequest request) {
        BankUser applicant=userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));
        if (applicant.getRole()!=UserRole.CUSTOMER) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only customers can apply for loans");
        if (applicant.getKycStatus()!=KycStatus.VERIFIED) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"KYC must be verified before applying for a loan");
        if (request.principalAmount().compareTo(MAX_LOAN_AMOUNT)>0) throw new IllegalArgumentException("Maximum loan amount is BDT 5,000,000");
        Account account=accountRepository.findByAccountNumberAndOwner_Id(request.accountNumber().trim(),applicant.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Linked account not found"));
        if (account.getStatus()!=AccountStatus.ACTIVE || account.getAccountType()==AccountType.FIXED_DEPOSIT)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Loan must be linked to an active Savings or Current account");
        Loan saved=loanRepository.save(new Loan(applicant,account,request.loanType(),request.purpose(),request.principalAmount(),request.termMonths()));
        auditLogService.record(email,"LOAN_APPLIED","LOAN",saved.getLoanNumber(),"Loan application submitted");
        notificationService.notifyUser(applicant,"Loan application received","Your loan application " + saved.getLoanNumber() + " is pending review.");
        return LoanResponse.from(saved);
    }

    @Transactional(readOnly=true)
    public Page<LoanResponse> myLoans(String email,int page,int size) {
        BankUser user=userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));
        return loanRepository.findByApplicant_Id(user.getId(),pageable(page,size)).map(LoanResponse::from);
    }

    @Transactional(readOnly=true)
    public LoanResponse getMyLoan(String email,Long loanId) {
        BankUser user=userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));
        Loan loan=loanRepository.findByIdAndApplicant_Id(loanId,user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan not found"));
        return LoanResponse.from(loan);
    }

    @Transactional(readOnly=true)
    public Page<LoanResponse> pendingLoans(int page,int size) {
        return loanRepository.findByStatus(LoanStatus.PENDING,pageable(page,size)).map(LoanResponse::from);
    }

    @Transactional
    public LoanResponse decide(Long loanId,String officerEmail,LoanDecisionRequest request) {
        if (request==null || request.approved()==null) throw new IllegalArgumentException("Loan decision is required");
        Loan loan=loanRepository.findByIdForUpdate(loanId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan application not found"));
        if (loan.getStatus()!=LoanStatus.PENDING) throw new ResponseStatusException(HttpStatus.CONFLICT,"Loan has already been reviewed");
        if (request.approved()) {
            if (request.annualInterestRate()==null) throw new IllegalArgumentException("Interest rate is required for approval");
            loan.approve(request.annualInterestRate(),officerEmail,request.note());
            // Credit and loan status commit together. Any failure rolls the entire decision back.
            bankingService.deposit(loan.getLinkedAccount().getAccountNumber(),loan.getPrincipalAmount(),
                "Loan disbursement " + loan.getLoanNumber(),officerEmail);
            auditLogService.record(officerEmail,"LOAN_APPROVED_AND_DISBURSED","LOAN",loan.getLoanNumber(),"Annual rate="+loan.getAnnualInterestRate());
            notificationService.notifyUser(loan.getApplicant(),"Loan approved","Your loan " + loan.getLoanNumber() + " has been approved and disbursed. EMI: BDT " + loan.getMonthlyInstallment());
        } else {
            loan.reject(officerEmail,request.note());
            auditLogService.record(officerEmail,"LOAN_REJECTED","LOAN",loan.getLoanNumber(),request.note());
            notificationService.notifyUser(loan.getApplicant(),"Loan application rejected","Your loan " + loan.getLoanNumber() + " was rejected. Reason: " + request.note());
        }
        return LoanResponse.from(loanRepository.save(loan));
    }

    @Transactional
    public LoanRepaymentResponse repay(String email,Long loanId,LoanRepaymentRequest request) {
        if(request==null) throw new IllegalArgumentException("Repayment details are required");
        BankUser user=userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found"));
        Loan loan=loanRepository.findByIdForUpdate(loanId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan not found"));
        if(!loan.getApplicant().getId().equals(user.getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan not found");
        Account paying=accountRepository.findByAccountNumberAndOwner_Id(request.accountNumber().trim(),user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Paying account not found"));
        if(paying.getStatus()!=AccountStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT,"Paying account is not active");
        int installment=repaymentRepository.findByLoan_IdOrderByInstallmentNumberAsc(loanId).size()+1;
        loan.recordRepayment(request.amount(), installment);
        bankingService.withdraw(paying.getAccountNumber(),request.amount(),"Loan repayment " + loan.getLoanNumber(),email);
        LoanRepayment saved=repaymentRepository.save(new LoanRepayment(loan,paying,request.amount(),installment));
        loanRepository.save(loan);
        auditLogService.record(email,"LOAN_REPAYMENT","LOAN",loan.getLoanNumber(),"Installment="+installment+", amount="+request.amount());
        notificationService.notifyUser(user,"Loan repayment received","Installment " + installment + " received for loan " + loan.getLoanNumber());
        return LoanRepaymentResponse.from(saved);
    }

    @Transactional(readOnly=true)
    public List<LoanRepaymentResponse> repayments(String email,Long loanId) {
        LoanResponse ignored=getMyLoan(email,loanId);
        return repaymentRepository.findByLoan_IdOrderByInstallmentNumberAsc(ignored.id()).stream().map(LoanRepaymentResponse::from).toList();
    }

    private Pageable pageable(int page,int size) {
        if(page<0 || size<1 || size>MAX_PAGE_SIZE) throw new IllegalArgumentException("Page must be >= 0 and size must be 1 to 100");
        return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"submittedAt"));
    }
}
