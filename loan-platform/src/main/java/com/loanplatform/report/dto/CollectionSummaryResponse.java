package com.loanplatform.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CollectionSummaryResponse {
    private BigDecimal expectedCollection;
    private BigDecimal actualCollection;
    private double collectionEfficiency;
    private BigDecimal overdueAmount;
    private long paymentsCount;
}
