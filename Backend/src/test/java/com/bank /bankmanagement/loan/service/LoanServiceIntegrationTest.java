package com.bank.bankmanagement.loan.service;

import com.bank.bankmanagement.account.entity.SavingsAccount;
import com.bank.bankmanagement.account.repository.AccountRepository;
import com.bank.bankmanagement.common.notification.NotificationRepository;
import com.bank.bankmanagement.loan.dto.*;
import com.bank.bankmanagement.loan.entity.Loan;
import com.bank.bankmanagement.loan.enums.LoanStatus;
import com.bank.bankmanagement.loan.enums.LoanType;
import com.bank.bankmanagement.loan.repository.LoanRepository;
import com.bank.bankmanagement.loan.repository.LoanRepaymentRepository;
import com.bank.bankmanagement.transaction.repository.TransactionRepository;
import com.bank.bankmanagement.user.entity.BankUser;
import com.bank.bankmanagement.user.enums.KycStatus;
import com.bank.bankmanagement.user.enums.UserRole;
import com.bank.bankmanagement.user.repository.BankUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoanServiceIntegrationTest {
    @Autowired private LoanService service;
    @Autowired private BankUserRepository users;
    @Autowired private AccountRepository accounts;
    @Autowired private LoanRepository loans;
    @Autowired private LoanRepaymentRepository repayments;
    @Autowired private TransactionRepository transactions;
    @Autowired private NotificationRepository notifications;
    private BankUser customer; private String accountNumber; private Long loanId;

    @BeforeEach void setUp(){
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,18);
        customer=users.saveAndFlush(new BankUser("Loan Integration Customer","loan-"+suffix+"@example.test","hash",UserRole.CUSTOMER));
        customer.updateKycStatus(KycStatus.VERIFIED);customer=users.saveAndFlush(customer);
        accountNumber="42"+suffix.substring(0,12).toUpperCase();
        accounts.saveAndFlush(new SavingsAccount(accountNumber,customer,new BigDecimal("10000.00"),new BigDecimal("500.00"),new BigDecimal("5.00")));
    }
    @AfterEach void cleanup(){
        if(loanId!=null){repayments.deleteAllByLoanId(loanId);loans.findById(loanId).ifPresent(loans::delete);}
        transactions.deleteAllForAccountNumbers(List.of(accountNumber));
        accounts.findByAccountNumber(accountNumber).ifPresent(accounts::delete);
        if(customer!=null){notifications.deleteAllByUserId(customer.getId());users.deleteById(customer.getId());}
    }
    private LoanApplicationRequest request(){return new LoanApplicationRequest(LoanType.PERSONAL,"Education expenses",new BigDecimal("3000.00"),3,accountNumber);}

    @Test void customerCanApplyAndViewOwnLoan(){
        LoanResponse response=service.apply(customer.getEmail(),request());loanId=response.id();
        assertEquals(LoanStatus.PENDING,response.status());
        assertEquals(1,service.myLoans(customer.getEmail(),0,10).getTotalElements());
    }
    @Test void pendingKycCustomerCannotApply(){
        customer.updateKycStatus(KycStatus.PENDING);users.saveAndFlush(customer);
        ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.apply(customer.getEmail(),request()));
        assertEquals(HttpStatus.FORBIDDEN,ex.getStatusCode());
    }
    @Test void approvalDisbursesPrincipalAndSetsEmi(){
        LoanResponse pending=service.apply(customer.getEmail(),request());loanId=pending.id();
        LoanResponse result=service.decide(loanId,"loan-officer@example.test",new LoanDecisionRequest(true,BigDecimal.ZERO,"Approved"));
        assertEquals(LoanStatus.DISBURSED,result.status());
        assertEquals(new BigDecimal("1000.00"),result.monthlyInstallment());
        assertEquals(new BigDecimal("13000.00"),accounts.findByAccountNumber(accountNumber).orElseThrow().getBalance());
    }
    @Test void customerCanRepayOneInstallment(){
        LoanResponse pending=service.apply(customer.getEmail(),request());loanId=pending.id();
        service.decide(loanId,"loan-officer@example.test",new LoanDecisionRequest(true,BigDecimal.ZERO,"Approved"));
        LoanRepaymentResponse paid=service.repay(customer.getEmail(),loanId,new LoanRepaymentRequest(accountNumber,new BigDecimal("1000.00")));
        assertEquals(new BigDecimal("1000.00"),paid.amount());
        assertEquals(new BigDecimal("12000.00"),accounts.findByAccountNumber(accountNumber).orElseThrow().getBalance());
        assertEquals(new BigDecimal("2000.00"),service.getMyLoan(customer.getEmail(),loanId).outstandingAmount());
    }
    @Test void rejectionKeepsLoanUndisbursed(){
        LoanResponse pending=service.apply(customer.getEmail(),request());loanId=pending.id();
        LoanResponse rejected=service.decide(loanId,"loan-officer@example.test",new LoanDecisionRequest(false,null,"Incomplete documents"));
        assertEquals(LoanStatus.REJECTED,rejected.status());
        assertEquals(new BigDecimal("10000.00"),accounts.findByAccountNumber(accountNumber).orElseThrow().getBalance());
    }
}
