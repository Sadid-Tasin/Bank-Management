package com.bank.bankmanagement.loan.repository;
import com.bank.bankmanagement.loan.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;import org.springframework.data.jpa.repository.Modifying;import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;import org.springframework.transaction.annotation.Transactional;
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    List<LoanRepayment> findByLoan_IdOrderByInstallmentNumberAsc(Long loanId);
    boolean existsByReference(String reference);
    @Modifying @Transactional @Query("DELETE FROM LoanRepayment r WHERE r.loan.id = :loanId")
    void deleteAllByLoanId(@Param("loanId") Long loanId);
}
