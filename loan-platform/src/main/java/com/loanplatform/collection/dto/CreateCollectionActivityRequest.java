package com.loanplatform.collection.dto;

import com.loanplatform.collection.entity.ActivityType;
import com.loanplatform.collection.entity.ContactMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCollectionActivityRequest {
    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    private ContactMethod contactMethod;

    private String contactResult;

    private String notes;

    private LocalDate ptpDate;

    private BigDecimal ptpAmount;

    private LocalDate nextActionDate;

    private String nextAction;
}
