package com.loanplatform.payment.repository;

import com.loanplatform.payment.entity.Payment;
import com.loanplatform.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId ORDER BY p.paymentDate DESC")
    Page<Payment> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<Payment> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.paymentNumber = :paymentNumber")
    Optional<Payment> findByPaymentNumberAndTenantId(@Param("paymentNumber") String paymentNumber, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.idempotencyKey = :idempotencyKey")
    Optional<Payment> findByIdempotencyKeyAndTenantId(@Param("idempotencyKey") String idempotencyKey, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Payment p WHERE p.loanId = :loanId AND p.isReversed = false ORDER BY p.paymentDate DESC")
    List<Payment> findByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT p FROM Payment p WHERE p.loanId = :loanId AND p.isReversed = false ORDER BY p.paymentDate DESC")
    Page<Payment> findByLoanId(@Param("loanId") UUID loanId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.paymentDate BETWEEN :startDate AND :endDate AND p.isReversed = false")
    List<Payment> findByTenantIdAndDateRange(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.loanId = :loanId AND p.isReversed = false")
    BigDecimal getTotalPaymentsForLoan(@Param("loanId") UUID loanId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.tenantId = :tenantId AND p.paymentDate = :date AND p.isReversed = false")
    long countByTenantIdAndDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    boolean existsByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);
}
