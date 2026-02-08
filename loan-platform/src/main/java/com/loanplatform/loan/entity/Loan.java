package com.loanplatform.loan.entity;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Loan extends AuditableEntity {

    @Column(name = "borrower_id", nullable = false)
    private UUID borrowerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", insertable = false, updatable = false)
    private Borrower borrower;

    @Column(name = "loan_product_id")
    private UUID loanProductId;

    @Column(name = "loan_number", nullable = false)
    private String loanNumber;

    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false)
    private InterestType interestType;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_frequency", nullable = false)
    @Builder.Default
    private RepaymentFrequency repaymentFrequency = RepaymentFrequency.MONTHLY;

    @Column(name = "processing_fee", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "daily_fixed_amount", precision = 18, scale = 2)
    private BigDecimal dailyFixedAmount;  // Fixed rupee amount per day for DAILY_FIXED interest type

    @Column(name = "deduct_charges_upfront")
    @Builder.Default
    private Boolean deductChargesUpfront = true;

    @Column(name = "total_charges_deducted", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalChargesDeducted = BigDecimal.ZERO;

    @Column(name = "net_disbursement_amount", precision = 18, scale = 2)
    private BigDecimal netDisbursementAmount;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "closure_date")
    private LocalDate closureDate;

    @Column(name = "emi_amount", precision = 18, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "total_interest", precision = 18, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_payable", precision = 18, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "outstanding_principal", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Column(name = "outstanding_interest", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    @Column(name = "outstanding_penalty", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal outstandingPenalty = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "principal_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "penalty_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.DRAFT;

    @Column(name = "dpd")
    @Builder.Default
    private Integer dpd = 0;

    @Column(name = "dpd_bucket")
    private String dpdBucket;

    @Column(name = "is_npa")
    @Builder.Default
    private Boolean isNpa = false;

    @Column(name = "npa_date")
    private LocalDate npaDate;

    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approval_notes")
    private String approvalNotes;

    @Column(name = "has_collateral")
    @Builder.Default
    private Boolean hasCollateral = false;

    @Column(name = "collateral_type")
    private String collateralType;

    @Column(name = "collateral_value", precision = 18, scale = 2)
    private BigDecimal collateralValue;

    @Column(name = "collateral_description")
    private String collateralDescription;

    @Column(name = "penalty_type")
    private String penaltyType;

    @Column(name = "penalty_rate", precision = 8, scale = 4)
    private BigDecimal penaltyRate;

    @Column(name = "grace_period_days")
    @Builder.Default
    private Integer gracePeriodDays = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RepaymentSchedule> schedules = new ArrayList<>();

    public BigDecimal getTotalOutstanding() {
        return outstandingPrincipal.add(outstandingInterest).add(outstandingPenalty);
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.DISBURSED;
    }

    public boolean isClosed() {
        return status == LoanStatus.CLOSED;
    }

    public boolean canDisburse() {
        return status == LoanStatus.APPROVED ||
               (status == LoanStatus.DRAFT && !requiresApproval);
    }

    public void disburse(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
        this.outstandingPrincipal = this.principalAmount;

        // Calculate charges to be deducted upfront
        if (Boolean.TRUE.equals(this.deductChargesUpfront)) {
            BigDecimal charges = BigDecimal.ZERO;
            if (this.processingFee != null) {
                charges = charges.add(this.processingFee);
            }
            if (this.otherCharges != null) {
                charges = charges.add(this.otherCharges);
            }
            this.totalChargesDeducted = charges;
            this.netDisbursementAmount = this.principalAmount.subtract(charges);
        } else {
            this.totalChargesDeducted = BigDecimal.ZERO;
            this.netDisbursementAmount = this.principalAmount;
        }

        this.status = LoanStatus.ACTIVE;
    }

    public void close() {
        this.status = LoanStatus.CLOSED;
        this.closureDate = LocalDate.now();
    }
}
