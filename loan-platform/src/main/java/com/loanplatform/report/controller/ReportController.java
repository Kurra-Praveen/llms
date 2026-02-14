package com.loanplatform.report.controller;

import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.report.dto.*;
import com.loanplatform.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Analytics and Reporting endpoints")
@PreAuthorize("hasAnyRole('LENDER_ADMIN', 'LENDER_STAFF')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/portfolio-summary")
    @Operation(summary = "Get portfolio summary statistics")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getPortfolioSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Fetching portfolio summary for tenant: {}", principal.getTenantId());
        PortfolioSummaryResponse summary = reportService.getPortfolioSummary(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/collection-summary")
    @Operation(summary = "Get collection summary for a specific period")
    public ResponseEntity<ApiResponse<CollectionSummaryResponse>> getCollectionSummary(
            @RequestParam(defaultValue = "this_month") String period,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Fetching collection summary for tenant: {}, period: {}", principal.getTenantId(), period);
        CollectionSummaryResponse summary = reportService.getCollectionSummary(principal.getTenantId(), period);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/daily-collections")
    @Operation(summary = "Get daily collection trends")
    public ResponseEntity<ApiResponse<List<DailyCollectionResponse>>> getDailyCollections(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Fetching daily collections for tenant: {}, days: {}", principal.getTenantId(), days);
        List<DailyCollectionResponse> data = reportService.getDailyCollections(principal.getTenantId(), days);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/loans-by-status")
    @Operation(summary = "Get loan counts and volumes grouped by status")
    public ResponseEntity<ApiResponse<List<LoansByStatusResponse>>> getLoansByStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Fetching loans by status for tenant: {}", principal.getTenantId());
        List<LoansByStatusResponse> data = reportService.getLoansByStatus(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/aging")
    @Operation(summary = "Get portfolio aging report (DPD buckets)")
    public ResponseEntity<ApiResponse<AgingReportResponse>> getAgingReport(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Fetching aging report for tenant: {}", principal.getTenantId());
        AgingReportResponse report = reportService.getAgingReport(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
