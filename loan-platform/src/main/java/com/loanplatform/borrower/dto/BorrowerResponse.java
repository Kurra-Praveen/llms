package com.loanplatform.borrower.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerResponse {

    private UUID id;
    private UUID tenantId;
    private String borrowerCode;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String alternatePhone;
    private String email;
    private String idType;
    private String idNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String fullAddress;
    private String occupation;
    private String employerName;
    private BigDecimal monthlyIncome;
    private String creditRating;
    private Integer riskScore;
    private String riskBand;
    private String status;
    private String notes;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    private int activeLoansCount;
    private BigDecimal totalOutstanding;
}
