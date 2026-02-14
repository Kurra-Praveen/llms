package com.loanplatform.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortfolioSummaryResponse {
    private BigDecimal totalDisbursed;
    private BigDecimal totalOutstanding;
    private BigDecimal totalRepaid;
    private long activeLoansCount;
    private BigDecimal averageLoanSize;
    private double par30;
    private double par60;
    private double par90;
    private long totalLoansCount;
    private long closedLoansCount;
}
