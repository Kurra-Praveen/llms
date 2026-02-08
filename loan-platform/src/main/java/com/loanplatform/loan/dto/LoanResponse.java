package com.loanplatform.loan.dto;

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
public class LoanResponse {

    private UUID id;
    private UUID tenantId;
    private UUID borrowerId;
    private String borrowerName;
    private String borrowerCode;
    private UUID loanProductId;
    private String loanNumber;

    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private String interestType;
    private Integer tenureMonths;
    private String repaymentFrequency;

    private BigDecimal processingFee;
    private BigDecimal otherCharges;
    private BigDecimal dailyFixedAmount;  // Fixed rupee amount per day for DAILY_FIXED interest type
    private Boolean deductChargesUpfront;
    private BigDecimal totalChargesDeducted;
    private BigDecimal netDisbursementAmount;

    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate firstPaymentDate;
    private LocalDate maturityDate;
    private LocalDate closureDate;

    private BigDecimal emiAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalPayable;

    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private BigDecimal outstandingPenalty;
    private BigDecimal totalOutstanding;

    private BigDecimal totalPaid;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal penaltyPaid;

    private String status;
    private Integer dpd;
    private String dpdBucket;
    private Boolean isNpa;

    private Boolean requiresApproval;
    private UUID approvedBy;
    private String approvalNotes;

    private Boolean hasCollateral;
    private String collateralType;
    private BigDecimal collateralValue;
    private String collateralDescription;

    private String penaltyType;
    private BigDecimal penaltyRate;
    private Integer gracePeriodDays;

    private String notes;
    private Map<String, Object> metadata;

    private Instant createdAt;
    private Instant updatedAt;

    private List<ScheduleResponse> schedules;
}
