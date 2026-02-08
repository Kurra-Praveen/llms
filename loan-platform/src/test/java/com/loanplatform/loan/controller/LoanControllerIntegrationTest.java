package com.loanplatform.loan.controller;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.config.BaseIntegrationTest;
import com.loanplatform.loan.dto.CreateLoanRequest;
import com.loanplatform.loan.dto.DisburseLoanRequest;
import com.loanplatform.loan.entity.InterestType;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.LoanStatus;
import com.loanplatform.loan.entity.RepaymentFrequency;
import com.loanplatform.loan.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("LoanController Integration Tests")
class LoanControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    private Borrower testBorrower;

    @BeforeEach
    void setUpLoanTest() {
        testBorrower = createTestBorrower();
    }

    @Nested
    @DisplayName("Create Loan Tests")
    class CreateLoanTests {

        @Test
        @DisplayName("Should create loan successfully")
        void shouldCreateLoanSuccessfully() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principalAmount(new BigDecimal("100000"))
                    .interestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .repaymentFrequency(RepaymentFrequency.MONTHLY)
                    .build();

            mockMvc.perform(post("/v1/loans")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.loanNumber").isNotEmpty())
                    .andExpect(jsonPath("$.data.principalAmount").value(100000))
                    .andExpect(jsonPath("$.data.interestRate").value(12))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .build();

            mockMvc.perform(post("/v1/loans")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 without auth token")
        void shouldReturn403WithoutAuthToken() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principalAmount(new BigDecimal("100000"))
                    .interestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post("/v1/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Loan Tests")
    class GetLoanTests {

        @Test
        @DisplayName("Should get loan by ID")
        void shouldGetLoanById() throws Exception {
            Loan loan = createTestLoan();

            mockMvc.perform(get("/v1/loans/{id}", loan.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.loanNumber").value(loan.getLoanNumber()))
                    .andExpect(jsonPath("$.data.principalAmount").value(100000));
        }

        @Test
        @DisplayName("Should return 404 for non-existent loan")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/v1/loans/{id}", java.util.UUID.randomUUID())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should list all loans")
        void shouldListAllLoans() throws Exception {
            createTestLoan();
            createTestLoan();

            mockMvc.perform(get("/v1/loans")
                            .header("Authorization", getAuthHeader())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }

        @Test
        @DisplayName("Should get loans by borrower")
        void shouldGetLoansByBorrower() throws Exception {
            createTestLoan();

            mockMvc.perform(get("/v1/loans/borrower/{borrowerId}", testBorrower.getId())
                            .header("Authorization", getAuthHeader())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("Loan Approval Tests")
    class LoanApprovalTests {

        @Test
        @DisplayName("Should approve loan")
        void shouldApproveLoan() throws Exception {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.PENDING_APPROVAL);
            loanRepository.save(loan);

            mockMvc.perform(post("/v1/loans/{id}/approve", loan.getId())
                            .header("Authorization", getAuthHeader())
                            .param("notes", "Approved for testing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));
        }

        @Test
        @DisplayName("Should return 400 when approving active loan")
        void shouldReturn400WhenApprovingActiveLoan() throws Exception {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.ACTIVE);
            loanRepository.save(loan);

            mockMvc.perform(post("/v1/loans/{id}/approve", loan.getId())
                            .header("Authorization", getAuthHeader())
                            .param("notes", "Approved for testing"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Loan Disbursement Tests")
    class LoanDisbursementTests {

        @Test
        @DisplayName("Should disburse approved loan")
        void shouldDisburseApprovedLoan() throws Exception {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.APPROVED);
            loanRepository.save(loan);

            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .disbursementDate(LocalDate.now())
                    .build();

            mockMvc.perform(post("/v1/loans/{id}/disburse", loan.getId())
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.disbursementDate").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Loan Schedule Tests")
    class LoanScheduleTests {

        @Test
        @DisplayName("Should get loan schedule")
        void shouldGetLoanSchedule() throws Exception {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.APPROVED);
            loanRepository.save(loan);

            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .disbursementDate(LocalDate.now())
                    .build();

            mockMvc.perform(post("/v1/loans/{id}/disburse", loan.getId())
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            mockMvc.perform(get("/v1/loans/{id}/schedule", loan.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    private Borrower createTestBorrower() {
        Borrower borrower = Borrower.builder()
                .tenantId(testTenant.getId())
                .fullName("Test Borrower")
                .phone("9999" + System.nanoTime() % 1000000)
                .borrowerCode("BRW-TEST-" + System.nanoTime())
                .status(BorrowerStatus.ACTIVE)
                .riskScore(50)
                .build();
        return borrowerRepository.save(borrower);
    }

    private Loan createTestLoan() {
        Loan loan = Loan.builder()
                .tenantId(testTenant.getId())
                .borrowerId(testBorrower.getId())
                .loanNumber("LN-TEST-" + System.nanoTime())
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12"))
                .interestType(InterestType.REDUCING_BALANCE)
                .tenureMonths(12)
                .repaymentFrequency(RepaymentFrequency.MONTHLY)
                .emiAmount(new BigDecimal("8884.88"))
                .totalInterest(new BigDecimal("6618.56"))
                .totalPayable(new BigDecimal("106618.56"))
                .status(LoanStatus.DRAFT)
                .outstandingPrincipal(BigDecimal.ZERO)
                .outstandingInterest(BigDecimal.ZERO)
                .outstandingPenalty(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .principalPaid(BigDecimal.ZERO)
                .interestPaid(BigDecimal.ZERO)
                .penaltyPaid(BigDecimal.ZERO)
                .gracePeriodDays(0)
                .applicationDate(LocalDate.now())
                .build();
        return loanRepository.save(loan);
    }
}
