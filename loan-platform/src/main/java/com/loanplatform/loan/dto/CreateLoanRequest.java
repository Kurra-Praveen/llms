package com.loanplatform.loan.dto;

import com.loanplatform.loan.entity.InterestType;
import com.loanplatform.loan.entity.RepaymentFrequency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanRequest {

    @NotNull(message = "Borrower ID is required")
    private UUID borrowerId;

    private UUID loanProductId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "100", message = "Principal must be at least 100")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0", message = "Interest rate must be non-negative")
    @DecimalMax(value = "100", message = "Interest rate must be at most 100")
    private BigDecimal interestRate;

    @NotNull(message = "Interest type is required")
    private InterestType interestType;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 360, message = "Tenure must be at most 360 months")
    private Integer tenureMonths;

    @Builder.Default
    private RepaymentFrequency repaymentFrequency = RepaymentFrequency.MONTHLY;

    @DecimalMin(value = "0", message = "Processing fee must be non-negative")
    private BigDecimal processingFee;

    @DecimalMin(value = "0", message = "Other charges must be non-negative")
    private BigDecimal otherCharges;

    @DecimalMin(value = "0", message = "Daily fixed amount must be non-negative")
    private BigDecimal dailyFixedAmount;  // Required when interestType is DAILY_FIXED

    @Builder.Default
    private Boolean deductChargesUpfront = true;

    private LocalDate applicationDate;

    private LocalDate firstPaymentDate;

    private Boolean requiresApproval;

    private Boolean hasCollateral;
    private String collateralType;
    private BigDecimal collateralValue;
    private String collateralDescription;

    private String penaltyType;
    private BigDecimal penaltyRate;
    private Integer gracePeriodDays;

    private String notes;
    private Map<String, Object> metadata;
}
