package com.loanplatform.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalSummaryResponse {

    private long activeLoansCount;
    private BigDecimal totalOutstanding;
    private NextEmi nextEmi;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextEmi {
        private UUID loanId;
        private String loanNumber;
        private Integer installmentNumber;
        private LocalDate dueDate;
        private BigDecimal installmentAmount;
        private BigDecimal outstandingAmount;
        private String status;
        private Integer daysPastDue;
    }
}
