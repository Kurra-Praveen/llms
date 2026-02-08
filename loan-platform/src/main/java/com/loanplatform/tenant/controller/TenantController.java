package com.loanplatform.tenant.controller;

import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.tenant.dto.CreateTenantRequest;
import com.loanplatform.tenant.dto.TenantResponse;
import com.loanplatform.tenant.dto.UpdateTenantRequest;
import com.loanplatform.tenant.service.TenantService;
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
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenants", description = "Tenant management endpoints (Super Admin only)")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new tenant with admin user")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Creating tenant: {}", request.getBusinessCode());
        TenantResponse response = tenantService.createTenant(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all tenants with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<TenantResponse>>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<TenantResponse> response = tenantService.getAllTenants(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search tenants by name, code, or email")
    public ResponseEntity<ApiResponse<PagedResponse<TenantResponse>>> searchTenants(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TenantResponse> response = tenantService.searchTenants(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('LENDER_ADMIN') and #tenantId == principal.tenantId)")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(@PathVariable UUID tenantId) {
        TenantResponse response = tenantService.getTenantById(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{businessCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get tenant by business code")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantByCode(@PathVariable String businessCode) {
        TenantResponse response = tenantService.getTenantByCode(businessCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update tenant details")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse response = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant updated successfully"));
    }

    @PostMapping("/{tenantId}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate a tenant")
    public ResponseEntity<ApiResponse<Void>> activateTenant(@PathVariable UUID tenantId) {
        tenantService.activateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant activated"));
    }

    @PostMapping("/{tenantId}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Suspend a tenant")
    public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable UUID tenantId) {
        tenantService.suspendTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant suspended"));
    }

    @PostMapping("/{tenantId}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate a tenant")
    public ResponseEntity<ApiResponse<Void>> deactivateTenant(@PathVariable UUID tenantId) {
        tenantService.deactivateTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant deactivated"));
    }
}
