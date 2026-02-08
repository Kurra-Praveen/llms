package com.loanplatform.borrower.controller;

import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.borrower.dto.BorrowerResponse;
import com.loanplatform.borrower.dto.CreateBorrowerRequest;
import com.loanplatform.borrower.dto.UpdateBorrowerRequest;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.service.BorrowerService;
import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.common.dto.PagedResponse;
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
@RequestMapping("/v1/borrowers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Borrowers", description = "Borrower management endpoints")
@PreAuthorize("hasAnyRole('LENDER_ADMIN', 'LENDER_STAFF')")
public class BorrowerController {

    private final BorrowerService borrowerService;

    @PostMapping
    @Operation(summary = "Create a new borrower")
    public ResponseEntity<ApiResponse<BorrowerResponse>> createBorrower(
            @Valid @RequestBody CreateBorrowerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Creating borrower: {}", request.getFullName());
        BorrowerResponse response = borrowerService.createBorrower(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Borrower created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all borrowers with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<BorrowerResponse>>> getAllBorrowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<BorrowerResponse> response = borrowerService.getAllBorrowers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search borrowers by name, phone, code, or email")
    public ResponseEntity<ApiResponse<PagedResponse<BorrowerResponse>>> searchBorrowers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BorrowerResponse> response = borrowerService.searchBorrowers(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get borrowers by status")
    public ResponseEntity<ApiResponse<PagedResponse<BorrowerResponse>>> getBorrowersByStatus(
            @PathVariable BorrowerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BorrowerResponse> response = borrowerService.getBorrowersByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{borrowerId}")
    @Operation(summary = "Get borrower by ID")
    public ResponseEntity<ApiResponse<BorrowerResponse>> getBorrowerById(@PathVariable UUID borrowerId) {
        BorrowerResponse response = borrowerService.getBorrowerById(borrowerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{borrowerCode}")
    @Operation(summary = "Get borrower by code")
    public ResponseEntity<ApiResponse<BorrowerResponse>> getBorrowerByCode(@PathVariable String borrowerCode) {
        BorrowerResponse response = borrowerService.getBorrowerByCode(borrowerCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{borrowerId}")
    @Operation(summary = "Update borrower details")
    public ResponseEntity<ApiResponse<BorrowerResponse>> updateBorrower(
            @PathVariable UUID borrowerId,
            @Valid @RequestBody UpdateBorrowerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BorrowerResponse response = borrowerService.updateBorrower(borrowerId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Borrower updated successfully"));
    }

    @PostMapping("/{borrowerId}/block")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Block a borrower")
    public ResponseEntity<ApiResponse<Void>> blockBorrower(
            @PathVariable UUID borrowerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        borrowerService.blockBorrower(borrowerId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Borrower blocked"));
    }

    @PostMapping("/{borrowerId}/activate")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Activate a borrower")
    public ResponseEntity<ApiResponse<Void>> activateBorrower(
            @PathVariable UUID borrowerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        borrowerService.activateBorrower(borrowerId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Borrower activated"));
    }

    @DeleteMapping("/{borrowerId}")
    @PreAuthorize("hasRole('LENDER_ADMIN')")
    @Operation(summary = "Soft-delete a borrower")
    public ResponseEntity<ApiResponse<Void>> deleteBorrower(
            @PathVariable UUID borrowerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        borrowerService.deleteBorrower(borrowerId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Borrower deleted"));
    }
}
