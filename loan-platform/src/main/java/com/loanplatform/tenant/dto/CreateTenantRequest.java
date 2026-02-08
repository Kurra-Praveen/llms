package com.loanplatform.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class CreateTenantRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 255, message = "Business name must be less than 255 characters")
    private String businessName;

    @NotBlank(message = "Business code is required")
    @Size(min = 3, max = 50, message = "Business code must be between 3 and 50 characters")
    private String businessCode;

    @NotBlank(message = "Contact email is required")
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

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email format")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, message = "Admin password must be at least 8 characters")
    private String adminPassword;

    @NotBlank(message = "Admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    private String adminLastName;

    private String adminPhone;
}
