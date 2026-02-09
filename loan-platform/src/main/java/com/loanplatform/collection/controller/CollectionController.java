package com.loanplatform.collection.controller;

import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.collection.dto.*;
import com.loanplatform.collection.service.CollectionService;
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
@RequestMapping("/v1/collections")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collections", description = "Collection case management endpoints")
@PreAuthorize("hasAnyRole('LENDER_ADMIN', 'LENDER_STAFF')")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "Get all collection cases with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<CollectionCaseResponse>>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dpdBucket) {
        log.info("Fetching collection cases - page: {}, status: {}, dpdBucket: {}", page, status, dpdBucket);
        PagedResponse<CollectionCaseResponse> cases = collectionService.getAllCases(page, size, status, dpdBucket);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection case by ID")
    public ResponseEntity<ApiResponse<CollectionCaseResponse>> getCaseById(@PathVariable UUID id) {
        log.info("Fetching collection case: {}", id);
        CollectionCaseResponse caseResponse = collectionService.getCaseById(id);
        return ResponseEntity.ok(ApiResponse.success(caseResponse));
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get collection case by loan ID")
    public ResponseEntity<ApiResponse<CollectionCaseResponse>> getCaseByLoanId(@PathVariable UUID loanId) {
        log.info("Fetching collection case for loan: {}", loanId);
        CollectionCaseResponse caseResponse = collectionService.getCaseByLoanId(loanId);
        return ResponseEntity.ok(ApiResponse.success(caseResponse));
    }

    @PostMapping("/loan/{loanId}")
    @Operation(summary = "Create collection case for a loan")
    public ResponseEntity<ApiResponse<CollectionCaseResponse>> createCase(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Creating collection case for loan: {}", loanId);
        CollectionCaseResponse caseResponse = collectionService.createCaseForLoan(loanId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(caseResponse, "Collection case created successfully"));
    }

    @PostMapping("/{id}/activities")
    @Operation(summary = "Log collection activity")
    public ResponseEntity<ApiResponse<CollectionActivityResponse>> addActivity(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCollectionActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Adding activity to case: {}", id);
        CollectionActivityResponse activity = collectionService.addActivity(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(activity, "Activity logged successfully"));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign collection case to an agent")
    public ResponseEntity<ApiResponse<CollectionCaseResponse>> assignCase(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Assigning case {} to user {}", id, userId);
        CollectionCaseResponse caseResponse = collectionService.assignCase(id, userId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(caseResponse, "Case assigned successfully"));
    }

    @PutMapping("/{id}/escalate")
    @Operation(summary = "Escalate collection case")
    public ResponseEntity<ApiResponse<CollectionCaseResponse>> escalateCase(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Escalating case: {}", id);
        CollectionCaseResponse caseResponse = collectionService.escalateCase(id, reason, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(caseResponse, "Case escalated successfully"));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve collection case")
    public ResponseEntity<ApiResponse<CollectionCaseResponse>> resolveCase(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveCollectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Resolving case: {}", id);
        CollectionCaseResponse caseResponse = collectionService.resolveCase(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(caseResponse, "Case resolved successfully"));
    }
}
