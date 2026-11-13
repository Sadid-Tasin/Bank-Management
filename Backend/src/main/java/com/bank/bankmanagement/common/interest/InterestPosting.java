package com.bank.bankmanagement.common.interest;
import com.bank.bankmanagement.account.entity.Account;import jakarta.persistence.*;import java.math.BigDecimal;import java.time.LocalDateTime;
@Entity @Table(name="interest_postings",uniqueConstraints=@UniqueConstraint(name="uk_interest_account_period",columnNames={"account_id","posting_key"}))
public class InterestPosting{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="account_id",nullable=false) private Account account;
 @Column(name="posting_key",nullable=false,length=40) private String postingKey;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 @Column(name="posted_at",nullable=false,updatable=false) private LocalDateTime postedAt;
 protected InterestPosting(){}
 public InterestPosting(Account a,String key,BigDecimal amount){account=a;postingKey=key;this.amount=amount;postedAt=LocalDateTime.now();}
 public Long getId(){return id;}public Account getAccount(){return account;}public String getPostingKey(){return postingKey;}public BigDecimal getAmount(){return amount;}public LocalDateTime getPostedAt(){return postedAt;}
}
