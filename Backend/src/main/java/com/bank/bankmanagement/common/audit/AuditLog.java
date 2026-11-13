package com.bank.bankmanagement.common.audit;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="audit_logs")
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="actor_email",length=150) private String actorEmail;
 @Column(nullable=false,length=60) private String action;
 @Column(name="entity_type",nullable=false,length=60) private String entityType;
 @Column(name="entity_id",length=80) private String entityId;
 @Column(length=1000) private String details;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 protected AuditLog(){}
 public AuditLog(String actor,String action,String type,String id,String details){this.actorEmail=actor;this.action=action;this.entityType=type;this.entityId=id;this.details=details;this.createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getActorEmail(){return actorEmail;} public String getAction(){return action;} public String getEntityType(){return entityType;} public String getEntityId(){return entityId;} public String getDetails(){return details;} public LocalDateTime getCreatedAt(){return createdAt;}
}
