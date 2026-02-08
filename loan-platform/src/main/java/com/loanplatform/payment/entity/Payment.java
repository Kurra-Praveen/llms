package com.loanplatform.payment.entity;

import com.loanplatform.common.entity.BaseEntity;
import com.loanplatform.loan.entity.Loan;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Payment extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private Loan loan;

    @Column(name = "payment_number", nullable = false)
    private String paymentNumber;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_time")
    @Builder.Default
    private Instant paymentTime = Instant.now();

    @Column(name = "amount_paid", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "principal_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "penalty_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    @Column(name = "fee_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal feePaid = BigDecimal.ZERO;

    @Column(name = "excess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal excessAmount = BigDecimal.ZERO;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "receipt_generated")
    @Builder.Default
    private Boolean receiptGenerated = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.COMPLETED;

    @Column(name = "is_reversed")
    @Builder.Default
    private Boolean isReversed = false;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by")
    private UUID reversedBy;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @Column(name = "original_payment_id")
    private UUID originalPaymentId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public boolean isReversed() {
        return Boolean.TRUE.equals(isReversed);
    }

    public void reverse(UUID reversedByUserId, String reason) {
        this.isReversed = true;
        this.reversedAt = Instant.now();
        this.reversedBy = reversedByUserId;
        this.reversalReason = reason;
        this.status = PaymentStatus.REVERSED;
    }
}
