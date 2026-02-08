package com.loanplatform.tenant.repository;

import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByBusinessCode(String businessCode);

    boolean existsByBusinessCode(String businessCode);

    boolean existsByContactEmail(String contactEmail);

    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE " +
           "LOWER(t.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.businessCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Tenant> searchTenants(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status")
    long countByStatus(@Param("status") TenantStatus status);
}
