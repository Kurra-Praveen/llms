package com.loanplatform.common.exception;

import org.springframework.http.HttpStatus;

public class TenantAccessDeniedException extends BusinessException {

    public TenantAccessDeniedException() {
        super("Access denied: Cross-tenant data access is not allowed", HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED");
    }

    public TenantAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED");
    }
}
