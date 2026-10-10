package com.loanplatform.portal.service;

import com.loanplatform.auth.security.SecurityUtils;
import com.loanplatform.borrower.dto.BorrowerResponse;
import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.mapper.BorrowerMapper;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.loan.dto.LoanResponse;
import com.loanplatform.loan.dto.ScheduleResponse;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.RepaymentSchedule;
import com.loanplatform.loan.mapper.LoanMapper;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.payment.dto.PaymentResponse;
import com.loanplatform.payment.entity.Payment;
import com.loanplatform.payment.mapper.PaymentMapper;
import com.loanplatform.payment.repository.PaymentRepository;
import com.loanplatform.portal.dto.PortalSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Borrower self-service portal. Every query is scoped to the authenticated
 * borrower's linked borrower record (from the JWT) AND the current tenant
 * (TenantContext + Hibernate tenant filter), giving two layers of isolation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortalService {

    private final BorrowerRepository borrowerRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PaymentRepository paymentRepository;
    private final BorrowerMapper borrowerMapper;
    private final LoanMapper loanMapper;
    private final PaymentMapper paymentMapper;

    public BorrowerResponse getMyProfile() {
        UUID tenantId = TenantContext.requireTenant();
        UUID borrowerId = SecurityUtils.requireBorrowerId();

        Borrower borrower = borrowerRepository.findByIdAndTenantId(borrowerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", borrowerId));

        BorrowerResponse response = borrowerMapper.toResponse(borrower);
        response.setActiveLoansCount((int) loanRepository.countActiveLoansForBorrower(borrowerId));
        response.setTotalOutstanding(loanRepository.getTotalOutstandingForBorrower(borrowerId));
        return response;
    }

    public PortalSummaryResponse getMySummary() {
        UUID tenantId = TenantContext.requireTenant();
        UUID borrowerId = SecurityUtils.requireBorrowerId();

        long activeLoans = loanRepository.countActiveLoansForBorrower(borrowerId);
        BigDecimal totalOutstanding = loanRepository.getTotalOutstandingForBorrower(borrowerId);

        PortalSummaryResponse.NextEmi nextEmi = null;
        List<UUID> loanIds = loanRepository.findIdsByTenantIdAndBorrowerId(tenantId, borrowerId);
        if (!loanIds.isEmpty()) {
            nextEmi = scheduleRepository
                    .findUnpaidSchedulesByLoanIds(loanIds, PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .map(this::toNextEmi)
                    .orElse(null);
        }

        return PortalSummaryResponse.builder()
                .activeLoansCount(activeLoans)
                .totalOutstanding(totalOutstanding)
                .nextEmi(nextEmi)
                .build();
    }

    public PagedResponse<LoanResponse> getMyLoans(int page, int size, String sortBy, String sortDir) {
        UUID tenantId = TenantContext.requireTenant();
        UUID borrowerId = SecurityUtils.requireBorrowerId();

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        Page<Loan> loans = loanRepository.findByTenantIdAndBorrowerId(tenantId, borrowerId, pageable);

        List<LoanResponse> content = loans.getContent().stream()
                .map(loanMapper::toResponse)
                .toList();
        return PagedResponse.of(content, loans.getNumber(), loans.getSize(), loans.getTotalElements());
    }

    public LoanResponse getMyLoan(UUID loanId) {
        Loan loan = requireOwnedLoan(loanId);
        return loanMapper.toResponse(loan);
    }

    public List<ScheduleResponse> getMyLoanSchedule(UUID loanId) {
        requireOwnedLoan(loanId);
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(loanId);
        return loanMapper.toScheduleResponses(schedules);
    }

    public PagedResponse<PaymentResponse> getMyPayments(int page, int size) {
        UUID tenantId = TenantContext.requireTenant();
        UUID borrowerId = SecurityUtils.requireBorrowerId();

        List<UUID> loanIds = loanRepository.findIdsByTenantIdAndBorrowerId(tenantId, borrowerId);
        if (loanIds.isEmpty()) {
            return PagedResponse.of(List.of(), page, size, 0);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> payments = paymentRepository.findByTenantIdAndLoanIdIn(tenantId, loanIds, pageable);

        List<PaymentResponse> content = payments.getContent().stream()
                .map(paymentMapper::toResponse)
                .toList();
        return PagedResponse.of(content, payments.getNumber(), payments.getSize(), payments.getTotalElements());
    }

    public PaymentResponse getMyPayment(UUID paymentId) {
        UUID tenantId = TenantContext.requireTenant();
        UUID borrowerId = SecurityUtils.requireBorrowerId();

        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        // Ownership check via the payment's loan — 404 (not 403) to avoid leaking existence
        Loan loan = loanRepository.findByIdAndTenantId(payment.getLoanId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (!borrowerId.equals(loan.getBorrowerId())) {
            throw new ResourceNotFoundException("Payment", paymentId);
        }

        return paymentMapper.toResponse(payment);
    }

    /**
     * Fetches a loan only if it belongs to the authenticated borrower.
     * Returns 404 (not 403) for loans owned by someone else to avoid leaking existence.
     */
    private Loan requireOwnedLoan(UUID loanId) {
        UUID tenantId = TenantContext.requireTenant();
        UUID borrowerId = SecurityUtils.requireBorrowerId();

        Loan loan = loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!borrowerId.equals(loan.getBorrowerId())) {
            log.warn("Borrower {} attempted to access loan {} owned by another borrower", borrowerId, loanId);
            throw new ResourceNotFoundException("Loan", loanId);
        }
        return loan;
    }

    private PortalSummaryResponse.NextEmi toNextEmi(RepaymentSchedule schedule) {
        String loanNumber = schedule.getLoan() != null ? schedule.getLoan().getLoanNumber() : null;
        return PortalSummaryResponse.NextEmi.builder()
                .loanId(schedule.getLoanId())
                .loanNumber(loanNumber)
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .installmentAmount(schedule.getInstallmentAmount())
                .outstandingAmount(schedule.getOutstandingAmount())
                .status(schedule.getStatus().name())
                .daysPastDue(schedule.getDaysPastDue())
                .build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy) {
            case "dueDate", "disbursementDate", "status", "createdAt" -> sortBy;
            default -> "createdAt";
        };
        return "asc".equalsIgnoreCase(sortDir) ? Sort.by(field).ascending() : Sort.by(field).descending();
    }
}
