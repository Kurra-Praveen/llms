package com.loanplatform.ledger.entity;

public enum TransactionType {
    LOAN_DISBURSEMENT,
    PAYMENT_RECEIVED,
    INTEREST_ACCRUAL,
    PENALTY_APPLIED,
    PENALTY_WAIVER,
    PAYMENT_REVERSAL,
    WRITE_OFF,
    ADJUSTMENT
}
