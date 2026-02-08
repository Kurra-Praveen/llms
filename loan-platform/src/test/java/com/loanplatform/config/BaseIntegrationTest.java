package com.loanplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.auth.security.JwtService;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.tenant.entity.Tenant;
import com.loanplatform.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected Tenant testTenant;
    protected User testUser;
    protected String authToken;

    @BeforeEach
    void setUpBase() {
        testTenant = createTestTenant();
        TenantContext.setCurrentTenant(testTenant.getId());
        testUser = createTestUser();
        authToken = jwtService.generateAccessToken(testUser);
    }

    @AfterEach
    void tearDownBase() {
        TenantContext.clear();
    }

    protected Tenant createTestTenant() {
        Tenant tenant = Tenant.builder()
                .businessName("Test Business")
                .businessCode("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .contactEmail("test@example.com")
                .contactPhone("1234567890")
                .build();
        return tenantRepository.save(tenant);
    }

    protected User createTestUser() {
        User user = User.builder()
                .tenantId(testTenant.getId())
                .email("testuser@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .role(Role.LENDER_ADMIN)
                .build();
        return userRepository.save(user);
    }

    protected String getAuthHeader() {
        return "Bearer " + authToken;
    }
}
