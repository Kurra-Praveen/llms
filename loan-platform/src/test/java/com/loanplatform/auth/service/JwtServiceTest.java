package com.loanplatform.auth.service;

import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits-long";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        User user = createTestUser();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        User user = createTestUser();

        String token = jwtService.generateRefreshToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        UUID extractedId = jwtService.extractUserId(token);

        assertThat(extractedId).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should extract tenant ID from token")
    void shouldExtractTenantIdFromToken() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        UUID extractedTenantId = jwtService.extractTenantId(token);

        assertThat(extractedTenantId).isEqualTo(user.getTenantId());
    }

    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRoleFromToken() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        String role = jwtService.extractRole(token);

        assertThat(role).isEqualTo(user.getRole().name());
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtService.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for tampered token")
    void shouldReturnFalseForTamperedToken() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        boolean isValid = jwtService.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.LENDER_ADMIN)
                .build();
    }
}
