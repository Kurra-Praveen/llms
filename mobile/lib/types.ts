/**
 * Shared domain types — mirrors the backend DTOs and the web frontend types.
 * (Will move to packages/shared in the monorepo step; duplicated here to keep
 * the mobile app self-contained for now.)
 */

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
  timestamp: string;
}

export interface PagedData<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type Role = 'SUPER_ADMIN' | 'LENDER_ADMIN' | 'LENDER_STAFF' | 'BORROWER';

export interface User {
  id: string;
  tenantId?: string;
  borrowerId?: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export type LoanStatus =
  | 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED'
  | 'ACTIVE' | 'DISBURSED' | 'CLOSED' | 'WRITTEN_OFF' | 'CANCELLED';

export type ScheduleStatus = 'PENDING' | 'PARTIAL' | 'PAID' | 'OVERDUE' | 'WAIVED';

export interface Loan {
  id: string;
  borrowerId: string;
  loanNumber: string;
  principalAmount: number;
  interestRate: number;
  interestType: string;
  tenureMonths: number;
  repaymentFrequency: string;
  emiAmount?: number;
  totalPaid: number;
  totalOutstanding?: number;
  outstandingPrincipal: number;
  outstandingInterest: number;
  outstandingPenalty: number;
  status: LoanStatus;
  disbursementDate?: string;
  maturityDate?: string;
  dpd?: number;
}

export interface RepaymentSchedule {
  id: string;
  installmentNumber: number;
  dueDate: string;
  installmentAmount: number;
  totalPaid: number;
  status: ScheduleStatus;
  daysPastDue?: number;
}

export interface Payment {
  id: string;
  loanId: string;
  loanNumber?: string;
  paymentNumber: string;
  paymentDate: string;
  amountPaid: number;
  paymentMethod: string;
  receiptNumber?: string;
  status: string;
}

export interface NextEmi {
  loanId: string;
  loanNumber?: string;
  installmentNumber: number;
  dueDate: string;
  installmentAmount: number;
  outstandingAmount: number;
  status: ScheduleStatus;
  daysPastDue?: number;
}

export interface PortalSummary {
  activeLoansCount: number;
  totalOutstanding: number;
  nextEmi?: NextEmi;
}

export interface AppNotification {
  id: string;
  channel: string;
  subject?: string;
  content: string;
  loanId?: string;
  status: string;
  readAt?: string;
  createdAt: string;
}
