package com.loanplatform.notification.service;

import com.loanplatform.auth.security.SecurityUtils;
import com.loanplatform.auth.security.UserPrincipal;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.notification.dto.DeviceTokenRequest;
import com.loanplatform.notification.entity.DeviceToken;
import com.loanplatform.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void registerDevice(DeviceTokenRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        // Tenant required for scoping; SUPER_ADMIN (no tenant) cannot register devices
        var tenantId = TenantContext.requireTenant();

        DeviceToken device = deviceTokenRepository.findByToken(request.getToken())
                .map(existing -> {
                    // Re-assign token if the device changed hands / reinstalled
                    existing.setUserId(user.getId());
                    existing.setTenantId(tenantId);
                    existing.setPlatform(request.getPlatform());
                    existing.setActive(true);
                    existing.setUpdatedAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .tenantId(tenantId)
                        .userId(user.getId())
                        .token(request.getToken())
                        .platform(request.getPlatform())
                        .active(true)
                        .build());

        deviceTokenRepository.save(device);
        log.info("Device registered for user {} on platform {}", user.getId(), request.getPlatform());
    }

    @Transactional
    public void unregisterDevice(String token) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        deviceTokenRepository.findByToken(token)
                .filter(d -> d.getUserId().equals(user.getId()))
                .ifPresent(d -> {
                    d.setActive(false);
                    d.setUpdatedAt(Instant.now());
                    deviceTokenRepository.save(d);
                    log.info("Device unregistered for user {}", user.getId());
                });
    }
}
