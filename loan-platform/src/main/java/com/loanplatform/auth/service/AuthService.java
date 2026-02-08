package com.loanplatform.auth.service;

import com.loanplatform.auth.dto.*;
import com.loanplatform.auth.entity.RefreshToken;
import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.entity.UserStatus;
import com.loanplatform.auth.repository.RefreshTokenRepository;
import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.auth.security.JwtService;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isLocked()) {
            throw new BusinessException("Account is locked. Please try again later.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active",
                    HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        user.recordLogin();
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);

        log.info("User logged in successfully: {}", user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token",
                        HttpStatus.UNAUTHORIZED, "INVALID_TOKEN"));

        if (storedToken.isExpired()) {
            storedToken.revoke();
            refreshTokenRepository.save(storedToken);
            throw new BusinessException("Refresh token expired", HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .filter(u -> !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("User not found or inactive",
                        HttpStatus.UNAUTHORIZED, "USER_INACTIVE"));

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, newRefreshToken);

        log.info("Token refreshed for user: {}", user.getId());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("User logged out: {}", userId);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, UUID createdByUserId) {
        if (userRepository.existsByEmailAndTenantIdAndDeletedFalse(request.getEmail(), request.getTenantId())) {
            throw new BusinessException("User with this email already exists", "EMAIL_EXISTS");
        }

        if (request.getRole() == Role.SUPER_ADMIN && request.getTenantId() != null) {
            throw new BusinessException("Super Admin cannot be associated with a tenant",
                    "INVALID_ROLE_TENANT");
        }

        if (request.getRole() != Role.SUPER_ADMIN && request.getTenantId() == null) {
            throw new BusinessException("Tenant ID is required for non-admin users",
                    "TENANT_REQUIRED");
        }

        User user = User.builder()
                .tenantId(request.getTenantId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .createdBy(createdByUserId)
                .build();

        user = userRepository.save(user);
        log.info("User created: {} by {}", user.getId(), createdByUserId);

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToUserResponse(user);
    }

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();
        refreshTokenRepository.save(token);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .tenantId(user.getTenantId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        String tenantName = null;
        if (user.getTenantId() != null && user.getTenant() != null) {
            tenantName = user.getTenant().getBusinessName();
        }

        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .tenantName(tenantName)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
