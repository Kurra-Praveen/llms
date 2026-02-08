package com.loanplatform.ledger.repository;

import com.loanplatform.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("SELECT le FROM LedgerEntry le WHERE le.transactionId = :transactionId")
    List<LedgerEntry> findByTransactionId(@Param("transactionId") UUID transactionId);

    @Query("SELECT le FROM LedgerEntry le WHERE le.accountId = :accountId ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) " +
           "FROM LedgerEntry le WHERE le.accountId = :accountId")
    BigDecimal getAccountBalance(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) " +
           "FROM LedgerEntry le WHERE le.accountId = :accountId")
    BigDecimal getTotalDebits(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) " +
           "FROM LedgerEntry le WHERE le.accountId = :accountId")
    BigDecimal getTotalCredits(@Param("accountId") UUID accountId);
}
