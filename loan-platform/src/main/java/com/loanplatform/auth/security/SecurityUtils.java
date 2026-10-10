package com.loanplatform.auth.security;

import com.loanplatform.auth.entity.Role;
import com.loanplatform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Helper to resolve the authenticated user from the security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("Authentication required", HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED");
        }
        return principal;
    }

    /**
     * Returns the borrower record linked to the authenticated BORROWER-role user.
     * Throws if the user is not a borrower or has no linked borrower record.
     */
    public static UUID requireBorrowerId() {
        UserPrincipal user = getCurrentUser();
        if (user.getRole() != Role.BORROWER) {
            throw new BusinessException("Borrower access required", HttpStatus.FORBIDDEN, "BORROWER_REQUIRED");
        }
        if (user.getBorrowerId() == null) {
            throw new BusinessException("User account is not linked to a borrower record",
                    HttpStatus.FORBIDDEN, "BORROWER_NOT_LINKED");
        }
        return user.getBorrowerId();
    }
}
