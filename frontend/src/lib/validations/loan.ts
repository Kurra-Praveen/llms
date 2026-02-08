import { z } from 'zod';

export const loanSchema = z.object({
  borrowerId: z.string().min(1, 'Borrower is required'),
  principalAmount: z.coerce
    .number()
    .min(100, 'Principal amount must be at least 100')
    .max(10000000, 'Principal amount cannot exceed 10,000,000'),
  interestRate: z.coerce
    .number()
    .min(0, 'Interest rate cannot be negative')
    .max(100, 'Interest rate cannot exceed 100%'),
  interestType: z.enum(['FLAT', 'REDUCING_BALANCE', 'SIMPLE', 'INTEREST_ONLY', 'BULLET', 'DAILY_FIXED']),
  dailyFixedAmount: z.coerce.number().min(0, 'Daily fixed amount cannot be negative').optional(),
  tenureMonths: z.coerce
    .number()
    .min(1, 'Tenure must be at least 1 month')
    .max(120, 'Tenure cannot exceed 120 months'),
  repaymentFrequency: z.enum(['DAILY', 'WEEKLY', 'BI_WEEKLY', 'MONTHLY', 'QUARTERLY']),
  processingFee: z.coerce.number().min(0).optional(),
  otherCharges: z.coerce.number().min(0).optional(),
  deductChargesUpfront: z.boolean().optional().default(true),
  gracePeriodDays: z.coerce.number().min(0).optional(),
  hasCollateral: z.boolean().optional(),
  collateralType: z.string().optional(),
  collateralValue: z.coerce.number().optional(),
  collateralDescription: z.string().optional(),
  notes: z.string().optional(),
});

export type LoanFormData = z.infer<typeof loanSchema>;
