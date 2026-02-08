package com.loanplatform.ledger.repository;

import com.loanplatform.ledger.entity.LedgerTransaction;
import com.loanplatform.ledger.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    @Query("SELECT lt FROM LedgerTransaction lt WHERE lt.tenantId = :tenantId ORDER BY lt.transactionDate DESC, lt.createdAt DESC")
    Page<LedgerTransaction> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT lt FROM LedgerTransaction lt WHERE lt.tenantId = :tenantId AND lt.id = :id")
    Optional<LedgerTransaction> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT lt FROM LedgerTransaction lt WHERE lt.loanId = :loanId ORDER BY lt.transactionDate DESC, lt.createdAt DESC")
    List<LedgerTransaction> findByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT lt FROM LedgerTransaction lt WHERE lt.referenceType = :refType AND lt.referenceId = :refId")
    List<LedgerTransaction> findByReference(@Param("refType") String refType, @Param("refId") UUID refId);

    @Query("SELECT lt FROM LedgerTransaction lt WHERE lt.tenantId = :tenantId AND lt.transactionDate BETWEEN :startDate AND :endDate ORDER BY lt.transactionDate")
    List<LedgerTransaction> findByTenantIdAndDateRange(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT lt FROM LedgerTransaction lt WHERE lt.tenantId = :tenantId AND lt.transactionType = :type ORDER BY lt.transactionDate DESC")
    Page<LedgerTransaction> findByTenantIdAndType(@Param("tenantId") UUID tenantId, @Param("type") TransactionType type, Pageable pageable);

    @Query("SELECT COUNT(lt) FROM LedgerTransaction lt WHERE lt.tenantId = :tenantId AND lt.transactionDate = :date")
    long countByTenantIdAndDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}
