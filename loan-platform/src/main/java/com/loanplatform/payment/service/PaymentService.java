package com.loanplatform.payment.service;

import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.LoanStatus;
import com.loanplatform.loan.entity.RepaymentSchedule;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.payment.dto.PaymentResponse;
import com.loanplatform.payment.dto.RecordPaymentRequest;
import com.loanplatform.payment.engine.PaymentAllocationEngine;
import com.loanplatform.payment.entity.Payment;
import com.loanplatform.payment.entity.PaymentAllocation;
import com.loanplatform.payment.entity.PaymentStatus;
import com.loanplatform.payment.mapper.PaymentMapper;
import com.loanplatform.payment.repository.PaymentAllocationRepository;
import com.loanplatform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PaymentAllocationEngine allocationEngine;
    private final PaymentMapper paymentMapper;

    private final AtomicLong paymentNumberSequence = new AtomicLong(System.currentTimeMillis() % 100000);

    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request, UUID createdByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Recording payment of {} for loan: {}", request.getAmount(), request.getLoanId());

        if (request.getIdempotencyKey() != null &&
            paymentRepository.existsByIdempotencyKeyAndTenantId(request.getIdempotencyKey(), tenantId)) {
            log.warn("Duplicate payment request with idempotency key: {}", request.getIdempotencyKey());
            Payment existingPayment = paymentRepository
                    .findByIdempotencyKeyAndTenantId(request.getIdempotencyKey(), tenantId)
                    .orElseThrow();
            return paymentMapper.toResponse(existingPayment);
        }

        Loan loan = loanRepository.findByIdAndTenantId(request.getLoanId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", request.getLoanId()));

        if (!loan.isActive()) {
            throw new BusinessException("Cannot record payment for inactive loan", "LOAN_NOT_ACTIVE");
        }

        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .loanId(loan.getId())
                .paymentNumber(generatePaymentNumber(tenantId))
                .idempotencyKey(request.getIdempotencyKey())
                .paymentDate(request.getPaymentDate())
                .paymentTime(Instant.now())
                .amountPaid(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .transactionId(request.getTransactionId())
                .notes(request.getNotes())
                .metadata(request.getMetadata())
                .status(PaymentStatus.COMPLETED)
                .createdBy(createdByUserId)
                .build();

        payment = paymentRepository.save(payment);

        List<RepaymentSchedule> unpaidSchedules = scheduleRepository.findUnpaidSchedulesByLoanId(loan.getId());

        PaymentAllocationEngine.AllocationResult result = allocationEngine.allocatePayment(
                payment.getId(),
                tenantId,
                request.getAmount(),
                unpaidSchedules
        );

        payment.setPrincipalPaid(result.getTotalPrincipalAllocated());
        payment.setInterestPaid(result.getTotalInterestAllocated());
        payment.setPenaltyPaid(result.getTotalPenaltyAllocated());
        payment.setExcessAmount(result.getExcessAmount());
        payment.setReceiptNumber(generateReceiptNumber(tenantId));
        payment.setReceiptGenerated(true);

        paymentRepository.save(payment);

        allocationRepository.saveAll(result.getAllocations());

        scheduleRepository.saveAll(result.getUpdatedSchedules());

        updateLoanOutstandings(loan, result);
        loanRepository.save(loan);

        if (loan.getTotalOutstanding().compareTo(BigDecimal.ZERO) <= 0) {
            loan.close();
            loanRepository.save(loan);
            log.info("Loan {} fully paid and closed", loan.getId());
        }

        log.info("Payment recorded: {} for loan: {}", payment.getId(), loan.getId());

        return buildPaymentResponse(payment, result.getAllocations(), unpaidSchedules);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        UUID tenantId = TenantContext.requireTenant();
        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        List<PaymentAllocation> allocations = allocationRepository.findByPaymentId(paymentId);
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumber(payment.getLoanId());

        return buildPaymentResponse(payment, allocations, schedules);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getAllPayments(int page, int size, String sortBy, String sortDir) {
        UUID tenantId = TenantContext.requireTenant();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Payment> payments = paymentRepository.findAllByTenantId(tenantId, pageable);

        return PagedResponse.of(
                payments.getContent().stream()
                        .map(paymentMapper::toResponse)
                        .toList(),
                page,
                size,
                payments.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsForLoan(UUID loanId, int page, int size) {
        UUID tenantId = TenantContext.requireTenant();

        loanRepository.findByIdAndTenantId(loanId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("paymentDate").descending());
        Page<Payment> payments = paymentRepository.findByLoanId(loanId, pageable);

        return PagedResponse.of(
                payments.getContent().stream()
                        .map(paymentMapper::toResponse)
                        .toList(),
                page,
                size,
                payments.getTotalElements()
        );
    }

    @Transactional
    public PaymentResponse reversePayment(UUID paymentId, String reason, UUID reversedByUserId) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Reversing payment: {}", paymentId);

        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.isReversed()) {
            throw new BusinessException("Payment is already reversed", "ALREADY_REVERSED");
        }

        List<PaymentAllocation> allocations = allocationRepository.findByPaymentId(paymentId);

        for (PaymentAllocation allocation : allocations) {
            RepaymentSchedule schedule = scheduleRepository.findById(allocation.getScheduleId())
                    .orElseThrow();
            allocationEngine.reverseAllocation(schedule, allocation);
            scheduleRepository.save(schedule);
        }

        Loan loan = loanRepository.findById(payment.getLoanId()).orElseThrow();
        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().add(payment.getPrincipalPaid()));
        loan.setOutstandingInterest(loan.getOutstandingInterest().add(payment.getInterestPaid()));
        loan.setOutstandingPenalty(loan.getOutstandingPenalty().add(payment.getPenaltyPaid()));
        loan.setTotalPaid(loan.getTotalPaid().subtract(payment.getAmountPaid()));
        loan.setPrincipalPaid(loan.getPrincipalPaid().subtract(payment.getPrincipalPaid()));
        loan.setInterestPaid(loan.getInterestPaid().subtract(payment.getInterestPaid()));
        loan.setPenaltyPaid(loan.getPenaltyPaid().subtract(payment.getPenaltyPaid()));

        if (loan.getStatus() == LoanStatus.CLOSED) {
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setClosureDate(null);
        }

        loanRepository.save(loan);

        payment.reverse(reversedByUserId, reason);
        paymentRepository.save(payment);

        log.info("Payment reversed: {}", paymentId);

        return paymentMapper.toResponse(payment);
    }

    private void updateLoanOutstandings(Loan loan, PaymentAllocationEngine.AllocationResult result) {
        loan.setOutstandingPrincipal(
                loan.getOutstandingPrincipal().subtract(result.getTotalPrincipalAllocated())
        );
        loan.setOutstandingInterest(
                loan.getOutstandingInterest().subtract(result.getTotalInterestAllocated())
        );
        loan.setOutstandingPenalty(
                loan.getOutstandingPenalty().subtract(result.getTotalPenaltyAllocated())
        );

        BigDecimal totalAllocated = result.getTotalPrincipalAllocated()
                .add(result.getTotalInterestAllocated())
                .add(result.getTotalPenaltyAllocated());

        loan.setTotalPaid(loan.getTotalPaid().add(totalAllocated));
        loan.setPrincipalPaid(loan.getPrincipalPaid().add(result.getTotalPrincipalAllocated()));
        loan.setInterestPaid(loan.getInterestPaid().add(result.getTotalInterestAllocated()));
        loan.setPenaltyPaid(loan.getPenaltyPaid().add(result.getTotalPenaltyAllocated()));
    }

    private String generatePaymentNumber(UUID tenantId) {
        String prefix = tenantId.toString().substring(0, 4).toUpperCase();
        long seq = paymentNumberSequence.incrementAndGet();
        return String.format("PAY-%s-%06d", prefix, seq % 1000000);
    }

    private String generateReceiptNumber(UUID tenantId) {
        String prefix = tenantId.toString().substring(0, 4).toUpperCase();
        long seq = paymentNumberSequence.incrementAndGet();
        return String.format("RCP-%s-%06d", prefix, seq % 1000000);
    }

    private PaymentResponse buildPaymentResponse(Payment payment, List<PaymentAllocation> allocations,
                                                  List<RepaymentSchedule> schedules) {
        PaymentResponse response = paymentMapper.toResponse(payment);

        List<PaymentResponse.AllocationDetail> allocationDetails = allocations.stream()
                .map(allocation -> {
                    RepaymentSchedule schedule = schedules.stream()
                            .filter(s -> s.getId().equals(allocation.getScheduleId()))
                            .findFirst()
                            .orElse(null);

                    return PaymentResponse.AllocationDetail.builder()
                            .installmentNumber(schedule != null ? schedule.getInstallmentNumber() : null)
                            .dueDate(schedule != null ? schedule.getDueDate() : null)
                            .principalAllocated(allocation.getPrincipalAllocated())
                            .interestAllocated(allocation.getInterestAllocated())
                            .penaltyAllocated(allocation.getPenaltyAllocated())
                            .totalAllocated(allocation.getTotalAllocated())
                            .build();
                })
                .collect(Collectors.toList());

        response.setAllocations(allocationDetails);
        return response;
    }
}
