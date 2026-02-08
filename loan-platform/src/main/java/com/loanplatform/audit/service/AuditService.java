package com.loanplatform.audit.service;

import com.loanplatform.audit.entity.AuditLog;
import com.loanplatform.audit.repository.AuditLogRepository;
import com.loanplatform.common.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            UUID userId,
            String ipAddress,
            String userAgent) {

        UUID tenantId = TenantContext.hasTenant() ? TenantContext.getCurrentTenant() : null;

        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} on {} {}", action, entityType, entityId);
    }

    public void logCreate(String entityType, UUID entityId, Map<String, Object> values, UUID userId) {
        logAction("CREATE", entityType, entityId, null, values, userId, null, null);
    }

    public void logUpdate(String entityType, UUID entityId, Map<String, Object> oldValues, Map<String, Object> newValues, UUID userId) {
        logAction("UPDATE", entityType, entityId, oldValues, newValues, userId, null, null);
    }

    public void logDelete(String entityType, UUID entityId, Map<String, Object> oldValues, UUID userId) {
        logAction("DELETE", entityType, entityId, oldValues, null, userId, null, null);
    }

    public void logLogin(UUID userId, String ipAddress, String userAgent) {
        logAction("LOGIN", "USER", userId, null, null, userId, ipAddress, userAgent);
    }

    public void logLogout(UUID userId) {
        logAction("LOGOUT", "USER", userId, null, null, userId, null, null);
    }

    public void logPaymentReceived(UUID loanId, UUID paymentId, Map<String, Object> details, UUID userId) {
        logAction("PAYMENT_RECEIVED", "LOAN", loanId, null, details, userId, null, null);
    }

    public void logLoanDisbursed(UUID loanId, Map<String, Object> details, UUID userId) {
        logAction("LOAN_DISBURSED", "LOAN", loanId, null, details, userId, null, null);
    }

    public void logPaymentReversed(UUID paymentId, Map<String, Object> details, UUID userId) {
        logAction("PAYMENT_REVERSED", "PAYMENT", paymentId, null, details, userId, null, null);
    }
}
