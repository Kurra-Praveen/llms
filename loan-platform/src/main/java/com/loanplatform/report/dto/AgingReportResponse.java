package com.loanplatform.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AgingReportResponse {
    private List<AgingBucketResponse> buckets;
    private long totalLoans;
    private BigDecimal totalOutstanding;
    private String generatedAt;

    @Data
    @Builder
    public static class AgingBucketResponse {
        private String bucket;
        private String label;
        private Integer minDpd;
        private Integer maxDpd;
        private long loansCount;
        private BigDecimal outstandingPrincipal;
        private BigDecimal outstandingInterest;
        private BigDecimal outstandingPenalty;
        private BigDecimal totalOutstanding;
        private double percentageOfPortfolio;
    }
}
