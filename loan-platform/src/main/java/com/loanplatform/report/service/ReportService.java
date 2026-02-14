package com.loanplatform.report.service;

import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.LoanStatus;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.payment.repository.PaymentRepository;
import com.loanplatform.report.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public PortfolioSummaryResponse getPortfolioSummary(UUID tenantId) {
        log.info("Generating portfolio summary for tenant: {}", tenantId);

        // Fetch totals
        BigDecimal totalDisbursed = loanRepository.sumPrincipalByStatusIn(
                tenantId,
                List.of(LoanStatus.ACTIVE, LoanStatus.CLOSED, LoanStatus.WRITTEN_OFF)
        );
        if (totalDisbursed == null) totalDisbursed = BigDecimal.ZERO;

        BigDecimal totalOutstanding = loanRepository.sumOutstandingPrincipalActive(tenantId);
        if (totalOutstanding == null) totalOutstanding = BigDecimal.ZERO;

        BigDecimal totalRepaid = loanRepository.sumTotalPaid(tenantId);
        if (totalRepaid == null) totalRepaid = BigDecimal.ZERO;

        long activeLoansCount = loanRepository.countByTenantIdAndStatus(tenantId, LoanStatus.ACTIVE);
        long closedLoansCount = loanRepository.countByTenantIdAndStatus(tenantId, LoanStatus.CLOSED);
        long totalLoansCount = loanRepository.countByTenantId(tenantId);

        BigDecimal averageLoanSize = BigDecimal.ZERO;
        long denom = activeLoansCount + closedLoansCount;
        if (denom > 0) {
            averageLoanSize = totalDisbursed.divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP);
        }

        // Calculate PAR (Portfolio At Risk)
        // PAR = (Outstanding Balance of Loans with DPD > X) / Total Outstanding Portfolio
        // Note: The frontend calculation was count-based percentages.
        // Standard financial PAR is usually volume-based (Amount).
        // However, to match frontend exactly, let's look at the frontend code:
        // "const par30 = activeLoansCount > 0 ? Math.round((par30Count / activeLoansCount) * 100 * 10) / 10 : 0;"
        // It uses COUNTS. I will stick to that to maintain consistency, but volume-based is better.

        long par30Count = loanRepository.countActiveLoansWithDpdAtLeast(tenantId, 30);
        long par60Count = loanRepository.countActiveLoansWithDpdAtLeast(tenantId, 60);
        long par90Count = loanRepository.countActiveLoansWithDpdAtLeast(tenantId, 90);

        double par30 = activeLoansCount > 0 ? (double) par30Count / activeLoansCount * 100 : 0;
        double par60 = activeLoansCount > 0 ? (double) par60Count / activeLoansCount * 100 : 0;
        double par90 = activeLoansCount > 0 ? (double) par90Count / activeLoansCount * 100 : 0;

        return PortfolioSummaryResponse.builder()
                .totalDisbursed(totalDisbursed)
                .totalOutstanding(totalOutstanding)
                .totalRepaid(totalRepaid)
                .activeLoansCount(activeLoansCount)
                .averageLoanSize(averageLoanSize)
                .par30(Math.round(par30 * 10.0) / 10.0)
                .par60(Math.round(par60 * 10.0) / 10.0)
                .par90(Math.round(par90 * 10.0) / 10.0)
                .totalLoansCount(totalLoansCount)
                .closedLoansCount(closedLoansCount)
                .build();
    }

    public CollectionSummaryResponse getCollectionSummary(UUID tenantId, String period) {
        log.info("Generating collection summary for tenant: {} period: {}", tenantId, period);

        DateRange range = getDateRange(period);

        // Expected Collection: Sum of schedules due in this period
        BigDecimal expectedCollection = repaymentScheduleRepository.sumTotalDueBetween(tenantId, range.startDate, range.endDate);
        if (expectedCollection == null) expectedCollection = BigDecimal.ZERO;

        // Actual Collection: Sum of payments received in this period
        BigDecimal actualCollection = paymentRepository.sumAmountPaidInDateRange(tenantId, range.startDate, range.endDate);
        if (actualCollection == null) actualCollection = BigDecimal.ZERO;

        long paymentsCount = paymentRepository.countPaymentsInDateRange(tenantId, range.startDate, range.endDate);

        // Overdue Amount: Sum of outstanding interest + penalty on active loans
        // Note: This matches frontend logic "overdueAmount += (loan.outstandingInterest + loan.outstandingPenalty)"
        BigDecimal overdueAmount = loanRepository.sumOverdueAmount(tenantId);
        if (overdueAmount == null) overdueAmount = BigDecimal.ZERO;

        double collectionEfficiency = 100.0;
        if (expectedCollection.compareTo(BigDecimal.ZERO) > 0) {
            collectionEfficiency = actualCollection.divide(expectedCollection, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        // Cap at 100% per frontend logic (though real efficiency can be > 100% due to arrears collection)
        collectionEfficiency = Math.min(collectionEfficiency, 100.0);

        return CollectionSummaryResponse.builder()
                .expectedCollection(expectedCollection)
                .actualCollection(actualCollection)
                .collectionEfficiency(Math.round(collectionEfficiency * 10.0) / 10.0)
                .overdueAmount(overdueAmount)
                .paymentsCount(paymentsCount)
                .build();
    }

    public List<DailyCollectionResponse> getDailyCollections(UUID tenantId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<DailyCollectionResponse> data = paymentRepository.getDailyCollections(tenantId, startDate, endDate);

        // Fill missing dates with zero
        Map<LocalDate, DailyCollectionResponse> map = data.stream()
                .collect(Collectors.toMap(DailyCollectionResponse::getDate, d -> d));

        List<DailyCollectionResponse> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            if (map.containsKey(date)) {
                result.add(map.get(date));
            } else {
                result.add(new DailyCollectionResponse(date, BigDecimal.ZERO, 0L));
            }
        }

        return result;
    }

    public List<LoansByStatusResponse> getLoansByStatus(UUID tenantId) {
        return loanRepository.countLoansByStatus(tenantId);
    }

    public AgingReportResponse getAgingReport(UUID tenantId) {
        // Fetch all active loans to bucket them
        // TODO: For very large datasets, move this bucketing to SQL
        List<Loan> loans = loanRepository.findAllActiveLoans();

        // Initialize buckets
        Map<String, AgingReportResponse.AgingBucketResponse> buckets = new LinkedHashMap<>();
        createBucket(buckets, "CURRENT", "Current (0 DPD)", 0, 0);
        createBucket(buckets, "1-30", "1-30 Days", 1, 30);
        createBucket(buckets, "31-60", "31-60 Days", 31, 60);
        createBucket(buckets, "61-90", "61-90 Days", 61, 90);
        createBucket(buckets, "90+", "90+ Days (NPA)", 91, null);

        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Loan loan : loans) {
            if (!loan.getTenantId().equals(tenantId)) continue; // Should be filtered by repository already

            int dpd = loan.getDpd() != null ? loan.getDpd() : 0;
            String bucketKey = getBucketKey(dpd);

            AgingReportResponse.AgingBucketResponse bucket = buckets.get(bucketKey);
            if (bucket != null) {
                bucket.setLoansCount(bucket.getLoansCount() + 1);

                BigDecimal principal = loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : BigDecimal.ZERO;
                BigDecimal interest = loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO;
                BigDecimal penalty = loan.getOutstandingPenalty() != null ? loan.getOutstandingPenalty() : BigDecimal.ZERO;
                BigDecimal total = principal.add(interest).add(penalty);

                bucket.setOutstandingPrincipal(bucket.getOutstandingPrincipal().add(principal));
                bucket.setOutstandingInterest(bucket.getOutstandingInterest().add(interest));
                bucket.setOutstandingPenalty(bucket.getOutstandingPenalty().add(penalty));
                bucket.setTotalOutstanding(bucket.getTotalOutstanding().add(total));

                totalOutstanding = totalOutstanding.add(total);
            }
        }

        // Calculate percentages
        BigDecimal finalTotalOutstanding = totalOutstanding;
        buckets.values().forEach(b -> {
            if (finalTotalOutstanding.compareTo(BigDecimal.ZERO) > 0) {
                double pct = b.getTotalOutstanding()
                        .divide(finalTotalOutstanding, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                b.setPercentageOfPortfolio(Math.round(pct * 10.0) / 10.0);
            }
        });

        return AgingReportResponse.builder()
                .buckets(new ArrayList<>(buckets.values()))
                .totalLoans(loans.size())
                .totalOutstanding(totalOutstanding)
                .generatedAt(java.time.LocalDateTime.now().toString())
                .build();
    }

    private void createBucket(Map<String, AgingReportResponse.AgingBucketResponse> map,
                            String key, String label, Integer min, Integer max) {
        map.put(key, AgingReportResponse.AgingBucketResponse.builder()
                .bucket(key)
                .label(label)
                .minDpd(min)
                .maxDpd(max)
                .loansCount(0)
                .outstandingPrincipal(BigDecimal.ZERO)
                .outstandingInterest(BigDecimal.ZERO)
                .outstandingPenalty(BigDecimal.ZERO)
                .totalOutstanding(BigDecimal.ZERO)
                .percentageOfPortfolio(0.0)
                .build());
    }

    private String getBucketKey(int dpd) {
        if (dpd >= 91) return "90+";
        if (dpd >= 61) return "61-90";
        if (dpd >= 31) return "31-60";
        if (dpd >= 1) return "1-30";
        return "CURRENT";
    }

    private DateRange getDateRange(String period) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now;
        LocalDate startDate;

        switch (period) {
            case "this_week":
                // Start of week (assuming Monday start, but frontend used now.getDay() offset)
                // JS getDay(): 0=Sun, 1=Mon.
                // If today is Wed(3), start is today-3 = Sun.
                startDate = now.minusDays(now.getDayOfWeek().getValue() % 7);
                break;
            case "last_month":
                startDate = now.minusMonths(1).withDayOfMonth(1);
                endDate = now.withDayOfMonth(1).minusDays(1);
                break;
            case "this_quarter":
                int currentQuarterMonth = (now.getMonthValue() - 1) / 3 * 3 + 1;
                startDate = LocalDate.of(now.getYear(), currentQuarterMonth, 1);
                break;
            case "this_month":
            default:
                startDate = now.withDayOfMonth(1);
                break;
        }
        return new DateRange(startDate, endDate);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {}
}
