package com.loanplatform.collection.dto;

import com.loanplatform.collection.entity.CollectionPriority;
import com.loanplatform.collection.entity.CollectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCaseResponse {
    private UUID id;
    private UUID tenantId;
    private UUID loanId;
    private String loanNumber;
    private String borrowerName;
    private String borrowerPhone;
    private String caseNumber;

    private String dpdBucket;
    private Integer dpdDays;
    private BigDecimal overdueAmount;
    private BigDecimal overduePrincipal;
    private BigDecimal overdueInterest;
    private BigDecimal overduePenalty;

    private UUID assignedTo;
    private String assignedToName;
    private Instant assignedAt;

    private CollectionStatus status;
    private CollectionPriority priority;

    private String resolutionType;
    private Instant resolvedAt;
    private UUID resolvedBy;
    private String resolvedByName;
    private String resolutionNotes;

    private LocalDate ptpDate;
    private BigDecimal ptpAmount;
    private String ptpStatus;

    private LocalDate lastContactDate;
    private LocalDate nextActionDate;
    private String nextAction;

    private List<CollectionActivityResponse> activities;

    private Instant createdAt;
    private Instant updatedAt;
}
