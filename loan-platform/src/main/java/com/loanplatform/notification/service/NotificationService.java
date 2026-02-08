package com.loanplatform.notification.service;

import com.loanplatform.borrower.entity.Borrower;
import com.loanplatform.common.config.TenantContext;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.notification.entity.Notification;
import com.loanplatform.notification.entity.NotificationChannel;
import com.loanplatform.notification.entity.NotificationStatus;
import com.loanplatform.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @Async
    @Transactional
    public void sendLoanDisbursementNotification(Loan loan, Borrower borrower) {
        UUID tenantId = loan.getTenantId();

        Map<String, String> variables = new HashMap<>();
        variables.put("borrower_name", borrower.getFullName());
        variables.put("loan_number", loan.getLoanNumber());
        variables.put("currency", "INR");
        variables.put("principal_amount", formatAmount(loan.getPrincipalAmount()));
        variables.put("emi_amount", formatAmount(loan.getEmiAmount()));
        variables.put("first_due_date", loan.getFirstPaymentDate().format(DATE_FORMAT));

        String smsContent = String.format(
                "Dear %s, your loan %s of INR %s has been disbursed. EMI: INR %s. First due: %s.",
                borrower.getFullName(),
                loan.getLoanNumber(),
                formatAmount(loan.getPrincipalAmount()),
                formatAmount(loan.getEmiAmount()),
                loan.getFirstPaymentDate().format(DATE_FORMAT)
        );

        createAndSendNotification(tenantId, borrower, loan, NotificationChannel.SMS, null, smsContent);

        log.info("Loan disbursement notification sent for loan: {}", loan.getId());
    }

    @Async
    @Transactional
    public void sendPaymentReceivedNotification(Loan loan, Borrower borrower, BigDecimal amount, String receiptNumber) {
        UUID tenantId = loan.getTenantId();

        String smsContent = String.format(
                "Dear %s, payment of INR %s received for loan %s. Receipt: %s. Outstanding: INR %s.",
                borrower.getFullName(),
                formatAmount(amount),
                loan.getLoanNumber(),
                receiptNumber,
                formatAmount(loan.getTotalOutstanding())
        );

        createAndSendNotification(tenantId, borrower, loan, NotificationChannel.SMS, null, smsContent);

        log.info("Payment received notification sent for loan: {}", loan.getId());
    }

    @Async
    @Transactional
    public void sendPaymentDueReminder(Loan loan, Borrower borrower, BigDecimal dueAmount, java.time.LocalDate dueDate) {
        UUID tenantId = loan.getTenantId();

        String smsContent = String.format(
                "Dear %s, your EMI of INR %s for loan %s is due on %s. Please pay on time to avoid penalty.",
                borrower.getFullName(),
                formatAmount(dueAmount),
                loan.getLoanNumber(),
                dueDate.format(DATE_FORMAT)
        );

        createAndSendNotification(tenantId, borrower, loan, NotificationChannel.SMS, null, smsContent);

        log.info("Payment due reminder sent for loan: {}", loan.getId());
    }

    @Async
    @Transactional
    public void sendOverdueAlert(Loan loan, Borrower borrower, BigDecimal overdueAmount, int dpd) {
        UUID tenantId = loan.getTenantId();

        String smsContent = String.format(
                "Dear %s, your EMI of INR %s for loan %s is overdue by %d days. Please pay immediately to avoid additional penalties.",
                borrower.getFullName(),
                formatAmount(overdueAmount),
                loan.getLoanNumber(),
                dpd
        );

        createAndSendNotification(tenantId, borrower, loan, NotificationChannel.SMS, null, smsContent);

        log.info("Overdue alert sent for loan: {}", loan.getId());
    }

    @Transactional
    public void processQueuedNotifications() {
        List<Notification> pending = notificationRepository.findPendingNotifications(Instant.now());
        log.info("Processing {} pending notifications", pending.size());

        for (Notification notification : pending) {
            try {
                sendNotification(notification);
            } catch (Exception e) {
                log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
                notification.markFailed(e.getMessage());
                notificationRepository.save(notification);
            }
        }
    }

    private void createAndSendNotification(UUID tenantId, Borrower borrower, Loan loan,
                                            NotificationChannel channel, String subject, String content) {
        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .recipientType("BORROWER")
                .recipientId(borrower.getId())
                .recipientContact(channel == NotificationChannel.EMAIL ? borrower.getEmail() : borrower.getPhone())
                .channel(channel)
                .subject(subject)
                .content(content)
                .referenceType("LOAN")
                .referenceId(loan.getId())
                .loanId(loan.getId())
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);
        sendNotification(notification);
    }

    private void sendNotification(Notification notification) {
        log.info("Sending {} notification to: {}", notification.getChannel(), notification.getRecipientContact());

        String externalId = "EXT-" + System.currentTimeMillis();
        notification.markSent(externalId);
        notificationRepository.save(notification);

        log.debug("Notification sent: {} -> {}", notification.getId(), externalId);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
