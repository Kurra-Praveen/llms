package com.loanplatform.common.config;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.loanplatform..repository.*Repository.*(..))")
    public void enableTenantFilter() {
        if (TenantContext.hasTenant()) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.getEnabledFilter("tenantFilter");
            if (filter != null) {
                return;
            }
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", TenantContext.getCurrentTenant());
            log.trace("Tenant filter enabled for tenant: {}", TenantContext.getCurrentTenant());
        }
    }
}
