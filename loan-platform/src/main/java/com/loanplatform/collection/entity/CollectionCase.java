package com.loanplatform.collection.entity;

import com.loanplatform.auth.entity.User;
import com.loanplatform.common.entity.BaseEntity;
import com.loanplatform.loan.entity.Loan;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "collection_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CollectionCase extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private Loan loan;

    @Column(name = "case_number", nullable = false)
    private String caseNumber;

    @Column(name = "dpd_bucket", nullable = false)
    private String dpdBucket;

    @Column(name = "dpd_days", nullable = false)
    private Integer dpdDays;

    @Column(name = "overdue_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal overdueAmount;

    @Column(name = "overdue_principal", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal overduePrincipal = BigDecimal.ZERO;

    @Column(name = "overdue_interest", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal overdueInterest = BigDecimal.ZERO;

    @Column(name = "overdue_penalty", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal overduePenalty = BigDecimal.ZERO;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", insertable = false, updatable = false)
    private User assignedUser;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CollectionStatus status = CollectionStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private CollectionPriority priority = CollectionPriority.MEDIUM;

    @Column(name = "resolution_type")
    private String resolutionType;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "ptp_date")
    private LocalDate ptpDate;

    @Column(name = "ptp_amount", precision = 18, scale = 2)
    private BigDecimal ptpAmount;

    @Column(name = "ptp_status")
    private String ptpStatus;

    @Column(name = "last_contact_date")
    private LocalDate lastContactDate;

    @Column(name = "next_action_date")
    private LocalDate nextActionDate;

    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    @OneToMany(mappedBy = "collectionCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CollectionActivity> activities = new ArrayList<>();

    public void assignTo(UUID userId) {
        this.assignedTo = userId;
        this.assignedAt = Instant.now();
        this.status = CollectionStatus.IN_PROGRESS;
    }

    public void escalate() {
        this.status = CollectionStatus.ESCALATED;
        this.priority = CollectionPriority.HIGH;
    }

    public void resolve(String resolutionType, String notes, UUID resolvedBy) {
        this.status = CollectionStatus.RESOLVED;
        this.resolutionType = resolutionType;
        this.resolutionNotes = notes;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
    }

    public void recordPtp(LocalDate date, BigDecimal amount) {
        this.ptpDate = date;
        this.ptpAmount = amount;
        this.ptpStatus = "PENDING";
    }

    public void updateFromLoan(int dpd, BigDecimal principal, BigDecimal interest, BigDecimal penalty) {
        this.dpdDays = dpd;
        this.overduePrincipal = principal;
        this.overdueInterest = interest;
        this.overduePenalty = penalty;
        this.overdueAmount = principal.add(interest).add(penalty);
        this.dpdBucket = calculateDpdBucket(dpd);
    }

    private String calculateDpdBucket(int dpd) {
        if (dpd <= 0) return "CURRENT";
        if (dpd <= 30) return "1-30";
        if (dpd <= 60) return "31-60";
        if (dpd <= 90) return "61-90";
        return "90+";
    }
}
