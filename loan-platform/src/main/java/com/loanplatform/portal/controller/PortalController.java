package com.loanplatform.portal.controller;

import com.loanplatform.borrower.dto.BorrowerResponse;
import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.loan.dto.LoanResponse;
import com.loanplatform.loan.dto.ScheduleResponse;
import com.loanplatform.payment.dto.PaymentResponse;
import com.loanplatform.portal.dto.PortalSummaryResponse;
import com.loanplatform.portal.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/portal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Borrower Portal", description = "Borrower self-service endpoints (mobile app)")
@PreAuthorize("hasRole('BORROWER')")
public class PortalController {

    private final PortalService portalService;

    @GetMapping("/me")
    @Operation(summary = "Get my borrower profile")
    public ResponseEntity<ApiResponse<BorrowerResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMyProfile()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get my dashboard summary (active loans, outstanding, next EMI)")
    public ResponseEntity<ApiResponse<PortalSummaryResponse>> getMySummary() {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMySummary()));
    }

    @GetMapping("/loans")
    @Operation(summary = "Get my loans with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponse>>> getMyLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success(
                portalService.getMyLoans(page, size, sortBy, sortDir)));
    }

    @GetMapping("/loans/{loanId}")
    @Operation(summary = "Get one of my loans by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> getMyLoan(@PathVariable UUID loanId) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMyLoan(loanId)));
    }

    @GetMapping("/loans/{loanId}/schedule")
    @Operation(summary = "Get the repayment schedule for one of my loans")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getMyLoanSchedule(@PathVariable UUID loanId) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMyLoanSchedule(loanId)));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get my payment history with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMyPayments(page, size)));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get one of my payments by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getMyPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMyPayment(paymentId)));
    }
}
