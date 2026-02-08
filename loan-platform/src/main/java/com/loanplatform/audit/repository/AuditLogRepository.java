package com.loanplatform.audit.repository;

import com.loanplatform.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<AuditLog> findByTenantIdAndDateRange(@Param("tenantId") UUID tenantId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.action = :action ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantIdAndAction(@Param("tenantId") UUID tenantId, @Param("action") String action, Pageable pageable);
}
