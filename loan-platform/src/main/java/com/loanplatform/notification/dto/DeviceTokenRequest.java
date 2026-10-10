package com.loanplatform.notification.dto;

import com.loanplatform.notification.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    private String token;

    @NotNull(message = "Platform is required")
    private DevicePlatform platform;
}
