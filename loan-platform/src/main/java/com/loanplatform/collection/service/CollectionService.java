package com.loanplatform.collection.service;

import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.collection.dto.*;
import com.loanplatform.collection.entity.*;
import com.loanplatform.collection.mapper.CollectionMapper;
import com.loanplatform.collection.repository.CollectionActivityRepository;
import com.loanplatform.collection.repository.CollectionCaseRepository;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CollectionCaseRepository collectionCaseRepository;
    private final CollectionActivityRepository activityRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final CollectionMapper mapper;

    private final AtomicLong caseSequence = new AtomicLong(1000);

    @Transactional(readOnly = true)
    public PagedResponse<CollectionCaseResponse> getAllCases(int page, int size, String status, String dpdBucket) {
        UUID tenantId = TenantContext.requireTenant();
        Pageable pageable = PageRequest.of(page, size);

        Page<CollectionCase> casesPage;
        if (status != null && !status.isEmpty()) {
            CollectionStatus collectionStatus = CollectionStatus.valueOf(status);
            casesPage = collectionCaseRepository.findByTenantIdAndStatus(tenantId, collectionStatus, pageable);
        } else if (dpdBucket != null && !dpdBucket.isEmpty()) {
            casesPage = collectionCaseRepository.findByTenantIdAndDpdBucket(tenantId, dpdBucket, pageable);
        } else {
            casesPage = collectionCaseRepository.findAllByTenantId(tenantId, pageable);
        }

        List<CollectionCaseResponse> responses = casesPage.getContent().stream()
                .map(this::enrichCaseResponse)
                .toList();

        return PagedResponse.of(responses, casesPage.getNumber(), casesPage.getSize(), casesPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CollectionCaseResponse getCaseById(UUID caseId) {
        UUID tenantId = TenantContext.requireTenant();
        CollectionCase collectionCase = collectionCaseRepository.findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Case", caseId));

        CollectionCaseResponse response = enrichCaseResponse(collectionCase);

        // Load activities
        List<CollectionActivity> activities = activityRepository.findByCollectionCaseId(caseId);
        response.setActivities(mapper.toActivityResponses(activities));

        return response;
    }

    @Transactional(readOnly = true)
    public CollectionCaseResponse getCaseByLoanId(UUID loanId) {
        UUID tenantId = TenantContext.requireTenant();
        CollectionCase collectionCase = collectionCaseRepository.findByLoanIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Case for Loan", loanId));

        return enrichCaseResponse(collectionCase);
    }

    @Transactional
    public CollectionCaseResponse createCaseForLoan(UUID loanId, UUID createdBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Creating collection case for loan: {}", loanId);

        // Check if active case already exists
        if (collectionCaseRepository.existsByLoanIdAndStatusNot(loanId, CollectionStatus.RESOLVED)) {
            throw new BusinessException("An active collection case already exists for this loan",
                    HttpStatus.CONFLICT, "CASE_EXISTS");
        }

        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getDpd() == null || loan.getDpd() <= 0) {
            throw new BusinessException("Cannot create collection case for non-overdue loan",
                    HttpStatus.BAD_REQUEST, "NOT_OVERDUE");
        }

        String caseNumber = generateCaseNumber(tenantId);
        int dpd = loan.getDpd();
        BigDecimal principal = loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : BigDecimal.ZERO;
        BigDecimal interest = loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO;
        BigDecimal penalty = loan.getOutstandingPenalty() != null ? loan.getOutstandingPenalty() : BigDecimal.ZERO;

        CollectionCase collectionCase = CollectionCase.builder()
                .tenantId(tenantId)
                .loanId(loanId)
                .caseNumber(caseNumber)
                .dpdDays(dpd)
                .dpdBucket(calculateDpdBucket(dpd))
                .overdueAmount(principal.add(interest).add(penalty))
                .overduePrincipal(principal)
                .overdueInterest(interest)
                .overduePenalty(penalty)
                .status(CollectionStatus.OPEN)
                .priority(calculatePriority(dpd))
                .build();

        collectionCase = collectionCaseRepository.save(collectionCase);

        // Create initial activity
        CollectionActivity activity = CollectionActivity.builder()
                .tenantId(tenantId)
                .collectionCaseId(collectionCase.getId())
                .activityType(ActivityType.NOTE_ADDED)
                .notes("Collection case created. DPD: " + dpd + " days, Outstanding: ₹" + collectionCase.getOverdueAmount())
                .createdBy(createdBy)
                .build();
        activityRepository.save(activity);

        log.info("Collection case created: {} for loan: {}", caseNumber, loanId);
        return enrichCaseResponse(collectionCase);
    }

    @Transactional
    public CollectionActivityResponse addActivity(UUID caseId, CreateCollectionActivityRequest request, UUID userId) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Adding activity to case: {}", caseId);

        CollectionCase collectionCase = collectionCaseRepository.findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Case", caseId));

        if (collectionCase.getStatus() == CollectionStatus.RESOLVED) {
            throw new BusinessException("Cannot add activity to resolved case", HttpStatus.BAD_REQUEST, "CASE_RESOLVED");
        }

        CollectionActivity activity = CollectionActivity.builder()
                .tenantId(tenantId)
                .collectionCaseId(caseId)
                .activityType(request.getActivityType())
                .contactMethod(request.getContactMethod())
                .contactResult(request.getContactResult())
                .notes(request.getNotes())
                .ptpDate(request.getPtpDate())
                .ptpAmount(request.getPtpAmount())
                .nextActionDate(request.getNextActionDate())
                .nextAction(request.getNextAction())
                .createdBy(userId)
                .build();

        activity = activityRepository.save(activity);

        // Update case based on activity
        collectionCase.setLastContactDate(java.time.LocalDate.now());
        if (request.getNextActionDate() != null) {
            collectionCase.setNextActionDate(request.getNextActionDate());
            collectionCase.setNextAction(request.getNextAction());
        }
        if (request.getPtpDate() != null && request.getPtpAmount() != null) {
            collectionCase.recordPtp(request.getPtpDate(), request.getPtpAmount());
        }
        collectionCaseRepository.save(collectionCase);

        log.info("Activity added to case: {}", caseId);
        return mapper.toActivityResponse(activity);
    }

    @Transactional
    public CollectionCaseResponse assignCase(UUID caseId, UUID assignToUserId, UUID assignedBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Assigning case {} to user {}", caseId, assignToUserId);

        CollectionCase collectionCase = collectionCaseRepository.findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Case", caseId));

        // Verify user exists
        if (!userRepository.existsById(assignToUserId)) {
            throw new ResourceNotFoundException("User", assignToUserId);
        }

        collectionCase.assignTo(assignToUserId);
        collectionCase = collectionCaseRepository.save(collectionCase);

        // Log activity
        CollectionActivity activity = CollectionActivity.builder()
                .tenantId(tenantId)
                .collectionCaseId(caseId)
                .activityType(ActivityType.STATUS_CHANGE)
                .notes("Case assigned to agent")
                .createdBy(assignedBy)
                .build();
        activityRepository.save(activity);

        log.info("Case {} assigned to user {}", caseId, assignToUserId);
        return enrichCaseResponse(collectionCase);
    }

    @Transactional
    public CollectionCaseResponse escalateCase(UUID caseId, String reason, UUID escalatedBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Escalating case: {}", caseId);

        CollectionCase collectionCase = collectionCaseRepository.findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Case", caseId));

        collectionCase.escalate();
        collectionCase = collectionCaseRepository.save(collectionCase);

        // Log activity
        CollectionActivity activity = CollectionActivity.builder()
                .tenantId(tenantId)
                .collectionCaseId(caseId)
                .activityType(ActivityType.ESCALATION)
                .notes(reason != null ? reason : "Case escalated")
                .createdBy(escalatedBy)
                .build();
        activityRepository.save(activity);

        log.info("Case {} escalated", caseId);
        return enrichCaseResponse(collectionCase);
    }

    @Transactional
    public CollectionCaseResponse resolveCase(UUID caseId, ResolveCollectionRequest request, UUID resolvedBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Resolving case: {}", caseId);

        CollectionCase collectionCase = collectionCaseRepository.findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection Case", caseId));

        collectionCase.resolve(request.getResolutionType(), request.getNotes(), resolvedBy);
        collectionCase = collectionCaseRepository.save(collectionCase);

        // Log activity
        CollectionActivity activity = CollectionActivity.builder()
                .tenantId(tenantId)
                .collectionCaseId(caseId)
                .activityType(ActivityType.STATUS_CHANGE)
                .notes("Case resolved: " + request.getResolutionType())
                .createdBy(resolvedBy)
                .build();
        activityRepository.save(activity);

        log.info("Case {} resolved with type: {}", caseId, request.getResolutionType());
        return enrichCaseResponse(collectionCase);
    }

    private CollectionCaseResponse enrichCaseResponse(CollectionCase collectionCase) {
        CollectionCaseResponse response = mapper.toResponse(collectionCase);

        // Manually set borrower info if loan is loaded
        if (collectionCase.getLoan() != null) {
            response.setLoanNumber(collectionCase.getLoan().getLoanNumber());
            if (collectionCase.getLoan().getBorrower() != null) {
                response.setBorrowerName(collectionCase.getLoan().getBorrower().getFullName());
                response.setBorrowerPhone(collectionCase.getLoan().getBorrower().getPhone());
            }
        }

        // Set assigned user name
        if (collectionCase.getAssignedUser() != null) {
            response.setAssignedToName(collectionCase.getAssignedUser().getFullName());
        }

        return response;
    }

    private String generateCaseNumber(UUID tenantId) {
        return "COL-" + caseSequence.incrementAndGet();
    }

    private String calculateDpdBucket(int dpd) {
        if (dpd <= 0) return "CURRENT";
        if (dpd <= 30) return "1-30";
        if (dpd <= 60) return "31-60";
        if (dpd <= 90) return "61-90";
        return "90+";
    }

    private CollectionPriority calculatePriority(int dpd) {
        if (dpd >= 90) return CollectionPriority.CRITICAL;
        if (dpd >= 60) return CollectionPriority.HIGH;
        if (dpd >= 30) return CollectionPriority.MEDIUM;
        return CollectionPriority.LOW;
    }
}
