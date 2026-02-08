package com.loanplatform.tenant.service;

import com.loanplatform.auth.dto.UserResponse;
import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.auth.service.AuthService;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.tenant.dto.CreateTenantRequest;
import com.loanplatform.tenant.dto.TenantResponse;
import com.loanplatform.tenant.dto.UpdateTenantRequest;
import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.entity.TenantStatus;
import com.loanplatform.tenant.mapper.TenantMapper;
import com.loanplatform.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Unit Tests")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    private TenantService tenantService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository, tenantMapper, authService, userRepository);
    }

    @Nested
    @DisplayName("Create Tenant Tests")
    class CreateTenantTests {

        @Test
        @DisplayName("Should create tenant successfully")
        void shouldCreateTenantSuccessfully() {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .businessName("Test Business")
                    .businessCode("TEST123")
                    .contactEmail("test@business.com")
                    .contactPhone("1234567890")
                    .adminEmail("admin@business.com")
                    .adminPassword("Password123!")
                    .adminFirstName("Admin")
                    .adminLastName("User")
                    .build();

            Tenant tenant = createTestTenant();
            TenantResponse expectedResponse = createTenantResponse();

            when(tenantRepository.existsByBusinessCode(anyString())).thenReturn(false);
            when(tenantMapper.toEntity(any(CreateTenantRequest.class))).thenReturn(tenant);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
            when(tenantMapper.toResponse(any(Tenant.class))).thenReturn(expectedResponse);
            when(authService.createUser(any(), any())).thenReturn(mock(UserResponse.class));
            when(userRepository.countByTenantId(any())).thenReturn(1L);

            TenantResponse response = tenantService.createTenant(request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getBusinessName()).isEqualTo("Test Business");
            verify(tenantRepository).save(any(Tenant.class));
            verify(authService).createUser(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when business code exists")
        void shouldThrowExceptionWhenBusinessCodeExists() {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .businessName("Test Business")
                    .businessCode("EXISTING")
                    .contactEmail("test@business.com")
                    .adminEmail("admin@business.com")
                    .adminPassword("Password123!")
                    .adminFirstName("Admin")
                    .adminLastName("User")
                    .build();

            when(tenantRepository.existsByBusinessCode(anyString())).thenReturn(true);

            assertThatThrownBy(() -> tenantService.createTenant(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Business code already exists");

            verify(tenantRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    @DisplayName("Get Tenant Tests")
    class GetTenantTests {

        @Test
        @DisplayName("Should get tenant by ID")
        void shouldGetTenantById() {
            Tenant tenant = createTestTenant();
            TenantResponse expectedResponse = createTenantResponse();

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(tenantMapper.toResponse(any(Tenant.class))).thenReturn(expectedResponse);
            when(userRepository.countByTenantId(any())).thenReturn(5L);

            TenantResponse response = tenantService.getTenantById(TENANT_ID);

            assertThat(response).isNotNull();
            verify(tenantRepository).findById(TENANT_ID);
        }

        @Test
        @DisplayName("Should throw exception when tenant not found")
        void shouldThrowExceptionWhenTenantNotFound() {
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getTenantById(TENANT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get tenant by code")
        void shouldGetTenantByCode() {
            Tenant tenant = createTestTenant();
            TenantResponse expectedResponse = createTenantResponse();

            when(tenantRepository.findByBusinessCode("TEST123")).thenReturn(Optional.of(tenant));
            when(tenantMapper.toResponse(any(Tenant.class))).thenReturn(expectedResponse);
            when(userRepository.countByTenantId(any())).thenReturn(5L);

            TenantResponse response = tenantService.getTenantByCode("TEST123");

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Update Tenant Tests")
    class UpdateTenantTests {

        @Test
        @DisplayName("Should update tenant successfully")
        void shouldUpdateTenantSuccessfully() {
            Tenant tenant = createTestTenant();
            UpdateTenantRequest request = UpdateTenantRequest.builder()
                    .businessName("Updated Business")
                    .contactEmail("updated@business.com")
                    .build();
            TenantResponse expectedResponse = createTenantResponse();
            expectedResponse.setBusinessName("Updated Business");

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
            when(tenantMapper.toResponse(any(Tenant.class))).thenReturn(expectedResponse);
            when(userRepository.countByTenantId(any())).thenReturn(5L);

            TenantResponse response = tenantService.updateTenant(TENANT_ID, request);

            assertThat(response).isNotNull();
            verify(tenantMapper).updateEntity(any(Tenant.class), any(UpdateTenantRequest.class));
            verify(tenantRepository).save(any(Tenant.class));
        }
    }

    @Nested
    @DisplayName("Tenant Status Tests")
    class TenantStatusTests {

        @Test
        @DisplayName("Should activate tenant")
        void shouldActivateTenant() {
            Tenant tenant = createTestTenant();
            tenant.setStatus(TenantStatus.SUSPENDED);

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

            tenantService.activateTenant(TENANT_ID);

            verify(tenantRepository).save(any(Tenant.class));
        }

        @Test
        @DisplayName("Should suspend tenant")
        void shouldSuspendTenant() {
            Tenant tenant = createTestTenant();

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

            tenantService.suspendTenant(TENANT_ID);

            verify(tenantRepository).save(any(Tenant.class));
        }

        @Test
        @DisplayName("Should deactivate tenant")
        void shouldDeactivateTenant() {
            Tenant tenant = createTestTenant();

            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

            tenantService.deactivateTenant(TENANT_ID);

            verify(tenantRepository).save(any(Tenant.class));
        }
    }

    private Tenant createTestTenant() {
        return Tenant.builder()
                .id(TENANT_ID)
                .businessName("Test Business")
                .businessCode("TEST123")
                .contactEmail("test@business.com")
                .contactPhone("1234567890")
                .status(TenantStatus.ACTIVE)
                .subscriptionStartDate(LocalDate.now())
                .maxBorrowers(100)
                .maxLoans(500)
                .build();
    }

    private TenantResponse createTenantResponse() {
        return TenantResponse.builder()
                .id(TENANT_ID)
                .businessName("Test Business")
                .businessCode("TEST123")
                .contactEmail("test@business.com")
                .status("ACTIVE")
                .build();
    }
}
