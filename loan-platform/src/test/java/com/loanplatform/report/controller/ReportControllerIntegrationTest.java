package com.loanplatform.report.controller;

import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.config.BaseIntegrationTest;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.LoanStatus;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.payment.entity.Payment;
import com.loanplatform.payment.entity.PaymentMethod;
import com.loanplatform.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ReportController Integration Tests")
class ReportControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @BeforeEach
    void setupData() {
        // Create borrower first
        Borrower borrower = Borrower.builder()
                .tenantId(testTenant.getId())
                .borrowerCode("BOR-001")
                .fullName("Test Borrower")
                .phone("9876543210")
                .email("borrower@example.com")
                .status(com.loanplatform.borrower.entity.BorrowerStatus.ACTIVE)
                .build();
        borrowerRepository.save(borrower);

        // Create an active loan
        Loan loan = Loan.builder()
                .tenantId(testTenant.getId())
                .borrowerId(borrower.getId())
                .loanNumber("LOAN-001")
                .principalAmount(new BigDecimal("10000.00"))
                .outstandingPrincipal(new BigDecimal("9000.00"))
                .status(LoanStatus.ACTIVE)
                .interestRate(new BigDecimal("10.0"))
                .interestType(com.loanplatform.loan.entity.InterestType.FLAT)
                .tenureMonths(12)
                .applicationDate(LocalDate.now())
                .build();
        loanRepository.save(loan);

        // Create a payment
        Payment payment = Payment.builder()
                .tenantId(testTenant.getId())
                .loanId(loan.getId())
                .paymentNumber("PAY-001")
                .amountPaid(new BigDecimal("1000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .status(com.loanplatform.payment.entity.PaymentStatus.COMPLETED)
                .build();
        paymentRepository.save(payment);
    }

    @Test
    @DisplayName("Should get portfolio summary")
    void shouldGetPortfolioSummary() throws Exception {
        mockMvc.perform(get("/v1/reports/portfolio-summary")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeLoansCount").value(1))
                .andExpect(jsonPath("$.data.totalDisbursed").value(10000.0));
    }

    @Test
    @DisplayName("Should get daily collections")
    void shouldGetDailyCollections() throws Exception {
        mockMvc.perform(get("/v1/reports/daily-collections?days=7")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(7));
    }
}
