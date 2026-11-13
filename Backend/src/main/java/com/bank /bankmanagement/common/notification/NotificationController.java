package com.bank.bankmanagement.common.notification;
import com.bank.bankmanagement.user.repository.BankUserRepository;import jakarta.validation.constraints.*;import org.springframework.data.domain.Page;import org.springframework.http.HttpStatus;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notifications") @PreAuthorize("isAuthenticated()")
public class NotificationController{
 private final NotificationService service;private final BankUserRepository users;public NotificationController(NotificationService s,BankUserRepository u){service=s;users=u;}
 @GetMapping public Page<NotificationResponse> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){var user=users.findByEmailIgnoreCase(jwt.getSubject()).orElseThrow(()->new IllegalArgumentException("User not found"));return service.list(jwt.getSubject(),user.getId(),page,size);}
 @PatchMapping("/{id}/read") public NotificationResponse read(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){var user=users.findByEmailIgnoreCase(jwt.getSubject()).orElseThrow(()->new IllegalArgumentException("User not found"));return service.markRead(id,user.getId());}
}
