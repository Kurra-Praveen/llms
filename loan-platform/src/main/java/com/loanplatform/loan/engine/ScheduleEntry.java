package com.loanplatform.loan.engine;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ScheduleEntry {
    private int installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal installmentAmount;
    private BigDecimal outstandingAfter;
}
