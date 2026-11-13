package com.bank.bankmanagement.admin.controller;
import com.bank.bankmanagement.admin.dto.*;import com.bank.bankmanagement.admin.service.ReportingService;import com.bank.bankmanagement.common.audit.*;import com.bank.bankmanagement.common.fraud.*;import com.bank.bankmanagement.common.interest.InterestPostingService;import java.time.YearMonth;import jakarta.validation.constraints.*;import org.springframework.data.domain.Page;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminReportingController{
 private final ReportingService reports;private final AuditLogService audit;private final FraudAlertService fraud;private final InterestPostingService interest;
 public AdminReportingController(ReportingService r,AuditLogService a,FraudAlertService f,InterestPostingService i){reports=r;audit=a;fraud=f;interest=i;}
 @GetMapping("/reports/overview") public SystemOverviewResponse overview(){return reports.overview();}
 @GetMapping("/reports/transactions") public Page<AdminTransactionResponse> transactions(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return reports.transactions(page,size);}
 @GetMapping("/audit-logs") public Page<AuditLog> audit(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return audit.latest(page,size);}
 @GetMapping("/fraud-alerts") public Page<FraudAlertResponse> alerts(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return fraud.open(page,size);}
 @PostMapping("/interest/post") public java.util.Map<String,Integer> postInterest(@RequestParam String period){
  YearMonth parsed; try { parsed = YearMonth.parse(period); }
  catch (java.time.DateTimeException ex) { throw new IllegalArgumentException("Period must use YYYY-MM format"); }
  int posted=interest.postMonthlyInterest(parsed);return java.util.Map.of("accountsCredited",posted);
 }
 @PatchMapping("/fraud-alerts/{id}/resolve") public FraudAlertResponse resolve(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt){return fraud.resolve(id,jwt.getSubject());}
}
