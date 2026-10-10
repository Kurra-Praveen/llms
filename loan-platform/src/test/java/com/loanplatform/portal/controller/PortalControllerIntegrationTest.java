package com.loanplatform.portal.controller;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.entity.User;
import com.loanplatform.config.BaseIntegrationTest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Borrower portal isolation tests — the safety net for the highest-severity
 * mobile risk: a borrower must NEVER see another borrower's data.
 */
@DisplayName("PortalController Borrower Isolation Tests")
class PortalControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private LoanRepository loanRepository;

    private Borrower borrowerA;
    private Borrower borrowerB;
    private User borrowerUserA;
    private String borrowerTokenA;
    private Loan loanA;
    private Loan loanB;

    @BeforeEach
    void setUpPortal() {
        borrowerA = createBorrower("Alice Borrower", "9000000001");
        borrowerB = createBorrower("Bob Borrower", "9000000002");
        borrowerUserA = createBorrowerUser(borrowerA, "alice@example.com");
        borrowerTokenA = jwtService.generateAccessToken(borrowerUserA);
        loanA = createLoan(borrowerA, "LN-A-" + UUID.randomUUID().toString().substring(0, 6));
        loanB = createLoan(borrowerB, "LN-B-" + UUID.randomUUID().toString().substring(0, 6));
    }

    @Nested
    @DisplayName("Own-data access")
    class OwnDataAccess {

        @Test
        @DisplayName("Borrower can view own profile")
        void borrowerCanViewOwnProfile() throws Exception {
            mockMvc.perform(get("/v1/portal/me")
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fullName").value("Alice Borrower"));
        }

        @Test
        @DisplayName("Borrower loan list contains only own loans")
        void borrowerLoanListContainsOnlyOwnLoans() throws Exception {
            mockMvc.perform(get("/v1/portal/loans")
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].loanNumber").value(loanA.getLoanNumber()));
        }

        @Test
        @DisplayName("Borrower can view own loan details")
        void borrowerCanViewOwnLoan() throws Exception {
            mockMvc.perform(get("/v1/portal/loans/{id}", loanA.getId())
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.loanNumber").value(loanA.getLoanNumber()));
        }

        @Test
        @DisplayName("Borrower can view own loan schedule")
        void borrowerCanViewOwnSchedule() throws Exception {
            mockMvc.perform(get("/v1/portal/loans/{id}/schedule", loanA.getId())
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Borrower can view summary")
        void borrowerCanViewSummary() throws Exception {
            mockMvc.perform(get("/v1/portal/summary")
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.activeLoansCount").isNumber());
        }
    }

    @Nested
    @DisplayName("Cross-borrower isolation")
    class CrossBorrowerIsolation {

        @Test
        @DisplayName("Borrower A gets 404 (not data) for borrower B's loan")
        void borrowerACannotAccessBorrowerBLoan() throws Exception {
            mockMvc.perform(get("/v1/portal/loans/{id}", loanB.getId())
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Borrower A gets 404 for borrower B's loan schedule")
        void borrowerACannotAccessBorrowerBSchedule() throws Exception {
            mockMvc.perform(get("/v1/portal/loans/{id}/schedule", loanB.getId())
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Borrower A gets 404 for a random loan ID")
        void borrowerAGets404ForRandomLoan() throws Exception {
            mockMvc.perform(get("/v1/portal/loans/{id}", UUID.randomUUID())
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Borrower A's payment history excludes borrower B's payments")
        void borrowerPaymentsAreIsolated() throws Exception {
            mockMvc.perform(get("/v1/portal/payments")
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Role enforcement")
    class RoleEnforcement {

        @Test
        @DisplayName("Staff role is forbidden from portal endpoints")
        void staffCannotAccessPortal() throws Exception {
            mockMvc.perform(get("/v1/portal/me")
                            .header("Authorization", getAuthHeader())) // LENDER_ADMIN token
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request is rejected")
        void unauthenticatedIsRejected() throws Exception {
            mockMvc.perform(get("/v1/portal/me"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Borrower role is forbidden from staff endpoints")
        void borrowerCannotAccessStaffEndpoints() throws Exception {
            mockMvc.perform(get("/v1/borrowers")
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/v1/loans")
                            .header("Authorization", bearer(borrowerTokenA)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Borrower createBorrower(String name, String phone) {
        Borrower borrower = Borrower.builder()
                .tenantId(testTenant.getId())
                .borrowerCode("BRW-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName(name)
                .phone(phone)
                .build();
        return borrowerRepository.save(borrower);
    }

    private User createBorrowerUser(Borrower borrower, String email) {
        User user = User.builder()
                .tenantId(testTenant.getId())
                .borrowerId(borrower.getId())
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName(borrower.getFullName().split(" ")[0])
                .lastName("Portal")
                .role(Role.BORROWER)
                .build();
        return userRepository.save(user);
    }

    private Loan createLoan(Borrower borrower, String loanNumber) {
        Loan loan = Loan.builder()
                .tenantId(testTenant.getId())
                .borrowerId(borrower.getId())
                .loanNumber(loanNumber)
                .principalAmount(new BigDecimal("10000"))
                .interestRate(new BigDecimal("12"))
                .interestType(InterestType.FLAT)
                .tenureMonths(10)
                .repaymentFrequency(RepaymentFrequency.MONTHLY)
                .applicationDate(LocalDate.now())
                .status(LoanStatus.ACTIVE)
                .build();
        return loanRepository.save(loan);
    }
}
