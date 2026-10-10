package com.loanplatform.loan.service;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.loan.dto.CreateLoanRequest;
import com.loanplatform.loan.dto.DisburseLoanRequest;
import com.loanplatform.loan.dto.LoanResponse;
import com.loanplatform.loan.engine.InterestCalculationEngine;
import com.loanplatform.loan.entity.InterestType;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.LoanStatus;
import com.loanplatform.loan.entity.RepaymentFrequency;
import com.loanplatform.loan.mapper.LoanMapper;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.repository.TenantRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LoanMapper loanMapper;

    @Mock
    private InterestCalculationEngine interestEngine;

    private LoanService loanService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID BORROWER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(
                loanRepository,
                scheduleRepository,
                borrowerRepository,
                tenantRepository,
                loanMapper,
                interestEngine
        );
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::requireTenant).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Nested
    @DisplayName("Create Loan Tests")
    class CreateLoanTests {

        @Test
        @DisplayName("Should create loan successfully")
        void shouldCreateLoanSuccessfully() {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(BORROWER_ID)
                    .principalAmount(new BigDecimal("100000"))
                    .interestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .repaymentFrequency(RepaymentFrequency.MONTHLY)
                    .build();

            Tenant tenant = createTestTenant();
            Borrower borrower = createTestBorrower();
            Loan loan = createTestLoan();
            LoanResponse expectedResponse = createLoanResponse();

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(loanRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(loanMapper.toEntity(any(CreateLoanRequest.class))).thenReturn(loan);
            when(interestEngine.calculateEmi(any(), any(), anyInt(), any(), any())).thenReturn(new BigDecimal("8884.88"));
            when(interestEngine.calculateTotalInterest(any(), any(), anyInt(), any(), any())).thenReturn(new BigDecimal("6618.56"));
            when(loanRepository.save(any(Loan.class))).thenReturn(loan);
            when(loanMapper.toResponse(any(Loan.class))).thenReturn(expectedResponse);

            LoanResponse response = loanService.createLoan(request, USER_ID);

            assertThat(response).isNotNull();
            verify(loanRepository).save(any(Loan.class));
        }

        @Test
        @DisplayName("Should throw exception for inactive borrower")
        void shouldThrowExceptionForInactiveBorrower() {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(BORROWER_ID)
                    .principalAmount(new BigDecimal("100000"))
                    .interestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .build();

            Tenant tenant = createTestTenant();
            Borrower borrower = createTestBorrower();
            borrower.setStatus(BorrowerStatus.BLOCKED);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(loanRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));

            assertThatThrownBy(() -> loanService.createLoan(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("inactive borrower");
        }

        @Test
        @DisplayName("Should throw exception when loan limit exceeded")
        void shouldThrowExceptionWhenLimitExceeded() {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(BORROWER_ID)
                    .principalAmount(new BigDecimal("100000"))
                    .interestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .build();

            Tenant tenant = createTestTenant();
            tenant.setMaxLoans(10);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(loanRepository.countByTenantId(TENANT_ID)).thenReturn(10L);

            assertThatThrownBy(() -> loanService.createLoan(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Maximum loan limit");
        }
    }

    @Nested
    @DisplayName("Get Loan Tests")
    class GetLoanTests {

        @Test
        @DisplayName("Should get loan by ID")
        void shouldGetLoanById() {
            Loan loan = createTestLoan();
            LoanResponse expectedResponse = createLoanResponse();

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));
            when(scheduleRepository.findByLoanIdOrderByInstallmentNumber(LOAN_ID)).thenReturn(Collections.emptyList());
            when(loanMapper.toResponse(any(Loan.class))).thenReturn(expectedResponse);
            when(loanMapper.toScheduleResponses(anyList())).thenReturn(Collections.emptyList());

            LoanResponse response = loanService.getLoanById(LOAN_ID);

            assertThat(response).isNotNull();
            verify(loanRepository).findByIdAndTenantId(LOAN_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should throw exception when loan not found")
        void shouldThrowExceptionWhenLoanNotFound() {
            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getLoanById(LOAN_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Loan Approval Tests")
    class LoanApprovalTests {

        @Test
        @DisplayName("Should approve loan")
        void shouldApproveLoan() {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.PENDING_APPROVAL);
            LoanResponse expectedResponse = createLoanResponse();
            expectedResponse.setStatus("APPROVED");

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(Loan.class))).thenReturn(loan);
            when(loanMapper.toResponse(any(Loan.class))).thenReturn(expectedResponse);

            LoanResponse response = loanService.approveLoan(LOAN_ID, "Approved", USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Should throw exception when approving non-pending loan")
        void shouldThrowExceptionWhenApprovingActiveLoan() {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.ACTIVE);

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.approveLoan(LOAN_ID, "Approved", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot be approved");
        }
    }

    @Nested
    @DisplayName("Loan Disbursement Tests")
    class LoanDisbursementTests {

        @Test
        @DisplayName("Should disburse loan successfully")
        void shouldDisburseLoan() {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.APPROVED);
            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .disbursementDate(LocalDate.now())
                    .build();
            LoanResponse expectedResponse = createLoanResponse();
            expectedResponse.setStatus("ACTIVE");

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));
            when(interestEngine.generateSchedule(any())).thenReturn(Collections.emptyList());
            when(loanRepository.save(any(Loan.class))).thenReturn(loan);
            when(loanMapper.toResponse(any(Loan.class))).thenReturn(expectedResponse);
            when(loanMapper.toScheduleResponses(anyList())).thenReturn(Collections.emptyList());

            LoanResponse response = loanService.disburseLoan(LOAN_ID, request, USER_ID);

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Loan Closure Tests")
    class LoanClosureTests {

        @Test
        @DisplayName("Should close fully paid loan")
        void shouldCloseFullyPaidLoan() {
            Loan loan = createTestLoan();
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setOutstandingPrincipal(BigDecimal.ZERO);
            loan.setOutstandingInterest(BigDecimal.ZERO);
            loan.setOutstandingPenalty(BigDecimal.ZERO);

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(Loan.class))).thenReturn(loan);

            loanService.closeLoan(LOAN_ID, USER_ID);

            verify(loanRepository).save(any(Loan.class));
        }

        @Test
        @DisplayName("Should throw exception when closing loan with outstanding balance")
        void shouldThrowExceptionWhenClosingWithBalance() {
            Loan loan = createTestLoan();
            loan.setOutstandingPrincipal(new BigDecimal("50000"));

            when(loanRepository.findByIdAndTenantId(LOAN_ID, TENANT_ID)).thenReturn(Optional.of(loan));

            assertThatThrownBy(() -> loanService.closeLoan(LOAN_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("outstanding balance");
        }
    }

    private Tenant createTestTenant() {
        return Tenant.builder()
                .id(TENANT_ID)
                .businessName("Test Business")
                .businessCode("TEST123")
                .maxBorrowers(100)
                .maxLoans(500)
                .build();
    }

    private Borrower createTestBorrower() {
        return Borrower.builder()
                .id(BORROWER_ID)
                .tenantId(TENANT_ID)
                .fullName("John Doe")
                .phone("1234567890")
                .status(BorrowerStatus.ACTIVE)
                .build();
    }

    private Loan createTestLoan() {
        return Loan.builder()
                .id(LOAN_ID)
                .tenantId(TENANT_ID)
                .borrowerId(BORROWER_ID)
                .loanNumber("LN-TEST-000001")
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
                .build();
    }

    private LoanResponse createLoanResponse() {
        return LoanResponse.builder()
                .id(LOAN_ID)
                .tenantId(TENANT_ID)
                .borrowerId(BORROWER_ID)
                .loanNumber("LN-TEST-000001")
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12"))
                .interestType("REDUCING_BALANCE")
                .tenureMonths(12)
                .status("DRAFT")
                .build();
    }
}
