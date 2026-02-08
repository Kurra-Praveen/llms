package com.loanplatform.ledger.service;

import com.loanplatform.common.config.TenantContext;
import com.loanplatform.common.exception.BusinessException;
import com.loanplatform.common.exception.ResourceNotFoundException;
import com.loanplatform.ledger.entity.*;
import com.loanplatform.ledger.repository.LedgerAccountRepository;
import com.loanplatform.ledger.repository.LedgerEntryRepository;
import com.loanplatform.ledger.repository.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    private final AtomicLong transactionSequence = new AtomicLong(System.currentTimeMillis() % 100000);

    public static final String CASH_BANK = "1000";
    public static final String LOAN_PRINCIPAL_OUTSTANDING = "1100";
    public static final String INTEREST_RECEIVABLE = "1200";
    public static final String PENALTY_RECEIVABLE = "1300";
    public static final String INTEREST_INCOME = "4000";
    public static final String PENALTY_INCOME = "4100";
    public static final String PROCESSING_FEE_INCOME = "4200";
    public static final String BAD_DEBT_EXPENSE = "5000";

    @Transactional
    public LedgerTransaction recordLoanDisbursement(UUID loanId, BigDecimal principal, BigDecimal processingFee, UUID postedBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Recording loan disbursement: loan={}, principal={}", loanId, principal);

        LedgerTransaction txn = createTransaction(
                tenantId,
                TransactionType.LOAN_DISBURSEMENT,
                "Loan disbursement",
                "LOAN",
                loanId,
                loanId,
                postedBy
        );

        addEntry(txn, LOAN_PRINCIPAL_OUTSTANDING, EntryType.DEBIT, principal, "Principal disbursed");
        addEntry(txn, CASH_BANK, EntryType.CREDIT, principal, "Cash disbursed");

        if (processingFee != null && processingFee.compareTo(BigDecimal.ZERO) > 0) {
            addEntry(txn, CASH_BANK, EntryType.DEBIT, processingFee, "Processing fee received");
            addEntry(txn, PROCESSING_FEE_INCOME, EntryType.CREDIT, processingFee, "Processing fee income");
        }

        validateAndSave(txn);
        return txn;
    }

    @Transactional
    public LedgerTransaction recordPaymentReceived(
            UUID loanId,
            UUID paymentId,
            BigDecimal principalPaid,
            BigDecimal interestPaid,
            BigDecimal penaltyPaid,
            UUID postedBy) {

        UUID tenantId = TenantContext.requireTenant();
        BigDecimal totalReceived = principalPaid.add(interestPaid).add(penaltyPaid);
        log.info("Recording payment received: loan={}, total={}", loanId, totalReceived);

        LedgerTransaction txn = createTransaction(
                tenantId,
                TransactionType.PAYMENT_RECEIVED,
                "Payment received",
                "PAYMENT",
                paymentId,
                loanId,
                postedBy
        );

        addEntry(txn, CASH_BANK, EntryType.DEBIT, totalReceived, "Cash received");

        if (principalPaid.compareTo(BigDecimal.ZERO) > 0) {
            addEntry(txn, LOAN_PRINCIPAL_OUTSTANDING, EntryType.CREDIT, principalPaid, "Principal repaid");
        }

        if (interestPaid.compareTo(BigDecimal.ZERO) > 0) {
            addEntry(txn, INTEREST_RECEIVABLE, EntryType.CREDIT, interestPaid, "Interest received");
        }

        if (penaltyPaid.compareTo(BigDecimal.ZERO) > 0) {
            addEntry(txn, PENALTY_RECEIVABLE, EntryType.CREDIT, penaltyPaid, "Penalty received");
        }

        validateAndSave(txn);
        return txn;
    }

    @Transactional
    public LedgerTransaction recordInterestAccrual(UUID loanId, BigDecimal interestAmount, UUID postedBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Recording interest accrual: loan={}, amount={}", loanId, interestAmount);

        LedgerTransaction txn = createTransaction(
                tenantId,
                TransactionType.INTEREST_ACCRUAL,
                "Interest accrual",
                "LOAN",
                loanId,
                loanId,
                postedBy
        );

        addEntry(txn, INTEREST_RECEIVABLE, EntryType.DEBIT, interestAmount, "Interest accrued");
        addEntry(txn, INTEREST_INCOME, EntryType.CREDIT, interestAmount, "Interest income recognized");

        validateAndSave(txn);
        return txn;
    }

    @Transactional
    public LedgerTransaction recordPenaltyApplied(UUID loanId, BigDecimal penaltyAmount, UUID postedBy) {
        UUID tenantId = TenantContext.requireTenant();
        log.info("Recording penalty applied: loan={}, amount={}", loanId, penaltyAmount);

        LedgerTransaction txn = createTransaction(
                tenantId,
                TransactionType.PENALTY_APPLIED,
                "Penalty applied",
                "LOAN",
                loanId,
                loanId,
                postedBy
        );

        addEntry(txn, PENALTY_RECEIVABLE, EntryType.DEBIT, penaltyAmount, "Penalty charged");
        addEntry(txn, PENALTY_INCOME, EntryType.CREDIT, penaltyAmount, "Penalty income recognized");

        validateAndSave(txn);
        return txn;
    }

    @Transactional
    public LedgerTransaction reverseTransaction(UUID originalTransactionId, String reason, UUID postedBy) {
        UUID tenantId = TenantContext.requireTenant();

        LedgerTransaction original = transactionRepository.findByIdAndTenantId(originalTransactionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", originalTransactionId));

        if (Boolean.TRUE.equals(original.getIsReversed())) {
            throw new BusinessException("Transaction is already reversed", "ALREADY_REVERSED");
        }

        log.info("Reversing transaction: {}", originalTransactionId);

        LedgerTransaction reversal = createTransaction(
                tenantId,
                original.getTransactionType(),
                "REVERSAL: " + reason,
                original.getReferenceType(),
                original.getReferenceId(),
                original.getLoanId(),
                postedBy
        );
        reversal.setReversalOf(originalTransactionId);

        List<LedgerEntry> originalEntries = entryRepository.findByTransactionId(originalTransactionId);
        for (LedgerEntry entry : originalEntries) {
            EntryType reversedType = entry.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT;
            LedgerEntry reversalEntry = LedgerEntry.builder()
                    .tenantId(tenantId)
                    .transactionId(reversal.getId())
                    .accountId(entry.getAccountId())
                    .entryType(reversedType)
                    .amount(entry.getAmount())
                    .narration("Reversal: " + entry.getNarration())
                    .build();
            reversal.addEntry(reversalEntry);
        }

        validateAndSave(reversal);

        original.setIsReversed(true);
        original.setReversedByTransactionId(reversal.getId());
        transactionRepository.save(original);

        return reversal;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(UUID accountId) {
        return entryRepository.getAccountBalance(accountId);
    }

    @Transactional(readOnly = true)
    public List<LedgerTransaction> getTransactionsForLoan(UUID loanId) {
        return transactionRepository.findByLoanId(loanId);
    }

    @Transactional(readOnly = true)
    public LedgerAccount getAccountByCode(String accountCode) {
        UUID tenantId = TenantContext.requireTenant();
        return accountRepository.findByTenantIdAndAccountCode(tenantId, accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountCode));
    }

    private LedgerTransaction createTransaction(
            UUID tenantId,
            TransactionType type,
            String description,
            String referenceType,
            UUID referenceId,
            UUID loanId,
            UUID postedBy) {

        return LedgerTransaction.builder()
                .tenantId(tenantId)
                .transactionNumber(generateTransactionNumber(tenantId))
                .transactionDate(LocalDate.now())
                .transactionType(type)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .loanId(loanId)
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .postedBy(postedBy)
                .build();
    }

    private void addEntry(LedgerTransaction txn, String accountCode, EntryType type, BigDecimal amount, String narration) {
        LedgerAccount account = accountRepository.findByTenantIdAndAccountCode(txn.getTenantId(), accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountCode));

        LedgerEntry entry = LedgerEntry.builder()
                .tenantId(txn.getTenantId())
                .transactionId(txn.getId())
                .accountId(account.getId())
                .entryType(type)
                .amount(amount)
                .narration(narration)
                .build();

        txn.addEntry(entry);
    }

    private void validateAndSave(LedgerTransaction txn) {
        txn.recalculateTotals();

        if (!txn.isBalanced()) {
            throw new BusinessException(
                    String.format("Transaction not balanced: Debits=%s, Credits=%s",
                            txn.getTotalDebit(), txn.getTotalCredit()),
                    "UNBALANCED_TRANSACTION"
            );
        }

        transactionRepository.save(txn);
        entryRepository.saveAll(txn.getEntries());

        log.info("Ledger transaction saved: {} with {} entries",
                txn.getTransactionNumber(), txn.getEntries().size());
    }

    private String generateTransactionNumber(UUID tenantId) {
        String prefix = tenantId.toString().substring(0, 4).toUpperCase();
        long seq = transactionSequence.incrementAndGet();
        return String.format("TXN-%s-%06d", prefix, seq % 1000000);
    }
}
