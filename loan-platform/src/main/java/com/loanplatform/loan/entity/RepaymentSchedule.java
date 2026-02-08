package com.loanplatform.loan.entity;

import com.loanplatform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "repayment_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class RepaymentSchedule extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_component", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 18, scale = 2)
    private BigDecimal interestComponent;

    @Column(name = "installment_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "outstanding_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal outstandingAfter;

    @Column(name = "principal_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "penalty_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "penalty_calculated_at")
    private java.time.Instant penaltyCalculatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "days_past_due")
    @Builder.Default
    private Integer daysPastDue = 0;

    public BigDecimal getOutstandingAmount() {
        return installmentAmount.add(penaltyAmount)
                .subtract(totalPaid);
    }

    public BigDecimal getPrincipalOutstanding() {
        return principalComponent.subtract(principalPaid);
    }

    public BigDecimal getInterestOutstanding() {
        return interestComponent.subtract(interestPaid);
    }

    public BigDecimal getPenaltyOutstanding() {
        return penaltyAmount.subtract(penaltyPaid);
    }

    public boolean isFullyPaid() {
        return getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isOverdue(LocalDate asOfDate) {
        return !isFullyPaid() && dueDate.isBefore(asOfDate);
    }

    public void applyPayment(BigDecimal principal, BigDecimal interest, BigDecimal penalty) {
        this.principalPaid = this.principalPaid.add(principal);
        this.interestPaid = this.interestPaid.add(interest);
        this.penaltyPaid = this.penaltyPaid.add(penalty);
        this.totalPaid = this.principalPaid.add(this.interestPaid).add(this.penaltyPaid);

        if (isFullyPaid()) {
            this.status = ScheduleStatus.PAID;
            this.paidDate = LocalDate.now();
        } else if (this.totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.status = ScheduleStatus.PARTIAL;
        }
    }
}
