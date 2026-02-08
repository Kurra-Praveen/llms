package com.loanplatform.loan.engine;

import com.loanplatform.loan.entity.InterestType;
import com.loanplatform.loan.entity.RepaymentFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ScheduleGenerationRequest {
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private BigDecimal dailyFixedAmount;  // For DAILY_FIXED interest type
    private InterestType interestType;
    private int tenureMonths;
    private RepaymentFrequency frequency;
    private LocalDate startDate;
    private int gracePeriodDays;
}

