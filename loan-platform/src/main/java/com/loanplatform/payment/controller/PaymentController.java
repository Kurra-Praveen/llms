package com.loanplatform.payment.controller;

import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.payment.dto.PaymentResponse;
import com.loanplatform.payment.dto.RecordPaymentRequest;
import com.loanplatform.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing endpoints")
@PreAuthorize("hasAnyRole('LENDER_ADMIN', 'LENDER_STAFF')")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Record a new payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Recording payment for loan: {}", request.getLoanId());
        PaymentResponse response = paymentService.recordPayment(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Payment recorded successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all payments with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<PaymentResponse> response = paymentService.getAllPayments(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get payments for a loan")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getPaymentsForLoan(
            @PathVariable UUID loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PaymentResponse> response = paymentService.getPaymentsForLoan(loanId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{paymentId}/reverse")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Reverse a payment (requires approval)")
    public ResponseEntity<ApiResponse<PaymentResponse>> reversePayment(
            @PathVariable UUID paymentId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Reversing payment: {}", paymentId);
        PaymentResponse response = paymentService.reversePayment(paymentId, reason, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Payment reversed successfully"));
    }
}
