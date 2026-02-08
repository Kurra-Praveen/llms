package com.loanplatform.notification.repository;

import com.loanplatform.notification.entity.Notification;
import com.loanplatform.notification.entity.NotificationStatus;
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
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId ORDER BY n.createdAt DESC")
    Page<Notification> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND (n.scheduledFor IS NULL OR n.scheduledFor <= :now) ORDER BY n.createdAt")
    List<Notification> findPendingNotifications(@Param("now") Instant now);

    @Query("SELECT n FROM Notification n WHERE n.loanId = :loanId ORDER BY n.createdAt DESC")
    List<Notification> findByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("recipientId") UUID recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries ORDER BY n.failedAt")
    List<Notification> findFailedForRetry(@Param("maxRetries") int maxRetries);
}
