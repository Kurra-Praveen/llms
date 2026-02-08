package com.loanplatform.tenant.service;

import com.loanplatform.auth.dto.CreateUserRequest;
import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.auth.service.AuthService;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.tenant.dto.CreateTenantRequest;
import com.loanplatform.tenant.dto.TenantResponse;
import com.loanplatform.tenant.dto.UpdateTenantRequest;
import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.entity.TenantStatus;
import com.loanplatform.tenant.mapper.TenantMapper;
import com.loanplatform.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request, UUID createdByUserId) {
        log.info("Creating tenant: {}", request.getBusinessCode());

        if (tenantRepository.existsByBusinessCode(request.getBusinessCode())) {
            throw new BusinessException("Business code already exists", "BUSINESS_CODE_EXISTS");
        }

        Tenant tenant = tenantMapper.toEntity(request);
        if (tenant.getSubscriptionStartDate() == null) {
            tenant.setSubscriptionStartDate(LocalDate.now());
        }
        if (tenant.getMaxBorrowers() == null) {
            tenant.setMaxBorrowers(100);
        }
        if (tenant.getMaxLoans() == null) {
            tenant.setMaxLoans(500);
        }

        tenant = tenantRepository.save(tenant);
        log.info("Tenant created with ID: {}", tenant.getId());

        CreateUserRequest adminRequest = CreateUserRequest.builder()
                .tenantId(tenant.getId())
                .email(request.getAdminEmail())
                .password(request.getAdminPassword())
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .phone(request.getAdminPhone())
                .role(Role.LENDER_ADMIN)
                .build();

        authService.createUser(adminRequest, createdByUserId);
        log.info("Admin user created for tenant: {}", tenant.getId());

        return enrichTenantResponse(tenantMapper.toResponse(tenant), tenant.getId());
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        return enrichTenantResponse(tenantMapper.toResponse(tenant), tenantId);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantByCode(String businessCode) {
        Tenant tenant = tenantRepository.findByBusinessCode(businessCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", businessCode));
        return enrichTenantResponse(tenantMapper.toResponse(tenant), tenant.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantResponse> getAllTenants(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Tenant> tenants = tenantRepository.findAll(pageable);

        return PagedResponse.of(
                tenants.getContent().stream()
                        .map(t -> enrichTenantResponse(tenantMapper.toResponse(t), t.getId()))
                        .toList(),
                page,
                size,
                tenants.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<TenantResponse> searchTenants(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("businessName").ascending());
        Page<Tenant> tenants = tenantRepository.searchTenants(search, pageable);

        return PagedResponse.of(
                tenants.getContent().stream()
                        .map(t -> enrichTenantResponse(tenantMapper.toResponse(t), t.getId()))
                        .toList(),
                page,
                size,
                tenants.getTotalElements()
        );
    }

    @Transactional
    public TenantResponse updateTenant(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        tenantMapper.updateEntity(tenant, request);
        tenant = tenantRepository.save(tenant);

        log.info("Tenant updated: {}", tenantId);
        return enrichTenantResponse(tenantMapper.toResponse(tenant), tenantId);
    }

    @Transactional
    public void activateTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        tenant.activate();
        tenantRepository.save(tenant);
        log.info("Tenant activated: {}", tenantId);
    }

    @Transactional
    public void suspendTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        tenant.suspend();
        tenantRepository.save(tenant);
        log.info("Tenant suspended: {}", tenantId);
    }

    @Transactional
    public void deactivateTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        tenant.deactivate();
        tenantRepository.save(tenant);
        log.info("Tenant deactivated: {}", tenantId);
    }

    public long countActiveTenants() {
        return tenantRepository.countByStatus(TenantStatus.ACTIVE);
    }

    private TenantResponse enrichTenantResponse(TenantResponse response, UUID tenantId) {
        response.setUserCount(userRepository.countByTenantId(tenantId));
        return response;
    }
}
