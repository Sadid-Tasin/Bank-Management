package com.bank.bankmanagement.common.notification;
import com.bank.bankmanagement.user.entity.BankUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="bank_notifications")
public class BankNotification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private BankUser user;
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=500) private String message;
 @Column(name="is_read",nullable=false) private boolean read;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="read_at") private LocalDateTime readAt;
 protected BankNotification(){}
 public BankNotification(BankUser user,String title,String message){this.user=user;this.title=title;this.message=message;this.createdAt=LocalDateTime.now();}
 public void markRead(){if(!read){read=true;readAt=LocalDateTime.now();}}
 public Long getId(){return id;} public BankUser getUser(){return user;} public String getTitle(){return title;} public String getMessage(){return message;} public boolean isRead(){return read;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getReadAt(){return readAt;}
}
