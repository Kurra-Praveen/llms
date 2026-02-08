package com.loanplatform.auth.service;

import com.loanplatform.auth.dto.AuthResponse;
import com.loanplatform.auth.dto.CreateUserRequest;
import com.loanplatform.auth.dto.LoginRequest;
import com.loanplatform.auth.entity.Role;
import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.entity.UserStatus;
import com.loanplatform.auth.repository.RefreshTokenRepository;
import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.auth.security.JwtService;
import com.loanplatform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtService,
                passwordEncoder,
                authenticationManager
        );
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            User user = createActiveUser();

            when(userRepository.findByEmailAndDeletedFalse(anyString())).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(user, null));
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid email")
        void shouldThrowExceptionForInvalidEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@example.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmailAndDeletedFalse(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception for locked user")
        void shouldThrowExceptionForLockedUser() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            User user = createActiveUser();
            user.setStatus(UserStatus.LOCKED);

            when(userRepository.findByEmailAndDeletedFalse(anyString())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("locked");
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            User user = createActiveUser();
            user.setStatus(UserStatus.INACTIVE);

            when(userRepository.findByEmailAndDeletedFalse(anyString())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Should increment failed attempts on wrong password")
        void shouldIncrementFailedAttemptsOnWrongPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrongpassword")
                    .build();

            User user = createActiveUser();

            when(userRepository.findByEmailAndDeletedFalse(anyString())).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .tenantId(TENANT_ID)
                    .email("newuser@example.com")
                    .password("Password123!")
                    .firstName("John")
                    .lastName("Doe")
                    .role(Role.LENDER_STAFF)
                    .build();

            when(userRepository.existsByEmailAndTenantIdAndDeletedFalse(anyString(), any(UUID.class)))
                    .thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(USER_ID);
                return user;
            });

            var response = authService.createUser(request, UUID.randomUUID());

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("newuser@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .tenantId(TENANT_ID)
                    .email("existing@example.com")
                    .password("Password123!")
                    .firstName("John")
                    .lastName("Doe")
                    .role(Role.LENDER_STAFF)
                    .build();

            when(userRepository.existsByEmailAndTenantIdAndDeletedFalse(anyString(), any(UUID.class)))
                    .thenReturn(true);

            assertThatThrownBy(() -> authService.createUser(request, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    private User createActiveUser() {
        return User.builder()
                .id(USER_ID)
                .tenantId(TENANT_ID)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .role(Role.LENDER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
