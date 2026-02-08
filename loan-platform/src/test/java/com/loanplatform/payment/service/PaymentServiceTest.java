package com.loanplatform.payment.service;

import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.loan.entity.*;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.payment.dto.PaymentResponse;
import com.loanplatform.payment.dto.RecordPaymentRequest;
import com.loanplatform.payment.engine.PaymentAllocationEngine;
import com.loanplatform.payment.entity.Payment;
import com.loanplatform.payment.entity.PaymentMethod;
import com.loanplatform.payment.entity.PaymentStatus;
import com.loanplatform.payment.mapper.PaymentMapper;
import com.loanplatform.payment.repository.PaymentAllocationRepository;
import com.loanplatform.payment.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationRepository allocationRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private PaymentAllocationEngine allocationEngine;

    @Mock
    private PaymentMapper paymentMapper;

    private PaymentService paymentService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                allocationRepository,
                loanRepository,
                scheduleRepository,
                allocationEngine,
                paymentMapper
        );
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::requireTenant).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Nested
    @DisplayName("Record Payment Tests")
    class RecordPaymentTests {

        @Test
        @DisplayName("Should record payment successfully")
        void shouldRecordPaymentSuccessfully() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(LOAN_ID)
                    .amount(new BigDecimal("10000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .build();

            Loan loan = createTestLoan();
            Payment payment = createTestPayment();
            PaymentResponse expectedResponse = createPaymentResponse();

            PaymentAllocationEngine.AllocationResult allocationResult = PaymentAllocationEngine.AllocationResult.builder()
                    .totalPrincipalAllocated(new BigDecimal("8000"))
                    .totalInterestAllocated(new BigDecimal("2000"))
                    .totalPenaltyAllocated(BigDecimal.ZERO)
                    .excessAmount(BigDecimal.ZERO)
                    .allocations(Collections.emptyList())
                    .updatedSchedules(Collections.emptyList())
                    .build();

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(scheduleRepository.findUnpaidSchedulesByLoanId(LOAN_ID)).thenReturn(Collections.emptyList());
            when(allocationEngine.allocatePayment(any(), any(), any(), anyList())).thenReturn(allocationResult);
            when(loanRepository.save(any(Loan.class))).thenReturn(loan);
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

            PaymentResponse response = paymentService.recordPayment(request, USER_ID);

            assertThat(response).isNotNull();
            verify(paymentRepository, times(2)).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should handle idempotency key")
        void shouldHandleIdempotencyKey() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(LOAN_ID)
                    .amount(new BigDecimal("10000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .idempotencyKey("unique-key-123")
                    .build();

            Payment existingPayment = createTestPayment();
            PaymentResponse expectedResponse = createPaymentResponse();

            when(paymentRepository.existsByIdempotencyKeyAndTenantId("unique-key-123", TENANT_ID)).thenReturn(true);
            when(paymentRepository.findByIdempotencyKeyAndTenantId("unique-key-123", TENANT_ID))
                    .thenReturn(Optional.of(existingPayment));
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

            PaymentResponse response = paymentService.recordPayment(request, USER_ID);

            assertThat(response).isNotNull();
            verify(loanRepository, never()).findByIdAndTenantId(any(), any());
        }

        @Test
        @DisplayName("Should throw exception for inactive loan")
        void shouldThrowExceptionForInactiveLoan() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(LOAN_ID)
                    .amount(new BigDecimal("10000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .build();

            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.CLOSED);

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> paymentService.recordPayment(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("inactive loan");
        }

        @Test
        @DisplayName("Should throw exception when loan not found")
        void shouldThrowExceptionWhenLoanNotFound() {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(LOAN_ID)
                    .amount(new BigDecimal("10000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .build();

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.recordPayment(request, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Payment Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should get payment by ID")
        void shouldGetPaymentById() {
            Payment payment = createTestPayment();
            PaymentResponse expectedResponse = createPaymentResponse();

            when(paymentRepository.findByIdAndTenantId(PAYMENT_ID, TENANT_ID)).thenReturn(Optional.of(payment));
            when(allocationRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Collections.emptyList());
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(LOAN_ID)).thenReturn(Collections.emptyList());
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

            PaymentResponse response = paymentService.getPaymentById(PAYMENT_ID);

            assertThat(response).isNotNull();
            verify(paymentRepository).findByIdAndTenantId(PAYMENT_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(paymentRepository.findByIdAndTenantId(PAYMENT_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById(PAYMENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Reverse Payment Tests")
    class ReversePaymentTests {

        @Test
        @DisplayName("Should reverse payment successfully")
        void shouldReversePaymentSuccessfully() {
            Payment payment = createTestPayment();
            Loan loan = createTestLoan();
            PaymentResponse expectedResponse = createPaymentResponse();
            expectedResponse.setStatus("REVERSED");

            when(paymentRepository.findByIdAndTenantId(PAYMENT_ID, TENANT_ID)).thenReturn(Optional.of(payment));
            when(allocationRepository.findByPaymentId(PAYMENT_ID)).thenReturn(Collections.emptyList());
            when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(Loan.class))).thenReturn(loan);
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

            PaymentResponse response = paymentService.reversePayment(PAYMENT_ID, "Test reversal", USER_ID);

            assertThat(response).isNotNull();
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should throw exception when reversing already reversed payment")
        void shouldThrowExceptionWhenAlreadyReversed() {
            Payment payment = createTestPayment();
            payment.setStatus(PaymentStatus.REVERSED);
            payment.setIsReversed(true);

            when(paymentRepository.findByIdAndTenantId(PAYMENT_ID, TENANT_ID)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.reversePayment(PAYMENT_ID, "Test reversal", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already reversed");
        }
    }

    private Loan createTestLoan() {
        return Loan.builder()
                .id(LOAN_ID)
                .tenantId(TENANT_ID)
                .loanNumber("LN-TEST-000001")
                .principalAmount(new BigDecimal("100000"))
                .status(LoanStatus.ACTIVE)
                .outstandingPrincipal(new BigDecimal("90000"))
                .outstandingInterest(new BigDecimal("5000"))
                .outstandingPenalty(BigDecimal.ZERO)
                .totalPaid(new BigDecimal("15000"))
                .principalPaid(new BigDecimal("10000"))
                .interestPaid(new BigDecimal("5000"))
                .penaltyPaid(BigDecimal.ZERO)
                .build();
    }

    private Payment createTestPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .tenantId(TENANT_ID)
                .loanId(LOAN_ID)
                .paymentNumber("PAY-TEST-000001")
                .amountPaid(new BigDecimal("10000"))
                .principalPaid(new BigDecimal("8000"))
                .interestPaid(new BigDecimal("2000"))
                .penaltyPaid(BigDecimal.ZERO)
                .paymentDate(LocalDate.now())
                .paymentTime(Instant.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.COMPLETED)
                .build();
    }

    private PaymentResponse createPaymentResponse() {
        return PaymentResponse.builder()
                .id(PAYMENT_ID)
                .loanId(LOAN_ID)
                .paymentNumber("PAY-TEST-000001")
                .amountPaid(new BigDecimal("10000"))
                .principalPaid(new BigDecimal("8000"))
                .interestPaid(new BigDecimal("2000"))
                .status("COMPLETED")
                .build();
    }
}
