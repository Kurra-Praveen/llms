package com.loanplatform.notification.controller;

import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.notification.dto.DeviceTokenRequest;
import com.loanplatform.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Devices", description = "Push notification device registration")
@PreAuthorize("isAuthenticated()")
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @Operation(summary = "Register a device token for push notifications")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.registerDevice(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Device registered"));
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Unregister a device token (e.g., on logout)")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(@PathVariable String token) {
        deviceTokenService.unregisterDevice(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Device unregistered"));
    }
}
