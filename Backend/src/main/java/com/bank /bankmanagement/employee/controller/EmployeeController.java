package com.bank.bankmanagement.employee.controller;

import com.bank.bankmanagement.account.dto.AccountResponse;
import com.bank.bankmanagement.banking.dto.BankingOperationRequest;
import com.bank.bankmanagement.employee.dto.EmployeeCustomerResponse;
import com.bank.bankmanagement.employee.dto.KycDecisionRequest;
import com.bank.bankmanagement.employee.dto.KycReviewResponse;
import com.bank.bankmanagement.employee.service.EmployeeService;
import com.bank.bankmanagement.transaction.entity.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('TELLER','LOAN_OFFICER','MANAGER','ADMIN')")
    public Page<EmployeeCustomerResponse> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return employeeService.listCustomers(page, size);
    }

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public Page<KycReviewResponse> listPendingKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return employeeService.listPendingKyc(page, size);
    }

    @GetMapping("/kyc/{customerId}")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public KycReviewResponse getCustomerKyc(@PathVariable Long customerId) {
        return employeeService.getCustomerKyc(customerId);
    }

    @PutMapping("/kyc/{customerId}")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public KycReviewResponse reviewKyc(
            @PathVariable Long customerId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody KycDecisionRequest request) {
        return employeeService.reviewKyc(customerId, jwt.getSubject(), request);
    }

    @PostMapping("/counter/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public TransactionResponse counterDeposit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BankingOperationRequest request) {
        return employeeService.counterDeposit(jwt.getSubject(), request);
    }

    @PostMapping("/counter/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN')")
    public TransactionResponse counterWithdraw(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BankingOperationRequest request) {
        return employeeService.counterWithdraw(jwt.getSubject(), request);
    }

    @PutMapping("/accounts/{accountNumber}/freeze")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public AccountResponse freezeAccount(@PathVariable String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {
        return employeeService.freezeAccount(accountNumber, jwt.getSubject());
    }

    @PutMapping("/accounts/{accountNumber}/unfreeze")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public AccountResponse unfreezeAccount(@PathVariable String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {
        return employeeService.unfreezeAccount(accountNumber, jwt.getSubject());
    }
}
