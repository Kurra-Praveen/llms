import { z } from 'zod';

export const paymentSchema = z.object({
  loanId: z.string().min(1, 'Loan is required'),
  amount: z.coerce
    .number()
    .min(1, 'Amount must be greater than 0'),
  paymentDate: z.string().refine((val) => !isNaN(Date.parse(val)), {
    message: 'Invalid date',
  }),
  paymentMethod: z.enum([
    'CASH',
    'BANK_TRANSFER',
    'MOBILE_MONEY',
    'CHEQUE',
    'CARD',
    'WALLET',
    'OTHER',
  ]),
  referenceNumber: z.string().optional(),
  transactionId: z.string().optional(),
  notes: z.string().optional(),
});

export type PaymentFormData = z.infer<typeof paymentSchema>;
