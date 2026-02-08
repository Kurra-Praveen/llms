package com.loanplatform.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_MDC_KEY = "tenantId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantHeader = request.getHeader(TENANT_HEADER);

            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    UUID tenantId = UUID.fromString(tenantHeader);
                    TenantContext.setCurrentTenant(tenantId);
                    MDC.put(TENANT_MDC_KEY, tenantId.toString());
                    log.debug("Tenant context set: {}", tenantId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant ID format: {}", tenantHeader);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(TENANT_MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/api/v1/public/") ||
               path.startsWith("/api/v3/api-docs") ||
               path.startsWith("/api/swagger-ui");
    }
}
