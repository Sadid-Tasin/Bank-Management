package com.bank.bankmanagement.common.notification;
import java.time.LocalDateTime;
public record NotificationResponse(Long id,String title,String message,boolean read,LocalDateTime createdAt,LocalDateTime readAt){public static NotificationResponse from(BankNotification n){return new NotificationResponse(n.getId(),n.getTitle(),n.getMessage(),n.isRead(),n.getCreatedAt(),n.getReadAt());}}
