import { useState, useMemo } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { Button, Input, Card } from '@/components/ui';
import { loanSchema, type LoanFormData } from '@/lib/validations/loan';
import { loanService } from '@/services/loanService';
import { borrowerService } from '@/services/borrowerService';
import { logger } from '@/utils/logger';
import { CalculatorIcon } from '@heroicons/react/24/outline';

const formLogger = logger.scope('LoanForm');

// EMI Calculation helper functions
function calculateEMI(principal: number, annualRate: number, tenureMonths: number, interestType: string, frequency: string, dailyFixedAmount?: number): {
  emiAmount: number;
  totalInterest: number;
  totalPayable: number;
  numberOfPayments: number;
} {
  if (principal <= 0 || tenureMonths <= 0) {
    return { emiAmount: 0, totalInterest: 0, totalPayable: 0, numberOfPayments: 0 };
  }

  // Calculate number of payments based on frequency
  let numberOfPayments = tenureMonths;
  let periodicRate = annualRate / 100 / 12;

  switch (frequency) {
    case 'WEEKLY':
      numberOfPayments = tenureMonths * 4; // ~4 weeks per month
      periodicRate = annualRate / 100 / 52;
      break;
    case 'BI_WEEKLY':
      numberOfPayments = tenureMonths * 2;
      periodicRate = annualRate / 100 / 26;
      break;
    case 'DAILY':
      numberOfPayments = tenureMonths * 30;
      periodicRate = annualRate / 100 / 365;
      break;
    case 'QUARTERLY':
      numberOfPayments = Math.ceil(tenureMonths / 3);
      periodicRate = annualRate / 100 / 4;
      break;
    default: // MONTHLY
      numberOfPayments = tenureMonths;
      periodicRate = annualRate / 100 / 12;
  }

  let emiAmount = 0;
  let totalInterest = 0;
  let totalPayable = 0;

  switch (interestType) {
    case 'DAILY_FIXED':
      // Daily Fixed: Fixed rupee amount per ₹100 of principal per day
      // Daily Interest = (Principal / 100) × Daily Fixed Rate
      // Total Interest = Daily Interest × Total Days in tenure
      const totalDays = tenureMonths * 30;
      const dailyRate = dailyFixedAmount || 0;
      const dailyInterest = (principal / 100) * dailyRate;
      totalInterest = dailyInterest * totalDays;
      totalPayable = principal + totalInterest;
      emiAmount = totalPayable / numberOfPayments;
      break;

    case 'FLAT':
      // Flat: Total Interest = P * R * T, EMI = (P + Total Interest) / N
      totalInterest = principal * (annualRate / 100) * (tenureMonths / 12);
      totalPayable = principal + totalInterest;
      emiAmount = totalPayable / numberOfPayments;
      break;

    case 'REDUCING_BALANCE':
      // EMI formula: P * r * (1+r)^n / ((1+r)^n - 1)
      if (periodicRate === 0) {
        emiAmount = principal / numberOfPayments;
        totalInterest = 0;
      } else {
        const factor = Math.pow(1 + periodicRate, numberOfPayments);
        emiAmount = (principal * periodicRate * factor) / (factor - 1);
        totalInterest = (emiAmount * numberOfPayments) - principal;
      }
      totalPayable = principal + totalInterest;
      break;

    case 'SIMPLE':
      // Simple Interest: I = P * R * T
      totalInterest = principal * (annualRate / 100) * (tenureMonths / 12);
      totalPayable = principal + totalInterest;
      emiAmount = totalPayable / numberOfPayments;
      break;

    case 'INTEREST_ONLY':
      // Interest only payments, principal at end
      const monthlyInterest = principal * (annualRate / 100) / 12;
      emiAmount = monthlyInterest;
      totalInterest = monthlyInterest * tenureMonths;
      totalPayable = principal + totalInterest;
      break;

    case 'BULLET':
      // All payment at end
      totalInterest = principal * (annualRate / 100) * (tenureMonths / 12);
      totalPayable = principal + totalInterest;
      emiAmount = totalPayable; // Single payment
      numberOfPayments = 1;
      break;

    default:
      totalPayable = principal;
      emiAmount = principal / numberOfPayments;
  }

  return {
    emiAmount: Math.round(emiAmount * 100) / 100,
    totalInterest: Math.round(totalInterest * 100) / 100,
    totalPayable: Math.round(totalPayable * 100) / 100,
    numberOfPayments,
  };
}

export function LoanForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchTerm] = useState('');

  // Fetch borrowers for selection
  const { data: borrowersPage } = useQuery({
    queryKey: ['borrowers', 'search', searchTerm],
    queryFn: () => borrowerService.search(searchTerm),
    enabled: true, // Always enable to show initial list
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<LoanFormData>({
    resolver: zodResolver(loanSchema),
    defaultValues: {
      interestType: 'FLAT',
      repaymentFrequency: 'MONTHLY',
      principalAmount: 0,
      interestRate: 0,
      tenureMonths: 12,
      processingFee: 0,
      otherCharges: 0,
      deductChargesUpfront: true,
      gracePeriodDays: 0,
      hasCollateral: false,
    },
  });

  const hasCollateral = watch('hasCollateral');
  const principalAmount = watch('principalAmount');
  const interestRate = watch('interestRate');
  const tenureMonths = watch('tenureMonths');
  const interestType = watch('interestType');
  const repaymentFrequency = watch('repaymentFrequency');
  const processingFee = watch('processingFee');
  const otherCharges = watch('otherCharges');
  const deductChargesUpfront = watch('deductChargesUpfront');
  const dailyFixedAmount = watch('dailyFixedAmount');

  // Calculate total charges and net disbursement
  const totalCharges = (Number(processingFee) || 0) + (Number(otherCharges) || 0);
  const netDisbursement = deductChargesUpfront
    ? (Number(principalAmount) || 0) - totalCharges
    : (Number(principalAmount) || 0);

  // Calculate EMI preview
  const emiPreview = useMemo(() => {
    return calculateEMI(
      Number(principalAmount) || 0,
      Number(interestRate) || 0,
      Number(tenureMonths) || 0,
      interestType || 'FLAT',
      repaymentFrequency || 'MONTHLY',
      Number(dailyFixedAmount) || 0
    );
  }, [principalAmount, interestRate, tenureMonths, interestType, repaymentFrequency, dailyFixedAmount]);

  const getFrequencyLabel = (freq: string) => {
    switch (freq) {
      case 'WEEKLY': return 'Weekly';
      case 'BI_WEEKLY': return 'Bi-Weekly';
      case 'DAILY': return 'Daily';
      case 'QUARTERLY': return 'Quarterly';
      default: return 'Monthly';
    }
  };

  const mutation = useMutation({
    mutationFn: (data: LoanFormData) => loanService.create(data),
    onSuccess: (data) => {
      formLogger.info('Loan created', { id: data.id });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      navigate(`/loans/${data.id}`);
    },
    onError: (error) => {
      formLogger.error('Failed to create loan', { error });
    },
  });

  const onSubmit = (data: LoanFormData) => {
    formLogger.debug('Form submitted', { data });
    mutation.mutate(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">New Loan Application</h1>
        <p className="text-gray-500 mt-1">Create a new loan application for a borrower.</p>
      </div>

      <Card>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="col-span-full">
            <h3 className="text-lg font-medium text-gray-900">Borrower Selection</h3>
          </div>

          <div className="col-span-full">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Select Borrower <span className="text-red-500">*</span>
            </label>
            <Controller
              control={control}
              name="borrowerId"
              render={({ field }) => (
                <select
                  {...field}
                  className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                >
                  <option value="">Select a borrower...</option>
                  {borrowersPage?.content.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.fullName} ({b.borrowerCode}) - {b.phone}
                    </option>
                  ))}
                </select>
              )}
            />
            {errors.borrowerId?.message && (
              <p className="text-sm text-red-600 mt-1">{errors.borrowerId.message}</p>
            )}
            <p className="text-xs text-gray-500 mt-1">
              Can't find the borrower? <a href="/borrowers/new" className="text-primary-600 hover:underline">Add new borrower</a>
            </p>
          </div>

          <div className="col-span-full border-t border-gray-200 my-4" />

          <div className="col-span-full">
            <h3 className="text-lg font-medium text-gray-900">Loan Details</h3>
          </div>

          <Input
            label="Principal Amount"
            type="number"
            {...register('principalAmount')}
            error={errors.principalAmount?.message}
            required
            leftIcon={<span className="text-gray-500">₹</span>}
          />

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">Interest Type</label>
            <select
              {...register('interestType')}
              className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            >
              <option value="FLAT">Flat Rate</option>
              <option value="REDUCING_BALANCE">Reducing Balance (EMI)</option>
              <option value="SIMPLE">Simple Interest</option>
              <option value="DAILY_FIXED">Daily Fixed (₹/day)</option>
              {/* <option value="INTEREST_ONLY">Interest Only</option> */}
              {/* <option value="BULLET">Bullet / Balloon</option> */}
            </select>
            {errors.interestType?.message && (
              <p className="text-sm text-red-600">{errors.interestType.message}</p>
            )}
            {interestType === 'DAILY_FIXED' && (
              <p className="text-xs text-gray-500 mt-1">
                Fixed rupee amount per ₹100 of principal per day (e.g., ₹5 means ₹50/day for ₹1000 loan)
              </p>
            )}
          </div>

          {interestType === 'DAILY_FIXED' ? (
            <Input
              label="Daily Rate (₹ per ₹100/day)"
              type="number"
              step="0.5"
              {...register('dailyFixedAmount')}
              error={errors.dailyFixedAmount?.message}
              required
              leftIcon={<span className="text-gray-500">₹</span>}
              helpText="Amount charged per ₹100 of principal per day"
            />
          ) : (
            <Input
              label="Interest Rate (% per annum)"
              type="number"
              step="0.01"
              {...register('interestRate')}
              error={errors.interestRate?.message}
              required
            />
          )}

          <Input
            label="Tenure (Months)"
            type="number"
            {...register('tenureMonths')}
            error={errors.tenureMonths?.message}
            required
          />

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">Repayment Frequency</label>
            <select
              {...register('repaymentFrequency')}
              className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            >
              <option value="MONTHLY">Monthly</option>
              <option value="WEEKLY">Weekly</option>
              <option value="BI_WEEKLY">Bi-Weekly</option>
              <option value="DAILY">Daily</option>
              <option value="QUARTERLY">Quarterly</option>
            </select>
            {errors.repaymentFrequency?.message && (
              <p className="text-sm text-red-600">{errors.repaymentFrequency.message}</p>
            )}
          </div>

          <Input
            label="Processing Fee"
            type="number"
            {...register('processingFee')}
            error={errors.processingFee?.message}
            leftIcon={<span className="text-gray-500">₹</span>}
          />

          <Input
            label="Other Charges"
            type="number"
            {...register('otherCharges')}
            error={errors.otherCharges?.message}
            leftIcon={<span className="text-gray-500">₹</span>}
            helpText="Documentation, stamp duty, etc."
          />

          <div className="col-span-full">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                {...register('deductChargesUpfront')}
                className="h-4 w-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                defaultChecked={true}
              />
              <div>
                <span className="text-sm font-medium text-gray-700">Deduct charges from disbursement</span>
                <p className="text-xs text-gray-500">
                  If checked, processing fee and other charges will be deducted from the disbursed amount
                </p>
              </div>
            </label>
          </div>

          <Input
            label="Grace Period (Days)"
            type="number"
            {...register('gracePeriodDays')}
            error={errors.gracePeriodDays?.message}
            helpText="Days before penalties apply"
          />

          {/* EMI Preview Section */}
          {principalAmount > 0 && tenureMonths > 0 && (interestRate >= 0 || (interestType === 'DAILY_FIXED' && dailyFixedAmount > 0)) && (
            <>
              <div className="col-span-full border-t border-gray-200 my-4" />

              <div className="col-span-full">
                <div className="flex items-center gap-2 mb-4">
                  <CalculatorIcon className="h-5 w-5 text-primary-600" />
                  <h3 className="text-lg font-medium text-gray-900">
                    {interestType === 'DAILY_FIXED' ? 'Payment Preview' : 'EMI Preview'}
                  </h3>
                </div>

                <div className="bg-gradient-to-r from-primary-50 to-primary-100 rounded-lg p-6 border border-primary-200">
                  {interestType === 'DAILY_FIXED' && (
                    <div className="mb-4 pb-4 border-b border-primary-200">
                      <div className="grid grid-cols-2 gap-4 text-center">
                        <div>
                          <div className="text-xs uppercase tracking-wide text-amber-600 font-medium mb-1">
                            Rate per ₹100/day
                          </div>
                          <div className="text-xl font-bold text-amber-600">
                            ₹{Number(dailyFixedAmount || 0).toLocaleString()}
                          </div>
                        </div>
                        <div>
                          <div className="text-xs uppercase tracking-wide text-green-600 font-medium mb-1">
                            Daily Interest
                          </div>
                          <div className="text-xl font-bold text-green-600">
                            ₹{((Number(principalAmount) / 100) * Number(dailyFixedAmount || 0)).toLocaleString()}/day
                          </div>
                        </div>
                      </div>
                      <p className="text-xs text-gray-500 mt-2 text-center">
                        For ₹{Number(principalAmount).toLocaleString()} principal: (₹{Number(principalAmount).toLocaleString()} ÷ 100) × ₹{Number(dailyFixedAmount || 0)} = ₹{((Number(principalAmount) / 100) * Number(dailyFixedAmount || 0)).toLocaleString()}/day
                      </p>
                    </div>
                  )}

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="text-center">
                      <div className="text-xs uppercase tracking-wide text-primary-600 font-medium mb-1">
                        {getFrequencyLabel(repaymentFrequency)} Payment
                      </div>
                      <div className="text-2xl font-bold text-gray-900">
                        ₹{emiPreview.emiAmount.toLocaleString()}
                      </div>
                    </div>

                    <div className="text-center">
                      <div className="text-xs uppercase tracking-wide text-gray-500 font-medium mb-1">
                        Total Interest
                      </div>
                      <div className="text-xl font-semibold text-orange-600">
                        ₹{emiPreview.totalInterest.toLocaleString()}
                      </div>
                    </div>

                    <div className="text-center">
                      <div className="text-xs uppercase tracking-wide text-gray-500 font-medium mb-1">
                        Total Payable
                      </div>
                      <div className="text-xl font-semibold text-gray-900">
                        ₹{emiPreview.totalPayable.toLocaleString()}
                      </div>
                    </div>

                    <div className="text-center">
                      <div className="text-xs uppercase tracking-wide text-gray-500 font-medium mb-1">
                        No. of Payments
                      </div>
                      <div className="text-xl font-semibold text-gray-900">
                        {emiPreview.numberOfPayments}
                      </div>
                    </div>
                  </div>

                  {/* Disbursement Breakdown */}
                  {totalCharges > 0 && (
                    <div className="mt-4 pt-4 border-t border-primary-200">
                      <div className="grid grid-cols-3 gap-4 text-sm">
                        <div className="text-center">
                          <div className="text-xs uppercase tracking-wide text-gray-500 font-medium mb-1">
                            Principal Amount
                          </div>
                          <div className="font-semibold text-gray-900">
                            ₹{Number(principalAmount).toLocaleString()}
                          </div>
                        </div>
                        <div className="text-center">
                          <div className="text-xs uppercase tracking-wide text-gray-500 font-medium mb-1">
                            Total Charges
                          </div>
                          <div className="font-semibold text-red-600">
                            - ₹{totalCharges.toLocaleString()}
                          </div>
                        </div>
                        <div className="text-center">
                          <div className="text-xs uppercase tracking-wide text-gray-500 font-medium mb-1">
                            {deductChargesUpfront ? 'Net Disbursement' : 'Disbursement'}
                          </div>
                          <div className="font-semibold text-green-600">
                            ₹{netDisbursement.toLocaleString()}
                          </div>
                        </div>
                      </div>
                      {deductChargesUpfront && (
                        <p className="text-xs text-amber-600 text-center mt-2">
                          Charges will be deducted upfront from disbursement
                        </p>
                      )}
                    </div>
                  )}

                  <div className="mt-4 pt-4 border-t border-primary-200">
                    <p className="text-xs text-gray-600 text-center">
                      * This is an estimated calculation. Actual values may vary slightly based on disbursement date.
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}

          <div className="col-span-full border-t border-gray-200 my-4" />

          <div className="col-span-full">
            <div className="flex items-center gap-2 mb-4">
              <input
                type="checkbox"
                id="hasCollateral"
                {...register('hasCollateral')}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              />
              <label htmlFor="hasCollateral" className="text-lg font-medium text-gray-900">
                Collateral Information
              </label>
            </div>
          </div>

          {hasCollateral && (
            <>
              <Input
                label="Collateral Type"
                {...register('collateralType')}
                error={errors.collateralType?.message}
                placeholder="e.g. Vehicle, Property, Gold"
              />

              <Input
                label="Estimated Value"
                type="number"
                {...register('collateralValue')}
                error={errors.collateralValue?.message}
                leftIcon={<span className="text-gray-500">₹</span>}
              />

              <div className="col-span-full">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <textarea
                  {...register('collateralDescription')}
                  rows={3}
                  className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                  placeholder="Details about the collateral..."
                />
              </div>
            </>
          )}

          <div className="col-span-full">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Notes
            </label>
            <textarea
              {...register('notes')}
              rows={4}
              className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
              placeholder="Internal notes about this loan application..."
            />
          </div>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/loans')}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            Create Application
          </Button>
        </div>
      </Card>
    </form>
  );
}
