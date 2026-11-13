package com.bank.bankmanagement.loan.repository;
import com.bank.bankmanagement.loan.entity.Loan;
import com.bank.bankmanagement.loan.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Page<Loan> findByApplicant_Id(Long applicantId, Pageable pageable);
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);
    Optional<Loan> findByIdAndApplicant_Id(Long id, Long applicantId);
    long countByStatus(LoanStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);
}
