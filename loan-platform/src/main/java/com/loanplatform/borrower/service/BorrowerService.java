package com.loanplatform.borrower.service;

import com.loanplatform.borrower.dto.BorrowerResponse;
import com.loanplatform.borrower.dto.CreateBorrowerRequest;
import com.loanplatform.borrower.dto.UpdateBorrowerRequest;
import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.entity.RiskBand;
import com.loanplatform.borrower.mapper.BorrowerMapper;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerMapper borrowerMapper;
    private final TenantRepository tenantRepository;

    private final AtomicLong borrowerCodeSequence = new AtomicLong(System.currentTimeMillis() % 100000);

    @Transactional
    public BorrowerResponse createBorrower(CreateBorrowerRequest request, UUID createdByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Creating borrower for tenant: {}", tenantId);

        validateTenantLimits(tenantId);

        if (borrowerRepository.existsByPhoneAndTenantIdAndDeletedFalse(request.getPhone(), tenantId)) {
            throw new BusinessException("Borrower with this phone already exists", "PHONE_EXISTS");
        }

        if (request.getIdNumber() != null &&
            borrowerRepository.existsByIdNumberAndTenantIdAndDeletedFalse(request.getIdNumber(), tenantId)) {
            throw new BusinessException("Borrower with this ID number already exists", "ID_NUMBER_EXISTS");
        }

        Borrower borrower = borrowerMapper.toEntity(request);
        borrower.setTenantId(tenantId);
        borrower.setBorrowerCode(generateBorrowerCode(tenantId));
        borrower.setCreatedBy(createdByUserId);

        calculateRiskScore(borrower);

        borrower = borrowerRepository.save(borrower);
        log.info("Borrower created: {} for tenant: {}", borrower.getId(), tenantId);

        return borrowerMapper.toResponse(borrower);
    }

    @Transactional(readOnly = true)
    public BorrowerResponse getBorrowerById(UUID borrowerId) {
        UUID tenantId = TenantContext.requireTenant();
        Borrower borrower = borrowerRepository.findByIdAndTenantId(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId));
        return borrowerMapper.toResponse(borrower);
    }

    @Transactional(readOnly = true)
    public BorrowerResponse getBorrowerByCode(String borrowerCode) {
        UUID tenantId = TenantContext.requireTenant();
        Borrower borrower = borrowerRepository.findByBorrowerCodeAndTenantId(borrowerCode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerCode));
        return borrowerMapper.toResponse(borrower);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BorrowerResponse> getAllBorrowers(int page, int size, String sortBy, String sortDir) {
        UUID tenantId = TenantContext.requireTenant();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Borrower> borrowers = borrowerRepository.findAllByTenantId(tenantId, pageable);

        return PagedResponse.of(
                borrowers.getContent().stream()
                        .map(borrowerMapper::toResponse)
                        .toList(),
                page,
                size,
                borrowers.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<BorrowerResponse> searchBorrowers(String search, int page, int size) {
        UUID tenantId = TenantContext.requireTenant();
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<Borrower> borrowers = borrowerRepository.searchBorrowers(tenantId, search, pageable);

        return PagedResponse.of(
                borrowers.getContent().stream()
                        .map(borrowerMapper::toResponse)
                        .toList(),
                page,
                size,
                borrowers.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<BorrowerResponse> getBorrowersByStatus(BorrowerStatus status, int page, int size) {
        UUID tenantId = TenantContext.requireTenant();
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<Borrower> borrowers = borrowerRepository.findByTenantIdAndStatus(tenantId, status, pageable);

        return PagedResponse.of(
                borrowers.getContent().stream()
                        .map(borrowerMapper::toResponse)
                        .toList(),
                page,
                size,
                borrowers.getTotalElements()
        );
    }

    @Transactional
    public BorrowerResponse updateBorrower(UUID borrowerId, UpdateBorrowerRequest request, UUID updatedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        Borrower borrower = borrowerRepository.findByIdAndTenantId(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId));

        if (request.getPhone() != null && !request.getPhone().equals(borrower.getPhone())) {
            if (borrowerRepository.existsByPhoneAndTenantIdAndDeletedFalse(request.getPhone(), tenantId)) {
                throw new BusinessException("Borrower with this phone already exists", "PHONE_EXISTS");
            }
        }

        borrowerMapper.updateEntity(borrower, request);
        borrower.setUpdatedBy(updatedByUserId);

        calculateRiskScore(borrower);

        borrower = borrowerRepository.save(borrower);
        log.info("Borrower updated: {}", borrowerId);

        return borrowerMapper.toResponse(borrower);
    }

    @Transactional
    public void blockBorrower(UUID borrowerId, UUID blockedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        Borrower borrower = borrowerRepository.findByIdAndTenantId(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId));

        borrower.block();
        borrower.setUpdatedBy(blockedByUserId);
        borrowerRepository.save(borrower);
        log.info("Borrower blocked: {}", borrowerId);
    }

    @Transactional
    public void activateBorrower(UUID borrowerId, UUID activatedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        Borrower borrower = borrowerRepository.findByIdAndTenantId(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId));

        borrower.activate();
        borrower.setUpdatedBy(activatedByUserId);
        borrowerRepository.save(borrower);
        log.info("Borrower activated: {}", borrowerId);
    }

    @Transactional
    public void deleteBorrower(UUID borrowerId, UUID deletedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        Borrower borrower = borrowerRepository.findByIdAndTenantId(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId));

        borrower.softDelete(deletedByUserId);
        borrowerRepository.save(borrower);
        log.info("Borrower soft-deleted: {}", borrowerId);
    }

    public long countBorrowersByTenant(UUID tenantId) {
        return borrowerRepository.countByTenantId(tenantId);
    }

    private String generateBorrowerCode(UUID tenantId) {
        String prefix = tenantId.toString().substring(0, 4).toUpperCase();
        long seq = borrowerCodeSequence.incrementAndGet();
        return String.format("BRW-%s-%06d", prefix, seq % 1000000);
    }

    private void validateTenantLimits(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        long currentCount = borrowerRepository.countByTenantId(tenantId);
        if (currentCount >= tenant.getMaxBorrowers()) {
            throw new BusinessException("Maximum borrower limit reached for this tenant", "LIMIT_EXCEEDED");
        }
    }

    private void calculateRiskScore(Borrower borrower) {
        int score = 50;

        if (borrower.getMonthlyIncome() != null) {
            if (borrower.getMonthlyIncome().doubleValue() > 50000) {
                score += 20;
            } else if (borrower.getMonthlyIncome().doubleValue() > 25000) {
                score += 10;
            }
        }

        if (borrower.getIdNumber() != null && !borrower.getIdNumber().isBlank()) {
            score += 10;
        }

        if (borrower.getEmail() != null && !borrower.getEmail().isBlank()) {
            score += 5;
        }

        if (borrower.getEmployerName() != null && !borrower.getEmployerName().isBlank()) {
            score += 5;
        }

        borrower.setRiskScore(score);

        if (score >= 80) {
            borrower.setRiskBand(RiskBand.LOW);
        } else if (score >= 60) {
            borrower.setRiskBand(RiskBand.MEDIUM);
        } else if (score >= 40) {
            borrower.setRiskBand(RiskBand.HIGH);
        } else {
            borrower.setRiskBand(RiskBand.VERY_HIGH);
        }
    }
}
