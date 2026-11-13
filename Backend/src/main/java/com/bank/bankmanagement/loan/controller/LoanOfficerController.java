package com.bank.bankmanagement.loan.controller;
import com.bank.bankmanagement.loan.dto.*;
import com.bank.bankmanagement.loan.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/employee/loans")
@PreAuthorize("hasAnyRole('LOAN_OFFICER','MANAGER','ADMIN')")
public class LoanOfficerController {
    private final LoanService service; public LoanOfficerController(LoanService service){this.service=service;}
    @GetMapping("/pending") public Page<LoanResponse> pending(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.pendingLoans(page,size);}
    @PostMapping("/{loanId}/decision") @ResponseStatus(HttpStatus.OK)
    public LoanResponse decide(@PathVariable Long loanId,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody LoanDecisionRequest request){return service.decide(loanId,jwt.getSubject(),request);}
}
