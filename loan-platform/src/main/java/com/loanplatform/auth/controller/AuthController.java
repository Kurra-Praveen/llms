package com.loanplatform.auth.controller;

import com.loanplatform.auth.dto.*;
import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.auth.service.AuthService;
import com.loanplatform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke all refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = authService.getUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Profile update request for user: {}", principal.getId());
        UserResponse user = authService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile updated successfully"));
    }

    @PutMapping("/password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Password change request for user: {}", principal.getId());
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('LENDER_ADMIN')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse user = authService.createUser(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user, "User created successfully"));
    }
}
