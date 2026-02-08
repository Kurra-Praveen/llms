import { z } from 'zod';

export const borrowerSchema = z.object({
  fullName: z.string().min(3, 'Full name must be at least 3 characters'),
  email: z.string().email('Invalid email address').optional().or(z.literal('')),
  phone: z.string().min(10, 'Phone number must be at least 10 characters'),
  alternatePhone: z.string().optional(),
  dateOfBirth: z.string().optional(),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']).optional(),
  idType: z.enum(['NATIONAL_ID', 'PASSPORT', 'DRIVING_LICENSE', 'VOTER_ID', 'OTHER']).optional(),
  idNumber: z.string().optional(),
  addressLine1: z.string().optional(),
  addressLine2: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  postalCode: z.string().optional(),
  country: z.string().optional(),
  monthlyIncome: z.coerce.number().min(0, 'Income must be a positive number').optional(),
  occupation: z.string().optional(),
  employerName: z.string().optional(),
  notes: z.string().optional(),
});

export type BorrowerFormData = z.infer<typeof borrowerSchema>;
