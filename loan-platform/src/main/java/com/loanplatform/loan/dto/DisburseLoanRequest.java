package com.loanplatform.loan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisburseLoanRequest {

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;

    private LocalDate firstPaymentDate;

    private String notes;
}
