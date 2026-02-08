package com.loanplatform.borrower.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBorrowerRequest {

    @Size(max = 255, message = "Full name must be less than 255 characters")
    private String fullName;

    private LocalDate dateOfBirth;

    private String gender;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    private String alternatePhone;

    @Email(message = "Invalid email format")
    private String email;

    private String idType;

    private String idNumber;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String occupation;

    private String employerName;

    @DecimalMin(value = "0", message = "Monthly income must be positive")
    private BigDecimal monthlyIncome;

    private String notes;

    private Map<String, Object> metadata;
}
