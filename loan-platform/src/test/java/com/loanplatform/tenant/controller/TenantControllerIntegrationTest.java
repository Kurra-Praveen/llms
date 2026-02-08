package com.loanplatform.tenant.controller;

import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.entity.UserStatus;
import com.loanplatform.config.BaseIntegrationTest;
import com.loanplatform.tenant.dto.CreateTenantRequest;
import com.loanplatform.tenant.dto.UpdateTenantRequest;
import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.entity.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TenantController Integration Tests")
class TenantControllerIntegrationTest extends BaseIntegrationTest {

    private String superAdminToken;

    @BeforeEach
    void setUpSuperAdmin() {
        User superAdmin = User.builder()
                .email("superadmin@platform.com")
                .passwordHash(passwordEncoder.encode("superadmin123"))
                .firstName("Super")
                .lastName("Admin")
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        superAdmin = userRepository.save(superAdmin);
        superAdminToken = "Bearer " + jwtService.generateAccessToken(superAdmin);
    }

    @Nested
    @DisplayName("Create Tenant Tests")
    class CreateTenantTests {

        @Test
        @DisplayName("Should create tenant successfully")
        void shouldCreateTenantSuccessfully() throws Exception {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .businessName("New Business")
                    .businessCode("NEW-BIZ-" + System.nanoTime())
                    .contactEmail("contact@newbiz.com")
                    .contactPhone("1234567890")
                    .adminEmail("admin@newbiz.com")
                    .adminPassword("Password123!")
                    .adminFirstName("New")
                    .adminLastName("Admin")
                    .maxBorrowers(100)
                    .maxLoans(500)
                    .build();

            mockMvc.perform(post("/v1/tenants")
                            .header("Authorization", superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.businessName").value("New Business"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 400 for duplicate business code")
        void shouldReturn400ForDuplicateBusinessCode() throws Exception {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .businessName("Another Business")
                    .businessCode(testTenant.getBusinessCode())
                    .contactEmail("another@business.com")
                    .adminEmail("admin@another.com")
                    .adminPassword("Password123!")
                    .adminFirstName("Another")
                    .adminLastName("Admin")
                    .build();

            mockMvc.perform(post("/v1/tenants")
                            .header("Authorization", superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 for non-super-admin")
        void shouldReturn403ForNonSuperAdmin() throws Exception {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .businessName("Unauthorized Business")
                    .businessCode("UNAUTH-BIZ")
                    .contactEmail("unauth@business.com")
                    .adminEmail("admin@unauth.com")
                    .adminPassword("Password123!")
                    .adminFirstName("Unauth")
                    .adminLastName("Admin")
                    .build();

            mockMvc.perform(post("/v1/tenants")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Tenant Tests")
    class GetTenantTests {

        @Test
        @DisplayName("Should get tenant by ID")
        void shouldGetTenantById() throws Exception {
            mockMvc.perform(get("/v1/tenants/{id}", testTenant.getId())
                            .header("Authorization", superAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.businessName").value(testTenant.getBusinessName()))
                    .andExpect(jsonPath("$.data.businessCode").value(testTenant.getBusinessCode()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent tenant")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/v1/tenants/{id}", UUID.randomUUID())
                            .header("Authorization", superAdminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should list all tenants")
        void shouldListAllTenants() throws Exception {
            mockMvc.perform(get("/v1/tenants")
                            .header("Authorization", superAdminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }

        @Test
        @DisplayName("Should get tenant by code")
        void shouldGetTenantByCode() throws Exception {
            mockMvc.perform(get("/v1/tenants/code/{code}", testTenant.getBusinessCode())
                            .header("Authorization", superAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.businessName").value(testTenant.getBusinessName()));
        }
    }

    @Nested
    @DisplayName("Update Tenant Tests")
    class UpdateTenantTests {

        @Test
        @DisplayName("Should update tenant successfully")
        void shouldUpdateTenantSuccessfully() throws Exception {
            UpdateTenantRequest request = UpdateTenantRequest.builder()
                    .businessName("Updated Business Name")
                    .contactEmail("updated@business.com")
                    .build();

            mockMvc.perform(put("/v1/tenants/{id}", testTenant.getId())
                            .header("Authorization", superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.businessName").value("Updated Business Name"))
                    .andExpect(jsonPath("$.data.contactEmail").value("updated@business.com"));
        }
    }

    @Nested
    @DisplayName("Tenant Status Tests")
    class TenantStatusTests {

        @Test
        @DisplayName("Should suspend tenant")
        void shouldSuspendTenant() throws Exception {
            mockMvc.perform(post("/v1/tenants/{id}/suspend", testTenant.getId())
                            .header("Authorization", superAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            Tenant updated = tenantRepository.findById(testTenant.getId()).orElseThrow();
            assert updated.getStatus() == TenantStatus.SUSPENDED;
        }

        @Test
        @DisplayName("Should activate tenant")
        void shouldActivateTenant() throws Exception {
            testTenant.setStatus(TenantStatus.SUSPENDED);
            tenantRepository.save(testTenant);

            mockMvc.perform(post("/v1/tenants/{id}/activate", testTenant.getId())
                            .header("Authorization", superAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            Tenant updated = tenantRepository.findById(testTenant.getId()).orElseThrow();
            assert updated.getStatus() == TenantStatus.ACTIVE;
        }

        @Test
        @DisplayName("Should deactivate tenant")
        void shouldDeactivateTenant() throws Exception {
            mockMvc.perform(post("/v1/tenants/{id}/deactivate", testTenant.getId())
                            .header("Authorization", superAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            Tenant updated = tenantRepository.findById(testTenant.getId()).orElseThrow();
            assert updated.getStatus() == TenantStatus.INACTIVE;
        }
    }

    @Nested
    @DisplayName("Search Tenant Tests")
    class SearchTenantTests {

        @Test
        @DisplayName("Should search tenants by name")
        void shouldSearchTenantsByName() throws Exception {
            mockMvc.perform(get("/v1/tenants/search")
                            .header("Authorization", superAdminToken)
                            .param("q", "Test")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }
}
