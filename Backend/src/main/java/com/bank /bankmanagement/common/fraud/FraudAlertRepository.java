package com.bank.bankmanagement.common.fraud;
import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface FraudAlertRepository extends JpaRepository<FraudAlert,Long>{Page<FraudAlert> findByStatusOrderByCreatedAtDesc(String status,Pageable pageable);boolean existsByTransaction_Reference(String reference);long countByStatus(String status);}
