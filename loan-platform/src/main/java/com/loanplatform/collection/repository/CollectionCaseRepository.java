package com.loanplatform.collection.repository;

import com.loanplatform.collection.entity.CollectionCase;
import com.loanplatform.collection.entity.CollectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionCaseRepository extends JpaRepository<CollectionCase, UUID> {

    @Query("SELECT c FROM CollectionCase c WHERE c.tenantId = :tenantId ORDER BY c.createdAt DESC")
    Page<CollectionCase> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM CollectionCase c WHERE c.tenantId = :tenantId AND c.status = :status ORDER BY c.createdAt DESC")
    Page<CollectionCase> findByTenantIdAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") CollectionStatus status,
            Pageable pageable);

    @Query("SELECT c FROM CollectionCase c WHERE c.tenantId = :tenantId AND c.dpdBucket = :bucket AND c.status != 'RESOLVED' ORDER BY c.createdAt DESC")
    Page<CollectionCase> findByTenantIdAndDpdBucket(
            @Param("tenantId") UUID tenantId,
            @Param("bucket") String bucket,
            Pageable pageable);

    @Query("SELECT c FROM CollectionCase c WHERE c.tenantId = :tenantId AND c.assignedTo = :userId AND c.status != 'RESOLVED' ORDER BY c.createdAt DESC")
    Page<CollectionCase> findByTenantIdAndAssignedTo(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            Pageable pageable);

    Optional<CollectionCase> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<CollectionCase> findByLoanIdAndTenantId(UUID loanId, UUID tenantId);

    @Query("SELECT c FROM CollectionCase c WHERE c.loanId = :loanId AND c.status != 'RESOLVED'")
    Optional<CollectionCase> findActiveByLoanId(@Param("loanId") UUID loanId);

    boolean existsByLoanIdAndStatusNot(UUID loanId, CollectionStatus status);

    @Query("SELECT COUNT(c) FROM CollectionCase c WHERE c.tenantId = :tenantId AND c.status = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") CollectionStatus status);

    @Query("SELECT COUNT(c) FROM CollectionCase c WHERE c.tenantId = :tenantId AND c.dpdBucket = :bucket AND c.status != 'RESOLVED'")
    long countByTenantIdAndDpdBucket(@Param("tenantId") UUID tenantId, @Param("bucket") String bucket);
}
