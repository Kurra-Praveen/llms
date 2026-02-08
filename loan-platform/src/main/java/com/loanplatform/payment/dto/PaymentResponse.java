package com.loanplatform.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID tenantId;
    private UUID loanId;
    private String loanNumber;

    private String paymentNumber;
    private LocalDate paymentDate;
    private Instant paymentTime;

    private BigDecimal amountPaid;
    private String paymentMethod;
    private String referenceNumber;
    private String transactionId;

    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal penaltyPaid;
    private BigDecimal feePaid;
    private BigDecimal excessAmount;

    private String receiptNumber;
    private Boolean receiptGenerated;

    private String status;
    private Boolean isReversed;
    private Instant reversedAt;
    private String reversalReason;

    private String notes;
    private Map<String, Object> metadata;

    private Instant createdAt;

    private List<AllocationDetail> allocations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationDetail {
        private Integer installmentNumber;
        private LocalDate dueDate;
        private BigDecimal principalAllocated;
        private BigDecimal interestAllocated;
        private BigDecimal penaltyAllocated;
        private BigDecimal totalAllocated;
    }
}
