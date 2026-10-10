package com.loanplatform.notification.controller;

import com.loanplatform.common.dto.ApiResponse;
import com.loanplatform.common.dto.PagedResponse;
import com.loanplatform.notification.dto.NotificationResponse;
import com.loanplatform.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "In-app notification center")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications (paginated, newest first)")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getMyNotifications(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get my unread notification count (badge)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unreadCount", notificationService.getMyUnreadCount())));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(notificationId)));
    }
}
