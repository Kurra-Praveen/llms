package com.loanplatform.payment.controller;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.config.BaseIntegrationTest;
import com.loanplatform.loan.entity.*;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.payment.dto.RecordPaymentRequest;
import com.loanplatform.payment.entity.Payment;
import com.loanplatform.payment.entity.PaymentMethod;
import com.loanplatform.payment.entity.PaymentStatus;
import com.loanplatform.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PaymentController Integration Tests")
class PaymentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    private Borrower testBorrower;
    private Loan testLoan;

    @BeforeEach
    void setUpPaymentTest() {
        testBorrower = createTestBorrower();
        testLoan = createTestLoan();
    }

    @Nested
    @DisplayName("Record Payment Tests")
    class RecordPaymentTests {

        @Test
        @DisplayName("Should record payment successfully")
        void shouldRecordPaymentSuccessfully() throws Exception {
            createTestSchedule();

            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(testLoan.getId())
                    .amount(new BigDecimal("10000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .build();

            mockMvc.perform(post("/v1/payments")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentNumber").isNotEmpty())
                    .andExpect(jsonPath("$.data.amountPaid").value(10000))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should handle idempotency key")
        void shouldHandleIdempotencyKey() throws Exception {
            createTestSchedule();
            String idempotencyKey = UUID.randomUUID().toString();

            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(testLoan.getId())
                    .amount(new BigDecimal("5000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.CASH)
                    .idempotencyKey(idempotencyKey)
                    .build();

            // First request
            mockMvc.perform(post("/v1/payments")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Second request with same idempotency key - should return same result
            mockMvc.perform(post("/v1/payments")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(testLoan.getId())
                    .build();

            mockMvc.perform(post("/v1/payments")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 without auth token")
        void shouldReturn403WithoutAuthToken() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .loanId(testLoan.getId())
                    .amount(new BigDecimal("10000"))
                    .paymentDate(LocalDate.now())
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .build();

            mockMvc.perform(post("/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Payment Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should get payment by ID")
        void shouldGetPaymentById() throws Exception {
            Payment payment = createTestPayment();

            mockMvc.perform(get("/v1/payments/{id}", payment.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentNumber").value(payment.getPaymentNumber()))
                    .andExpect(jsonPath("$.data.amountPaid").value(10000));
        }

        @Test
        @DisplayName("Should return 404 for non-existent payment")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/v1/payments/{id}", UUID.randomUUID())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should list all payments")
        void shouldListAllPayments() throws Exception {
            createTestPayment();

            mockMvc.perform(get("/v1/payments")
                            .header("Authorization", getAuthHeader())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }

        @Test
        @DisplayName("Should get payments for loan")
        void shouldGetPaymentsForLoan() throws Exception {
            createTestPayment();

            mockMvc.perform(get("/v1/payments/loan/{loanId}", testLoan.getId())
                            .header("Authorization", getAuthHeader())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("Reverse Payment Tests")
    class ReversePaymentTests {

        @Test
        @DisplayName("Should reverse payment")
        void shouldReversePayment() throws Exception {
            Payment payment = createTestPayment();

            mockMvc.perform(post("/v1/payments/{id}/reverse", payment.getId())
                            .header("Authorization", getAuthHeader())
                            .param("reason", "Test reversal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("REVERSED"));
        }

        @Test
        @DisplayName("Should return 400 when reversing already reversed payment")
        void shouldReturn400WhenAlreadyReversed() throws Exception {
            Payment payment = createTestPayment();
            payment.setStatus(PaymentStatus.REVERSED);
            payment.setIsReversed(true);
            paymentRepository.save(payment);

            mockMvc.perform(post("/v1/payments/{id}/reverse", payment.getId())
                            .header("Authorization", getAuthHeader())
                            .param("reason", "Test reversal"))
                    .andExpect(status().isBadRequest());
        }
    }

    private Borrower createTestBorrower() {
        Borrower borrower = Borrower.builder()
                .tenantId(testTenant.getId())
                .fullName("Payment Test Borrower")
                .phone("8888" + System.nanoTime() % 1000000)
                .borrowerCode("BRW-PAY-" + System.nanoTime())
                .status(BorrowerStatus.ACTIVE)
                .riskScore(50)
                .build();
        return borrowerRepository.save(borrower);
    }

    private Loan createTestLoan() {
        Loan loan = Loan.builder()
                .tenantId(testTenant.getId())
                .borrowerId(testBorrower.getId())
                .loanNumber("LN-PAY-" + System.nanoTime())
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12"))
                .interestType(InterestType.REDUCING_BALANCE)
                .tenureMonths(12)
                .repaymentFrequency(RepaymentFrequency.MONTHLY)
                .emiAmount(new BigDecimal("8884.88"))
                .totalInterest(new BigDecimal("6618.56"))
                .totalPayable(new BigDecimal("106618.56"))
                .status(LoanStatus.ACTIVE)
                .outstandingPrincipal(new BigDecimal("100000"))
                .outstandingInterest(new BigDecimal("6618.56"))
                .outstandingPenalty(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .principalPaid(BigDecimal.ZERO)
                .interestPaid(BigDecimal.ZERO)
                .penaltyPaid(BigDecimal.ZERO)
                .gracePeriodDays(0)
                .applicationDate(LocalDate.now().minusDays(10))
                .disbursementDate(LocalDate.now().minusDays(5))
                .firstPaymentDate(LocalDate.now())
                .build();
        return loanRepository.save(loan);
    }

    private void createTestSchedule() {
        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .tenantId(testTenant.getId())
                .loanId(testLoan.getId())
                .installmentNumber(1)
                .dueDate(LocalDate.now())
                .principalComponent(new BigDecimal("7500"))
                .interestComponent(new BigDecimal("1000"))
                .installmentAmount(new BigDecimal("8500"))
                .outstandingAfter(new BigDecimal("92500"))
                .status(ScheduleStatus.PENDING)
                .principalPaid(BigDecimal.ZERO)
                .interestPaid(BigDecimal.ZERO)
                .penaltyPaid(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .build();
        scheduleRepository.save(schedule);
    }

    private Payment createTestPayment() {
        Payment payment = Payment.builder()
                .tenantId(testTenant.getId())
                .loanId(testLoan.getId())
                .paymentNumber("PAY-TEST-" + System.nanoTime())
                .amountPaid(new BigDecimal("10000"))
                .principalPaid(new BigDecimal("8000"))
                .interestPaid(new BigDecimal("2000"))
                .penaltyPaid(BigDecimal.ZERO)
                .excessAmount(BigDecimal.ZERO)
                .paymentDate(LocalDate.now())
                .paymentTime(Instant.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.COMPLETED)
                .receiptNumber("RCP-TEST-" + System.nanoTime())
                .receiptGenerated(true)
                .build();
        return paymentRepository.save(payment);
    }
}
