package com.loanplatform.collection.entity;

import com.loanplatform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "collection_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CollectionActivity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "collection_case_id", nullable = false)
    private UUID collectionCaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_case_id", insertable = false, updatable = false)
    private CollectionCase collectionCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(name = "activity_date", nullable = false)
    @Builder.Default
    private Instant activityDate = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_method")
    private ContactMethod contactMethod;

    @Column(name = "contact_result")
    private String contactResult;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ptp_date")
    private LocalDate ptpDate;

    @Column(name = "ptp_amount", precision = 18, scale = 2)
    private BigDecimal ptpAmount;

    @Column(name = "next_action_date")
    private LocalDate nextActionDate;

    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
