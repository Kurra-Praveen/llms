package com.loanplatform.ledger.repository;

import com.loanplatform.ledger.entity.AccountType;
import com.loanplatform.ledger.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    @Query("SELECT la FROM LedgerAccount la WHERE la.tenantId = :tenantId AND la.isActive = true")
    List<LedgerAccount> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT la FROM LedgerAccount la WHERE la.tenantId = :tenantId AND la.accountCode = :code")
    Optional<LedgerAccount> findByTenantIdAndAccountCode(@Param("tenantId") UUID tenantId, @Param("code") String code);

    @Query("SELECT la FROM LedgerAccount la WHERE la.tenantId = :tenantId AND la.accountType = :type AND la.isActive = true")
    List<LedgerAccount> findByTenantIdAndAccountType(@Param("tenantId") UUID tenantId, @Param("type") AccountType type);

    @Query("SELECT la FROM LedgerAccount la WHERE la.tenantId = :tenantId AND la.isSystemAccount = true")
    List<LedgerAccount> findSystemAccountsByTenantId(@Param("tenantId") UUID tenantId);
}
