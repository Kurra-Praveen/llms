package com.loanplatform.collection.dto;

import com.loanplatform.collection.entity.ActivityType;
import com.loanplatform.collection.entity.ContactMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionActivityResponse {
    private UUID id;
    private UUID collectionCaseId;
    private ActivityType activityType;
    private Instant activityDate;
    private ContactMethod contactMethod;
    private String contactResult;
    private String notes;
    private LocalDate ptpDate;
    private BigDecimal ptpAmount;
    private LocalDate nextActionDate;
    private String nextAction;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;
}
