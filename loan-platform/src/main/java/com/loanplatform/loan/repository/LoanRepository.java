package com.loanplatform.loan.repository;

import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false")
    Page<Loan> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.id = :id AND l.deleted = false")
    Optional<Loan> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.loanNumber = :loanNumber AND l.deleted = false")
    Optional<Loan> findByLoanNumberAndTenantId(@Param("loanNumber") String loanNumber, @Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.borrowerId = :borrowerId AND l.deleted = false")
    Page<Loan> findByTenantIdAndBorrowerId(@Param("tenantId") UUID tenantId, @Param("borrowerId") UUID borrowerId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.status = :status AND l.deleted = false")
    Page<Loan> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.status IN :statuses AND l.deleted = false")
    Page<Loan> findByTenantIdAndStatusIn(@Param("tenantId") UUID tenantId, @Param("statuses") List<LoanStatus> statuses, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.dpd > 0 AND l.status = 'ACTIVE' AND l.deleted = false ORDER BY l.dpd DESC")
    Page<Loan> findOverdueLoans(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false AND " +
           "(LOWER(l.loanNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Loan> searchLoans(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByLoanNumberAndTenantIdAndDeletedFalse(String loanNumber, UUID tenantId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.tenantId = :tenantId AND l.status = :status AND l.deleted = false")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") LoanStatus status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.borrowerId = :borrowerId AND l.status = 'ACTIVE' AND l.deleted = false")
    long countActiveLoansForBorrower(@Param("borrowerId") UUID borrowerId);

    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal + l.outstandingInterest + l.outstandingPenalty), 0) " +
           "FROM Loan l WHERE l.borrowerId = :borrowerId AND l.status = 'ACTIVE' AND l.deleted = false")
    BigDecimal getTotalOutstandingForBorrower(@Param("borrowerId") UUID borrowerId);

    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal + l.outstandingInterest + l.outstandingPenalty), 0) " +
           "FROM Loan l WHERE l.tenantId = :tenantId AND l.status = 'ACTIVE' AND l.deleted = false")
    BigDecimal getTotalOutstandingForTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.deleted = false")
    List<Loan> findAllActiveLoans();

    @Query("SELECT new com.loanplatform.report.dto.LoansByStatusResponse(l.status, COUNT(l), SUM(l.principalAmount)) " +
           "FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false GROUP BY l.status")
    List<com.loanplatform.report.dto.LoansByStatusResponse> countLoansByStatus(@Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false AND l.status IN :statuses")
    BigDecimal sumPrincipalByStatusIn(@Param("tenantId") UUID tenantId, @Param("statuses") List<LoanStatus> statuses);

    @Query("SELECT SUM(l.outstandingPrincipal) FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false AND l.status = 'ACTIVE'")
    BigDecimal sumOutstandingPrincipalActive(@Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(l.totalPaid) FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false")
    BigDecimal sumTotalPaid(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false AND l.status = 'ACTIVE' AND l.dpd >= :minDpd")
    long countActiveLoansWithDpdAtLeast(@Param("tenantId") UUID tenantId, @Param("minDpd") int minDpd);

    @Query("SELECT SUM(l.emiAmount) FROM Loan l WHERE l.tenantId = :tenantId AND l.deleted = false AND l.status = 'ACTIVE'")
    BigDecimal sumEmiAmountActive(@Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(l.outstandingInterest + l.outstandingPenalty) FROM Loan l " +
           "WHERE l.tenantId = :tenantId AND l.deleted = false AND l.status = 'ACTIVE' AND l.dpd > 0")
    BigDecimal sumOverdueAmount(@Param("tenantId") UUID tenantId);
}
