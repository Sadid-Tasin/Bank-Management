package com.bank.bankmanagement.account.controller;

import com.bank.bankmanagement.account.dto.AccountResponse;
import com.bank.bankmanagement.account.dto.BalanceResponse;
import com.bank.bankmanagement.account.dto.CreateAccountRequest;
import com.bank.bankmanagement.account.service.AccountService;
import com.bank.bankmanagement.transaction.entity.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@PreAuthorize("hasRole('CUSTOMER')")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(jwt.getSubject(), request);
    }

    @GetMapping
    public List<AccountResponse> getMyAccounts(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getMyAccounts(jwt.getSubject());
    }

    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse getBalance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String accountNumber) {
        return accountService.getBalance(jwt.getSubject(), accountNumber);
    }

    @GetMapping("/{accountNumber}/transactions")
    public Page<TransactionResponse> getTransactionHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return accountService.getTransactionHistory(
                jwt.getSubject(), accountNumber, page, size);
    }

    @GetMapping("/{accountNumber}/mini-statement")
    public List<TransactionResponse> getMiniStatement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String accountNumber) {
        return accountService.getMiniStatement(jwt.getSubject(), accountNumber);
    }
}
