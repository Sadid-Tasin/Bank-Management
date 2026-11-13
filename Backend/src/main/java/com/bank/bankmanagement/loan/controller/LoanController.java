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
import java.util.List;
@RestController @RequestMapping("/api/v1/loans") @PreAuthorize("hasRole('CUSTOMER')")
public class LoanController {
    private final LoanService service;
    public LoanController(LoanService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public LoanResponse apply(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody LoanApplicationRequest request){return service.apply(jwt.getSubject(),request);}
    @GetMapping public Page<LoanResponse> myLoans(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.myLoans(jwt.getSubject(),page,size);}
    @GetMapping("/{loanId}") public LoanResponse get(@AuthenticationPrincipal Jwt jwt,@PathVariable Long loanId){return service.getMyLoan(jwt.getSubject(),loanId);}
    @GetMapping("/{loanId}/repayments") public List<LoanRepaymentResponse> repayments(@AuthenticationPrincipal Jwt jwt,@PathVariable Long loanId){return service.repayments(jwt.getSubject(),loanId);}
    @PostMapping("/{loanId}/repayments") @ResponseStatus(HttpStatus.CREATED) public LoanRepaymentResponse repay(@AuthenticationPrincipal Jwt jwt,@PathVariable Long loanId,@Valid @RequestBody LoanRepaymentRequest request){return service.repay(jwt.getSubject(),loanId,request);}
}
