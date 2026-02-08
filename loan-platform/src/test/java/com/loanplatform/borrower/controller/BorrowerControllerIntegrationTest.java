package com.loanplatform.borrower.controller;

import com.loanplatform.borrower.dto.CreateBorrowerRequest;
import com.loanplatform.borrower.dto.UpdateBorrowerRequest;
import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.borrower.entity.BorrowerStatus;
import com.loanplatform.borrower.repository.BorrowerRepository;
import com.loanplatform.config.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BorrowerController Integration Tests")
class BorrowerControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Nested
    @DisplayName("Create Borrower Tests")
    class CreateBorrowerTests {

        @Test
        @DisplayName("Should create borrower successfully")
        void shouldCreateBorrowerSuccessfully() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("John Doe")
                    .phone("9876543210")
                    .email("john.doe@example.com")
                    .monthlyIncome(new BigDecimal("50000"))
                    .build();

            mockMvc.perform(post("/v1/borrowers")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.data.phone").value("9876543210"))
                    .andExpect(jsonPath("$.data.borrowerCode").isNotEmpty())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .email("john.doe@example.com")
                    .build();

            mockMvc.perform(post("/v1/borrowers")
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 without auth token")
        void shouldReturn403WithoutAuthToken() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("John Doe")
                    .phone("9876543210")
                    .build();

            mockMvc.perform(post("/v1/borrowers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Borrower Tests")
    class GetBorrowerTests {

        @Test
        @DisplayName("Should get borrower by ID")
        void shouldGetBorrowerById() throws Exception {
            Borrower borrower = createTestBorrower("Jane Doe", "1122334455");

            mockMvc.perform(get("/v1/borrowers/{id}", borrower.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fullName").value("Jane Doe"))
                    .andExpect(jsonPath("$.data.phone").value("1122334455"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent borrower")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/v1/borrowers/{id}", java.util.UUID.randomUUID())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should list all borrowers")
        void shouldListAllBorrowers() throws Exception {
            createTestBorrower("Borrower 1", "1111111111");
            createTestBorrower("Borrower 2", "2222222222");

            mockMvc.perform(get("/v1/borrowers")
                            .header("Authorization", getAuthHeader())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }
    }

    @Nested
    @DisplayName("Update Borrower Tests")
    class UpdateBorrowerTests {

        @Test
        @DisplayName("Should update borrower successfully")
        void shouldUpdateBorrowerSuccessfully() throws Exception {
            Borrower borrower = createTestBorrower("Original Name", "3333333333");

            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .fullName("Updated Name")
                    .email("updated@example.com")
                    .build();

            mockMvc.perform(put("/v1/borrowers/{id}", borrower.getId())
                            .header("Authorization", getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                    .andExpect(jsonPath("$.data.email").value("updated@example.com"));
        }
    }

    @Nested
    @DisplayName("Borrower Status Tests")
    class BorrowerStatusTests {

        @Test
        @DisplayName("Should block borrower")
        void shouldBlockBorrower() throws Exception {
            Borrower borrower = createTestBorrower("Block Test", "4444444444");

            mockMvc.perform(post("/v1/borrowers/{id}/block", borrower.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            Borrower updated = borrowerRepository.findById(borrower.getId()).orElseThrow();
            assert updated.getStatus() == BorrowerStatus.BLOCKED;
        }

        @Test
        @DisplayName("Should activate borrower")
        void shouldActivateBorrower() throws Exception {
            Borrower borrower = createTestBorrower("Activate Test", "5555555555");
            borrower.setStatus(BorrowerStatus.BLOCKED);
            borrowerRepository.save(borrower);

            mockMvc.perform(post("/v1/borrowers/{id}/activate", borrower.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            Borrower updated = borrowerRepository.findById(borrower.getId()).orElseThrow();
            assert updated.getStatus() == BorrowerStatus.ACTIVE;
        }

        @Test
        @DisplayName("Should soft delete borrower")
        void shouldSoftDeleteBorrower() throws Exception {
            Borrower borrower = createTestBorrower("Delete Test", "6666666666");

            mockMvc.perform(delete("/v1/borrowers/{id}", borrower.getId())
                            .header("Authorization", getAuthHeader()))
                    .andExpect(status().isOk());

            Borrower deleted = borrowerRepository.findById(borrower.getId()).orElseThrow();
            assert deleted.isDeleted();
        }
    }

    private Borrower createTestBorrower(String name, String phone) {
        Borrower borrower = Borrower.builder()
                .tenantId(testTenant.getId())
                .fullName(name)
                .phone(phone)
                .borrowerCode("BRW-TEST-" + System.nanoTime())
                .status(BorrowerStatus.ACTIVE)
                .riskScore(50)
                .build();
        return borrowerRepository.save(borrower);
    }
}
