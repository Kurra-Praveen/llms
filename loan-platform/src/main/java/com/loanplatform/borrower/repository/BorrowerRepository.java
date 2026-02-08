package com.loanplatform.borrower.repository;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.entity.RiskBand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.deleted = false")
    Page<Borrower> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.id = :id AND b.deleted = false")
    Optional<Borrower> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.borrowerCode = :code AND b.deleted = false")
    Optional<Borrower> findByBorrowerCodeAndTenantId(@Param("code") String code, @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.phone = :phone AND b.deleted = false")
    Optional<Borrower> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.status = :status AND b.deleted = false")
    Page<Borrower> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") BorrowerStatus status, Pageable pageable);

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.riskBand = :riskBand AND b.deleted = false")
    Page<Borrower> findByTenantIdAndRiskBand(@Param("tenantId") UUID tenantId, @Param("riskBand") RiskBand riskBand, Pageable pageable);

    @Query("SELECT b FROM Borrower b WHERE b.tenantId = :tenantId AND b.deleted = false AND " +
           "(LOWER(b.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.borrowerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Borrower> searchBorrowers(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByBorrowerCodeAndTenantIdAndDeletedFalse(String borrowerCode, UUID tenantId);

    boolean existsByPhoneAndTenantIdAndDeletedFalse(String phone, UUID tenantId);

    boolean existsByIdNumberAndTenantIdAndDeletedFalse(String idNumber, UUID tenantId);

    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.tenantId = :tenantId AND b.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.tenantId = :tenantId AND b.status = :status AND b.deleted = false")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") BorrowerStatus status);
}
