package com.loanplatform.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "principal_allocated", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal principalAllocated = BigDecimal.ZERO;

    @Column(name = "interest_allocated", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestAllocated = BigDecimal.ZERO;

    @Column(name = "penalty_allocated", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAllocated = BigDecimal.ZERO;

    @Column(name = "total_allocated", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAllocated;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
