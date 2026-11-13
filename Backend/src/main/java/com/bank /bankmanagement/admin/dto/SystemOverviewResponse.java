package com.bank.bankmanagement.admin.dto;
import java.math.BigDecimal;
public record SystemOverviewResponse(long totalCustomers,long activeAccounts,BigDecimal totalAccountBalance,
 long pendingLoans,long openFraudAlerts,BigDecimal depositsLast30Days,BigDecimal withdrawalsLast30Days){}
