package com.loanplatform.borrower.service;

import com.loanplatform.borrower.dto.BorrowerResponse;
import com.loanplatform.borrower.dto.CreateBorrowerRequest;
import com.loanplatform.borrower.dto.UpdateBorrowerRequest;
import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.entity.RiskBand;
import com.loanplatform.borrower.mapper.BorrowerMapper;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.repository.TenantRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowerService Unit Tests")
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BorrowerMapper borrowerMapper;

    @Mock
    private TenantRepository tenantRepository;

    private BorrowerService borrowerService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID BORROWER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        borrowerService = new BorrowerService(borrowerRepository, borrowerMapper, tenantRepository);
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::requireTenant).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Nested
    @DisplayName("Create Borrower Tests")
    class CreateBorrowerTests {

        @Test
        @DisplayName("Should create borrower successfully")
        void shouldCreateBorrowerSuccessfully() {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("John Doe")
                    .phone("1234567890")
                    .email("john@example.com")
                    .monthlyIncome(new BigDecimal("50000"))
                    .build();

            Tenant tenant = createTestTenant();
            Borrower borrower = createTestBorrower();
            BorrowerResponse expectedResponse = createBorrowerResponse();

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(borrowerRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
            when(borrowerRepository.existsByPhoneAndTenantIdAndDeletedFalse(anyString(), any())).thenReturn(false);
            when(borrowerMapper.toEntity(any(CreateBorrowerRequest.class))).thenReturn(borrower);
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(borrower);
            when(borrowerMapper.toResponse(any(Borrower.class))).thenReturn(expectedResponse);

            BorrowerResponse response = borrowerService.createBorrower(request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("John Doe");
            verify(borrowerRepository).save(any(Borrower.class));
        }

        @Test
        @DisplayName("Should throw exception when phone already exists")
        void shouldThrowExceptionWhenPhoneExists() {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("John Doe")
                    .phone("1234567890")
                    .build();

            Tenant tenant = createTestTenant();

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(borrowerRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
            when(borrowerRepository.existsByPhoneAndTenantIdAndDeletedFalse(anyString(), any())).thenReturn(true);

            assertThatThrownBy(() -> borrowerService.createBorrower(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("phone already exists");
        }

        @Test
        @DisplayName("Should throw exception when borrower limit exceeded")
        void shouldThrowExceptionWhenLimitExceeded() {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("John Doe")
                    .phone("1234567890")
                    .build();

            Tenant tenant = createTestTenant();
            tenant.setMaxBorrowers(10);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(borrowerRepository.countByTenantId(TENANT_ID)).thenReturn(10L);

            assertThatThrownBy(() -> borrowerService.createBorrower(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Maximum borrower limit");
        }
    }

    @Nested
    @DisplayName("Get Borrower Tests")
    class GetBorrowerTests {

        @Test
        @DisplayName("Should get borrower by ID")
        void shouldGetBorrowerById() {
            Borrower borrower = createTestBorrower();
            BorrowerResponse expectedResponse = createBorrowerResponse();

            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(borrowerMapper.toResponse(any(Borrower.class))).thenReturn(expectedResponse);

            BorrowerResponse response = borrowerService.getBorrowerById(BORROWER_ID);

            assertThat(response).isNotNull();
            verify(borrowerRepository).findByIdAndTenantId(BORROWER_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should throw exception when borrower not found")
        void shouldThrowExceptionWhenBorrowerNotFound() {
            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowerService.getBorrowerById(BORROWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Borrower Tests")
    class UpdateBorrowerTests {

        @Test
        @DisplayName("Should update borrower successfully")
        void shouldUpdateBorrowerSuccessfully() {
            Borrower borrower = createTestBorrower();
            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .fullName("Jane Doe")
                    .email("jane@example.com")
                    .build();
            BorrowerResponse expectedResponse = createBorrowerResponse();
            expectedResponse.setFullName("Jane Doe");

            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(borrower);
            when(borrowerMapper.toResponse(any(Borrower.class))).thenReturn(expectedResponse);

            BorrowerResponse response = borrowerService.updateBorrower(BORROWER_ID, request, USER_ID);

            assertThat(response).isNotNull();
            verify(borrowerMapper).updateEntity(any(Borrower.class), any(UpdateBorrowerRequest.class));
            verify(borrowerRepository).save(any(Borrower.class));
        }

        @Test
        @DisplayName("Should throw exception when updating phone to existing one")
        void shouldThrowExceptionWhenPhoneExists() {
            Borrower borrower = createTestBorrower();
            borrower.setPhone("0987654321");

            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .phone("1234567890")
                    .build();

            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(borrowerRepository.existsByPhoneAndTenantIdAndDeletedFalse("1234567890", TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> borrowerService.updateBorrower(BORROWER_ID, request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("phone already exists");
        }
    }

    @Nested
    @DisplayName("Borrower Status Tests")
    class BorrowerStatusTests {

        @Test
        @DisplayName("Should block borrower")
        void shouldBlockBorrower() {
            Borrower borrower = createTestBorrower();

            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(borrower);

            borrowerService.blockBorrower(BORROWER_ID, USER_ID);

            verify(borrowerRepository).save(any(Borrower.class));
        }

        @Test
        @DisplayName("Should activate borrower")
        void shouldActivateBorrower() {
            Borrower borrower = createTestBorrower();
            borrower.setStatus(BorrowerStatus.BLOCKED);

            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(borrower);

            borrowerService.activateBorrower(BORROWER_ID, USER_ID);

            verify(borrowerRepository).save(any(Borrower.class));
        }

        @Test
        @DisplayName("Should soft delete borrower")
        void shouldSoftDeleteBorrower() {
            Borrower borrower = createTestBorrower();

            when(borrowerRepository.findByIdAndTenantId(BORROWER_ID, TENANT_ID)).thenReturn(Optional.of(borrower));
            when(borrowerRepository.save(any(Borrower.class))).thenReturn(borrower);

            borrowerService.deleteBorrower(BORROWER_ID, USER_ID);

            verify(borrowerRepository).save(any(Borrower.class));
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
                .email("john@example.com")
                .borrowerCode("BRW-TEST-000001")
                .status(BorrowerStatus.ACTIVE)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .monthlyIncome(new BigDecimal("50000"))
                .riskScore(70)
                .riskBand(RiskBand.MEDIUM)
                .build();
    }

    private BorrowerResponse createBorrowerResponse() {
        return BorrowerResponse.builder()
                .id(BORROWER_ID)
                .tenantId(TENANT_ID)
                .fullName("John Doe")
                .phone("1234567890")
                .email("john@example.com")
                .borrowerCode("BRW-TEST-000001")
                .status("ACTIVE")
                .build();
    }
}
