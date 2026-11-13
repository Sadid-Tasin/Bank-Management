package com.bank.bankmanagement.banking.controller;

import com.bank.bankmanagement.banking.dto.BankingOperationRequest;
import com.bank.bankmanagement.banking.dto.TransferRequest;
import com.bank.bankmanagement.banking.service.CustomerBankingService;
import com.bank.bankmanagement.transaction.entity.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/banking")
@PreAuthorize("hasRole('CUSTOMER')")
public class BankingController {

    private final CustomerBankingService customerBankingService;

    public BankingController(CustomerBankingService customerBankingService) {
        this.customerBankingService = customerBankingService;
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BankingOperationRequest request) {
        return TransactionResponse.from(customerBankingService.deposit(
                jwt.getSubject(), request.accountNumber(),
                request.amount(), request.description()));
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BankingOperationRequest request) {
        return TransactionResponse.from(customerBankingService.withdraw(
                jwt.getSubject(), request.accountNumber(),
                request.amount(), request.description()));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request) {
        return TransactionResponse.from(customerBankingService.transfer(
                jwt.getSubject(), request.sourceAccountNumber(),
                request.destinationAccountNumber(), request.amount(),
                request.description()));
    }
}
