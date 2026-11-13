package com.bank.bankmanagement.common.interest;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.Modifying;import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;import org.springframework.transaction.annotation.Transactional;
public interface InterestPostingRepository extends JpaRepository<InterestPosting,Long>{boolean existsByAccount_IdAndPostingKey(Long accountId,String postingKey);long countByPostingKeyStartingWith(String prefix);
 @Modifying @Transactional @Query("DELETE FROM InterestPosting p WHERE p.account.id = :accountId") void deleteAllByAccountId(@Param("accountId") Long accountId);}
