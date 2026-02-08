package com.loanplatform.common.entity;

import com.loanplatform.common.config.TenantContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    protected void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.requireTenant();
        }
        validateTenantAccess();
    }

    @PreUpdate
    @PreRemove
    protected void validateTenantAccess() {
        if (TenantContext.hasTenant()) {
            UUID currentTenant = TenantContext.getCurrentTenant();
            if (!currentTenant.equals(this.tenantId)) {
                throw new SecurityException("Cross-tenant data access denied");
            }
        }
    }

    public void setTenantId(UUID tenantId) {
        if (this.tenantId != null && !this.tenantId.equals(tenantId)) {
            throw new IllegalStateException("Tenant ID cannot be changed once set");
        }
        this.tenantId = tenantId;
    }
}
