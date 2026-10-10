package com.loanplatform.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String channel;
    private String subject;
    private String content;
    private String referenceType;
    private UUID referenceId;
    private UUID loanId;
    private String status;
    private Instant readAt;
    private Instant createdAt;
}
