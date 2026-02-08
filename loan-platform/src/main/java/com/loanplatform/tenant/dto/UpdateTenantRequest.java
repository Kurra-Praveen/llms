package com.loanplatform.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {

    @Size(max = 255, message = "Business name must be less than 255 characters")
    private String businessName;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String contactPhone;

    private String address;

    private String subscriptionPlan;

    private LocalDate subscriptionStartDate;

    private LocalDate subscriptionEndDate;

    private Integer maxBorrowers;

    private Integer maxLoans;

    private Map<String, Object> settings;
}
