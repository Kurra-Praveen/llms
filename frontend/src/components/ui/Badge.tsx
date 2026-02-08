/**
 * Badge Component
 * Status indicators and labels
 */

import React from 'react';

type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'gray' | 'primary';
type BadgeSize = 'sm' | 'md';

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  size?: BadgeSize;
  dot?: boolean;
  className?: string;
}

const variantClasses: Record<BadgeVariant, string> = {
  success: 'bg-green-100 text-green-800',
  warning: 'bg-yellow-100 text-yellow-800',
  danger: 'bg-red-100 text-red-800',
  info: 'bg-blue-100 text-blue-800',
  gray: 'bg-gray-100 text-gray-800',
  primary: 'bg-primary-100 text-primary-800',
};

const dotColors: Record<BadgeVariant, string> = {
  success: 'bg-green-500',
  warning: 'bg-yellow-500',
  danger: 'bg-red-500',
  info: 'bg-blue-500',
  gray: 'bg-gray-500',
  primary: 'bg-primary-500',
};

const sizeClasses: Record<BadgeSize, string> = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-sm',
};

export function Badge({
  children,
  variant = 'gray',
  size = 'sm',
  dot = false,
  className = '',
}: BadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center gap-1.5
        font-medium rounded-full
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${className}
      `}
    >
      {dot && (
        <span className={`w-1.5 h-1.5 rounded-full ${dotColors[variant]}`} />
      )}
      {children}
    </span>
  );
}

/**
 * Status badge with predefined mappings
 */
interface StatusBadgeProps {
  status: string;
  type?: 'loan' | 'borrower' | 'payment' | 'tenant';
  size?: BadgeSize;
}

const statusVariantMap: Record<string, BadgeVariant> = {
  // Loan statuses
  DRAFT: 'gray',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'info',
  REJECTED: 'danger',
  ACTIVE: 'success',
  DISBURSED: 'success',
  CLOSED: 'gray',
  WRITTEN_OFF: 'danger',
  CANCELLED: 'gray',

  // Borrower statuses
  BLOCKED: 'danger',
  BLACKLISTED: 'danger',
  INACTIVE: 'gray',

  // Payment statuses
  PENDING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger',
  REVERSED: 'warning',

  // Tenant statuses
  SUSPENDED: 'warning',

  // Schedule statuses
  PAID: 'success',
  PARTIAL: 'warning',
  OVERDUE: 'danger',
  WAIVED: 'gray',
};

export function StatusBadge({ status, size = 'sm' }: StatusBadgeProps) {
  const variant = statusVariantMap[status] || 'gray';
  const displayText = status.replace(/_/g, ' ');

  return (
    <Badge variant={variant} size={size} dot>
      {displayText}
    </Badge>
  );
}

export default Badge;
