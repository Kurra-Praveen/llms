package com.loanplatform.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private UUID id;
    private String businessName;
    private String businessCode;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String status;
    private String subscriptionPlan;
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private Integer maxBorrowers;
    private Integer maxLoans;
    private Map<String, Object> settings;
    private Instant createdAt;
    private Instant updatedAt;

    private long borrowerCount;
    private long loanCount;
    private long userCount;
}
