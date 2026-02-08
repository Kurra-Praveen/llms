package com.loanplatform.ledger.entity;

import com.loanplatform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "ledger_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LedgerAccount extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system_account")
    @Builder.Default
    private Boolean isSystemAccount = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    public boolean increasesOnDebit() {
        return accountType == AccountType.ASSET || accountType == AccountType.EXPENSE;
    }

    public boolean increasesOnCredit() {
        return accountType == AccountType.LIABILITY || accountType == AccountType.INCOME;
    }
}
