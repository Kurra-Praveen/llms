/**
 * API Response Types
 * Matches backend ApiResponse structure
 */

// Generic API Response wrapper
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
  errors?: string[];
  timestamp: string;
}

// Paginated response
export interface PagedData<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type PagedResponse<T> = ApiResponse<PagedData<T>>;

// User & Auth types
export type Role = 'SUPER_ADMIN' | 'LENDER_ADMIN' | 'LENDER_STAFF' | 'BORROWER';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'PENDING_VERIFICATION';

export interface User {
  id: string;
  tenantId?: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: Role;
  status: UserStatus;
  lastLoginAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

// Tenant types
export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';

export interface Tenant {
  id: string;
  businessName: string;
  businessCode: string;
  contactEmail: string;
  contactPhone?: string;
  address?: string;
  status: TenantStatus;
  subscriptionPlan: string;
  maxBorrowers: number;
  maxLoans: number;
  createdAt: string;
}

export interface CreateTenantRequest {
  businessName: string;
  businessCode: string;
  contactEmail: string;
  contactPhone?: string;
  address?: string;
  subscriptionPlan?: string;
  maxBorrowers?: number;
  maxLoans?: number;
  adminEmail: string;
  adminPassword: string;
  adminFirstName: string;
  adminLastName: string;
}

// Borrower types
export type BorrowerStatus = 'ACTIVE' | 'BLOCKED' | 'BLACKLISTED' | 'INACTIVE';
export type RiskBand = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH' | 'UNRATED';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type IdType = 'NATIONAL_ID' | 'PASSPORT' | 'DRIVING_LICENSE' | 'VOTER_ID' | 'OTHER';

export interface Borrower {
  id: string;
  borrowerCode: string;
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  phone: string;
  alternatePhone?: string;
  email?: string;
  idType?: IdType;
  idNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  occupation?: string;
  employerName?: string;
  monthlyIncome?: number;
  creditRating?: string;
  riskScore?: number;
  riskBand?: RiskBand;
  status: BorrowerStatus;
  notes?: string;
  totalLoans?: number;
  activeLoans?: number;
  totalOutstanding?: number;
  createdAt: string;
}

export interface CreateBorrowerRequest {
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  phone: string;
  alternatePhone?: string;
  email?: string;
  idType?: IdType;
  idNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  occupation?: string;
  employerName?: string;
  monthlyIncome?: number;
  notes?: string;
}

// Loan types
export type LoanStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'ACTIVE'
  | 'DISBURSED'
  | 'CLOSED'
  | 'WRITTEN_OFF'
  | 'CANCELLED';

export type InterestType =
  | 'FLAT'
  | 'REDUCING_BALANCE'
  | 'SIMPLE'
  | 'INTEREST_ONLY'
  | 'BULLET'
  | 'DAILY_FIXED';

export type RepaymentFrequency =
  | 'DAILY'
  | 'WEEKLY'
  | 'BI_WEEKLY'
  | 'MONTHLY'
  | 'QUARTERLY';

export type ScheduleStatus = 'PENDING' | 'PARTIAL' | 'PAID' | 'OVERDUE' | 'WAIVED';

export interface Loan {
  id: string;
  borrowerId: string;
  borrowerName?: string;
  borrowerCode?: string;
  loanNumber: string;
  principalAmount: number;
  interestRate: number;
  interestType: InterestType;
  tenureMonths: number;
  repaymentFrequency: RepaymentFrequency;
  processingFee?: number;
  otherCharges?: number;
  dailyFixedAmount?: number;  // Fixed rupee amount per day for DAILY_FIXED interest type
  deductChargesUpfront?: boolean;
  totalChargesDeducted?: number;
  netDisbursementAmount?: number;
  applicationDate: string;
  approvalDate?: string;
  disbursementDate?: string;
  firstPaymentDate?: string;
  maturityDate?: string;
  closureDate?: string;
  emiAmount?: number;
  totalInterest?: number;
  totalPayable?: number;
  outstandingPrincipal: number;
  outstandingInterest: number;
  outstandingPenalty: number;
  totalOutstanding?: number;
  totalPaid: number;
  status: LoanStatus;
  dpd?: number;
  dpdBucket?: string;
  isNpa?: boolean;
  hasCollateral?: boolean;
  collateralType?: string;
  collateralValue?: number;
  collateralDescription?: string;
  gracePeriodDays?: number;
  notes?: string;
  createdAt: string;
}

export interface CreateLoanRequest {
  borrowerId: string;
  principalAmount: number;
  interestRate: number;
  interestType: InterestType;
  tenureMonths: number;
  repaymentFrequency?: RepaymentFrequency;
  processingFee?: number;
  otherCharges?: number;
  dailyFixedAmount?: number;  // Required when interestType is DAILY_FIXED
  deductChargesUpfront?: boolean;
  gracePeriodDays?: number;
  hasCollateral?: boolean;
  collateralType?: string;
  collateralValue?: number;
  collateralDescription?: string;
  notes?: string;
}

export interface DisburseLoanRequest {
  disbursementDate: string;
  firstPaymentDate: string;
}

export interface RepaymentSchedule {
  id: string;
  installmentNumber: number;
  dueDate: string;
  principalComponent: number;
  interestComponent: number;
  installmentAmount: number;
  outstandingAfter: number;
  principalPaid: number;
  interestPaid: number;
  penaltyPaid: number;
  totalPaid: number;
  penaltyAmount: number;
  status: ScheduleStatus;
  paidDate?: string;
  daysPastDue?: number;
}

// Payment types
export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
export type PaymentMethod =
  | 'CASH'
  | 'BANK_TRANSFER'
  | 'MOBILE_MONEY'
  | 'CHEQUE'
  | 'CARD'
  | 'WALLET'
  | 'OTHER';

export interface Payment {
  id: string;
  loanId: string;
  loanNumber?: string;
  borrowerName?: string;
  paymentNumber: string;
  paymentDate: string;
  paymentTime?: string;
  amountPaid: number;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  transactionId?: string;
  principalPaid: number;
  interestPaid: number;
  penaltyPaid: number;
  excessAmount: number;
  receiptNumber?: string;
  status: PaymentStatus;
  isReversed?: boolean;
  reversalReason?: string;
  notes?: string;
  allocations?: PaymentAllocation[];
  createdAt: string;
}

export interface PaymentAllocation {
  installmentNumber?: number;
  dueDate?: string;
  principalAllocated: number;
  interestAllocated: number;
  penaltyAllocated?: number;
  totalAllocated: number;
}

export interface RecordPaymentRequest {
  loanId: string;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  transactionId?: string;
  idempotencyKey?: string;
  notes?: string;
}

// Dashboard types
export interface DashboardSummary {
  totalBorrowers: number;
  activeBorrowers: number;
  totalLoans: number;
  activeLoans: number;
  totalDisbursed: number;
  totalOutstanding: number;
  totalCollected: number;
  overdueAmount: number;
  paymentsToday: number;
  collectionToday: number;
}
