package com.bank.bankmanagement.admin.dto;

import java.math.BigDecimal;

/** One day of bank-wide activity for the dashboard chart. date is ISO yyyy-MM-dd. */
public record DailyActivityResponse(String date, BigDecimal deposits, BigDecimal withdrawals) {
}
