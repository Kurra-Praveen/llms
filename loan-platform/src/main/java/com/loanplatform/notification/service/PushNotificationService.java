package com.loanplatform.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.auth.oauth2.GoogleCredentials;
import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.repository.UserRepository;
import com.loanplatform.notification.entity.DeviceToken;
import com.loanplatform.notification.entity.Notification;
import com.loanplatform.notification.repository.DeviceTokenRepository;
import com.loanplatform.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Delivers PUSH notifications via Firebase Cloud Messaging.
 *
 * Disabled by default; enable with FIREBASE_ENABLED=true and either
 * FIREBASE_CREDENTIALS_PATH (service account JSON file) or the standard
 * GOOGLE_APPLICATION_CREDENTIALS env var. When disabled, push notifications
 * are marked FAILED with a clear reason instead of crashing the flow.
 */
@Service
@Slf4j
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private volatile FirebaseMessaging firebaseMessaging;

    public PushNotificationService(
            DeviceTokenRepository deviceTokenRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            @Value("${app.firebase.enabled:false}") boolean enabled,
            @Value("${app.firebase.credentials-path:}") String credentialsPath) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        if (enabled) {
            initializeFirebase(credentialsPath);
        } else {
            log.info("Firebase push notifications disabled (app.firebase.enabled=false)");
        }
    }

    private void initializeFirebase(String credentialsPath) {
        try {
            GoogleCredentials credentials = StringUtils.hasText(credentialsPath)
                    ? GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                    : GoogleCredentials.getApplicationDefault();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            this.firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("Firebase Cloud Messaging initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Firebase — push notifications will be marked failed: {}",
                    e.getMessage());
            this.firebaseMessaging = null;
        }
    }

    public void send(Notification notification) {
        if (firebaseMessaging == null) {
            log.warn("Push provider not configured — failing notification {}", notification.getId());
            notification.markFailed("Push provider not configured");
            notificationRepository.save(notification);
            return;
        }

        // recipientId for borrower notifications is the borrower ID — resolve the linked user account
        UUID recipientId = notification.getRecipientId();
        Optional<User> userOpt = recipientId != null
                ? userRepository.findByBorrowerIdAndDeletedFalse(recipientId)
                : Optional.empty();

        if (userOpt.isEmpty()) {
            notification.markFailed("No user account linked to borrower");
            notificationRepository.save(notification);
            return;
        }

        List<DeviceToken> devices = deviceTokenRepository.findByUserIdAndActiveTrue(userOpt.get().getId());
        if (devices.isEmpty()) {
            notification.markFailed("No registered devices");
            notificationRepository.save(notification);
            return;
        }

        String title = StringUtils.hasText(notification.getSubject())
                ? notification.getSubject()
                : "Loan Platform";

        int successCount = 0;
        String lastError = null;

        for (DeviceToken device : devices) {
            try {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(device.getToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(notification.getContent())
                                .build())
                        .putData("notificationId", notification.getId().toString());

                if (notification.getLoanId() != null) {
                    messageBuilder.putData("loanId", notification.getLoanId().toString());
                }

                String messageId = firebaseMessaging.send(messageBuilder.build());
                successCount++;
                device.setLastUsedAt(Instant.now());
                deviceTokenRepository.save(device);
                log.debug("Push delivered to device {}: {}", device.getId(), messageId);
            } catch (FirebaseMessagingException e) {
                lastError = e.getMessage();
                log.warn("Push delivery failed for device {}: {}", device.getId(), e.getMessage());
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    device.setActive(false);
                    device.setUpdatedAt(Instant.now());
                    deviceTokenRepository.save(device);
                }
            }
        }

        if (successCount > 0) {
            notification.markSent("FCM-devices-" + successCount);
        } else {
            notification.markFailed(lastError != null ? lastError : "All device deliveries failed");
        }
        notificationRepository.save(notification);
    }
}
