package com.loanplatform.loan.controller;

import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.loan.dto.*;
import com.loanplatform.loan.entity.LoanStatus;
import com.loanplatform.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/loans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loans", description = "Loan management endpoints")
@PreAuthorize("hasAnyRole('LENDER_ADMIN', 'LENDER_STAFF')")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Create a new loan")
    public ResponseEntity<ApiResponse<LoanResponse>> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Creating loan for borrower: {}", request.getBorrowerId());
        LoanResponse response = loanService.createLoan(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Loan created successfully"));
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Disburse a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> disburseLoan(
            @PathVariable UUID loanId,
            @Valid @RequestBody DisburseLoanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Disbursing loan: {}", loanId);
        LoanResponse response = loanService.disburseLoan(loanId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Loan disbursed successfully"));
    }

    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Approve a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> approveLoan(
            @PathVariable UUID loanId,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanResponse response = loanService.approveLoan(loanId, notes, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Loan approved"));
    }

    @PostMapping("/{loanId}/close")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Close a fully paid loan")
    public ResponseEntity<ApiResponse<Void>> closeLoan(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loanService.closeLoan(loanId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Loan closed"));
    }

    @GetMapping
    @Operation(summary = "Get all loans with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponse>>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<LoanResponse> response = loanService.getAllLoans(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable UUID loanId) {
        LoanResponse response = loanService.getLoanById(loanId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{loanNumber}")
    @Operation(summary = "Get loan by loan number")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanByNumber(@PathVariable String loanNumber) {
        LoanResponse response = loanService.getLoanByNumber(loanNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/borrower/{borrowerId}")
    @Operation(summary = "Get loans for a borrower")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponse>>> getLoansByBorrower(
            @PathVariable UUID borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<LoanResponse> response = loanService.getLoansByBorrower(borrowerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get loans by status")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponse>>> getLoansByStatus(
            @PathVariable LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<LoanResponse> response = loanService.getLoansByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{loanId}/schedule")
    @Operation(summary = "Get repayment schedule for a loan")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getLoanSchedule(@PathVariable UUID loanId) {
        List<ScheduleResponse> schedules = loanService.getLoanSchedule(loanId);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }
}
