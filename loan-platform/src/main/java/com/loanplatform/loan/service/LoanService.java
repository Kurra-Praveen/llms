package com.loanplatform.loan.service;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.loan.dto.*;
import com.loanplatform.loan.engine.InterestCalculationEngine;
import com.loanplatform.loan.engine.ScheduleEntry;
import com.loanplatform.loan.engine.ScheduleGenerationRequest;
import com.loanplatform.loan.entity.*;
import com.loanplatform.loan.mapper.LoanMapper;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final BorrowerRepository borrowerRepository;
    private final TenantRepository tenantRepository;
    private final LoanMapper loanMapper;
    private final InterestCalculationEngine interestEngine;

    private final AtomicLong loanNumberSequence = new AtomicLong(System.currentTimeMillis() % 100000);

    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request, UUID createdByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Creating loan for borrower: {} in tenant: {}", request.getBorrowerId(), tenantId);

        validateTenantLimits(tenantId);

        Borrower borrower = borrowerRepository.findByIdAndTenantId(request.getBorrowerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", request.getBorrowerId()));

        if (!borrower.isActive()) {
            throw new BusinessException("Cannot create loan for inactive borrower", "BORROWER_INACTIVE");
        }

        Loan loan = loanMapper.toEntity(request);
        loan.setTenantId(tenantId);
        loan.setLoanNumber(generateLoanNumber(tenantId));
        loan.setCreatedBy(createdByUserId);

        if (request.getApplicationDate() == null) {
            loan.setApplicationDate(LocalDate.now());
        }

        if (loan.getGracePeriodDays() == null) {
            loan.setGracePeriodDays(0);
        }

        BigDecimal emi = interestEngine.calculateEmi(
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTenureMonths(),
                loan.getInterestType(),
                loan.getDailyFixedAmount()
        );
        loan.setEmiAmount(emi);

        BigDecimal totalInterest = interestEngine.calculateTotalInterest(
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTenureMonths(),
                loan.getInterestType(),
                loan.getDailyFixedAmount()
        );
        loan.setTotalInterest(totalInterest);
        loan.setTotalPayable(loan.getPrincipalAmount().add(totalInterest));

        loan = loanRepository.save(loan);
        log.info("Loan created: {} for tenant: {}", loan.getId(), tenantId);

        return enrichLoanResponse(loanMapper.toResponse(loan), loan);
    }

    @Transactional
    public LoanResponse disburseLoan(UUID loanId, DisburseLoanRequest request, UUID disbursedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Disbursing loan: {}", loanId);

        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (!loan.canDisburse()) {
            throw new BusinessException("Loan cannot be disbursed in current status: " + loan.getStatus(),
                    "INVALID_LOAN_STATUS");
        }

        // Use entity's disburse method which calculates charges and net disbursement
        loan.disburse(request.getDisbursementDate());
        loan.setUpdatedBy(disbursedByUserId);

        LocalDate firstPaymentDate = request.getFirstPaymentDate() != null
                ? request.getFirstPaymentDate()
                : calculateFirstPaymentDate(request.getDisbursementDate(), loan.getRepaymentFrequency(), loan.getGracePeriodDays());

        loan.setFirstPaymentDate(firstPaymentDate);

        List<RepaymentSchedule> schedules = generateSchedules(loan, firstPaymentDate);
        scheduleRepository.saveAll(schedules);

        if (!schedules.isEmpty()) {
            loan.setMaturityDate(schedules.get(schedules.size() - 1).getDueDate());
        }

        loan = loanRepository.save(loan);
        log.info("Loan disbursed: {} with {} installments", loanId, schedules.size());

        LoanResponse response = loanMapper.toResponse(loan);
        response.setSchedules(loanMapper.toScheduleResponses(schedules));
        return enrichLoanResponse(response, loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(UUID loanId) {
        UUID tenantId = TenantContext.requireTenant();
        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId);

        LoanResponse response = loanMapper.toResponse(loan);
        response.setSchedules(loanMapper.toScheduleResponses(schedules));
        return enrichLoanResponse(response, loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanByNumber(String loanNumber) {
        UUID tenantId = TenantContext.requireTenant();
        Loan loan = loanRepository.findByLoanNumberAndTenantId(loanNumber, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanNumber));

        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loan.getId());

        LoanResponse response = loanMapper.toResponse(loan);
        response.setSchedules(loanMapper.toScheduleResponses(schedules));
        return enrichLoanResponse(response, loan);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LoanResponse> getAllLoans(int page, int size, String sortBy, String sortDir) {
        UUID tenantId = TenantContext.requireTenant();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Loan> loans = loanRepository.findAllByTenantId(tenantId, pageable);

        return PagedResponse.of(
                loans.getContent().stream()
                        .map(loan -> enrichLoanResponse(loanMapper.toResponse(loan), loan))
                        .toList(),
                page,
                size,
                loans.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<LoanResponse> getLoansByBorrower(UUID borrowerId, int page, int size) {
        UUID tenantId = TenantContext.requireTenant();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Loan> loans = loanRepository.findByTenantIdAndBorrowerId(tenantId, borrowerId, pageable);

        return PagedResponse.of(
                loans.getContent().stream()
                        .map(loan -> enrichLoanResponse(loanMapper.toResponse(loan), loan))
                        .toList(),
                page,
                size,
                loans.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<LoanResponse> getLoansByStatus(LoanStatus status, int page, int size) {
        UUID tenantId = TenantContext.requireTenant();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Loan> loans = loanRepository.findByTenantIdAndStatus(tenantId, status, pageable);

        return PagedResponse.of(
                loans.getContent().stream()
                        .map(loan -> enrichLoanResponse(loanMapper.toResponse(loan), loan))
                        .toList(),
                page,
                size,
                loans.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getLoanSchedule(UUID loanId) {
        UUID tenantId = TenantContext.requireTenant();

        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId);
        return loanMapper.toScheduleResponses(schedules);
    }

    @Transactional
    public LoanResponse approveLoan(UUID loanId, String approvalNotes, UUID approvedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getStatus() != LoanStatus.PENDING_APPROVAL && loan.getStatus() != LoanStatus.DRAFT) {
            throw new BusinessException("Loan cannot be approved in current status", "INVALID_LOAN_STATUS");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());
        loan.setApprovedBy(approvedByUserId);
        loan.setApprovalNotes(approvalNotes);
        loan.setUpdatedBy(approvedByUserId);

        loan = loanRepository.save(loan);
        log.info("Loan approved: {}", loanId);

        return enrichLoanResponse(loanMapper.toResponse(loan), loan);
    }

    @Transactional
    public void closeLoan(UUID loanId, UUID closedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        if (loan.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot close loan with outstanding balance", "OUTSTANDING_BALANCE");
        }

        loan.close();
        loan.setUpdatedBy(closedByUserId);
        loanRepository.save(loan);
        log.info("Loan closed: {}", loanId);
    }

    private List<RepaymentSchedule> generateSchedules(Loan loan, LocalDate startDate) {
        ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                .principal(loan.getPrincipalAmount())
                .annualInterestRate(loan.getInterestRate())
                .dailyFixedAmount(loan.getDailyFixedAmount())
                .interestType(loan.getInterestType())
                .tenureMonths(loan.getTenureMonths())
                .frequency(loan.getRepaymentFrequency())
                .startDate(startDate)
                .gracePeriodDays(loan.getGracePeriodDays() != null ? loan.getGracePeriodDays() : 0)
                .build();

        List<ScheduleEntry> entries = interestEngine.generateSchedule(request);

        return entries.stream()
                .map(entry -> RepaymentSchedule.builder()
                        .tenantId(loan.getTenantId())
                        .loanId(loan.getId())
                        .installmentNumber(entry.getInstallmentNumber())
                        .dueDate(entry.getDueDate())
                        .principalComponent(entry.getPrincipalComponent())
                        .interestComponent(entry.getInterestComponent())
                        .installmentAmount(entry.getInstallmentAmount())
                        .outstandingAfter(entry.getOutstandingAfter())
                        .status(ScheduleStatus.PENDING)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private String generateLoanNumber(UUID tenantId) {
        String prefix = tenantId.toString().substring(0, 4).toUpperCase();
        long seq = loanNumberSequence.incrementAndGet();
        return String.format("LN-%s-%06d", prefix, seq % 1000000);
    }

    private LocalDate calculateFirstPaymentDate(LocalDate disbursementDate, RepaymentFrequency frequency, Integer graceDays) {
        LocalDate baseDate = disbursementDate.plusDays(graceDays != null ? graceDays : 0);
        return switch (frequency) {
            case DAILY -> baseDate.plusDays(1);
            case WEEKLY -> baseDate.plusWeeks(1);
            case BI_WEEKLY -> baseDate.plusWeeks(2);
            case MONTHLY -> baseDate.plusMonths(1);
            case QUARTERLY -> baseDate.plusMonths(3);
        };
    }

    private void validateTenantLimits(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        long currentCount = loanRepository.countByTenantId(tenantId);
        if (currentCount >= tenant.getMaxLoans()) {
            throw new BusinessException("Maximum loan limit reached for this tenant", "LIMIT_EXCEEDED");
        }
    }

    private LoanResponse enrichLoanResponse(LoanResponse response, Loan loan) {
        if (loan.getBorrower() != null) {
            response.setBorrowerName(loan.getBorrower().getFullName());
            response.setBorrowerCode(loan.getBorrower().getBorrowerCode());
        }
        return response;
    }
}
