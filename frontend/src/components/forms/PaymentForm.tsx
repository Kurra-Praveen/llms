import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { Button, Input, Card, Spinner } from '@/components/ui';
import { paymentSchema, type PaymentFormData } from '@/lib/validations/payment';
import { paymentService } from '@/services/paymentService';
import { loanService } from '@/services/loanService';
import { logger } from '@/utils/logger';

const formLogger = logger.scope('PaymentForm');

export function PaymentForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const preselectedLoanId = searchParams.get('loanId');

  // If loanId is provided, fetch loan details to display info and set max amount
  const { data: loan, isLoading: isLoadingLoan } = useQuery({
    queryKey: ['loan', preselectedLoanId],
    queryFn: () => loanService.getById(preselectedLoanId!),
    enabled: !!preselectedLoanId,
  });

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<PaymentFormData>({
    resolver: zodResolver(paymentSchema) as any,
    defaultValues: {
      loanId: preselectedLoanId || '',
      amount: 0,
      paymentDate: new Date().toISOString().split('T')[0],
      paymentMethod: 'CASH',
    },
  });

  // Update loanId if URL param changes or loan is fetched
  useEffect(() => {
    if (preselectedLoanId) {
      setValue('loanId', preselectedLoanId);
    }
  }, [preselectedLoanId, setValue]);


  const mutation = useMutation({
    mutationFn: (data: PaymentFormData) => {
        // Add idempotency key
        const requestData = {
            ...data,
            idempotencyKey: paymentService.generateIdempotencyKey()
        };
        return paymentService.recordPayment(requestData);
    },
    onSuccess: (data) => {
      formLogger.info('Payment recorded', { id: data.id });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['loan', data.loanId] }); // Refresh loan details
      queryClient.invalidateQueries({ queryKey: ['loan-schedule', data.loanId] }); // Refresh schedule

      // Redirect back to loan details
      navigate(`/loans/${data.loanId}`);
    },
    onError: (error) => {
      formLogger.error('Failed to record payment', { error });
    },
  });

  const onSubmit = (data: PaymentFormData) => {
    formLogger.debug('Form submitted', { data });
    mutation.mutate(data);
  };

  if (isLoadingLoan) {
      return <Spinner fullScreen text="Loading loan details..." />;
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Record Payment</h1>
        <p className="text-gray-500 mt-1">Record a repayment for a loan.</p>
      </div>

      {loan && (
          <Card className="bg-blue-50 border-blue-100">
              <h3 className="text-lg font-semibold text-blue-900 mb-2">Loan Summary</h3>
              <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                      <span className="text-blue-700">Borrower:</span> <span className="font-medium text-blue-900">{loan.borrowerName}</span>
                  </div>
                   <div>
                      <span className="text-blue-700">Loan No:</span> <span className="font-medium text-blue-900">{loan.loanNumber}</span>
                  </div>
                  <div>
                      <span className="text-blue-700">Total Outstanding:</span> <span className="font-bold text-blue-900">₹{(loan.totalOutstanding ?? 0).toLocaleString()}</span>
                  </div>
                   <div>
                      <span className="text-blue-700">Overdue:</span> <span className="font-medium text-red-700">₹{(loan.outstandingPenalty + loan.outstandingInterest).toLocaleString()}</span>
                  </div>
              </div>
          </Card>
      )}

      <form onSubmit={handleSubmit(onSubmit)}>
        <Card>
          <div className="space-y-6">
             {!preselectedLoanId && (
                <Input
                  label="Loan ID"
                  {...register('loanId')}
                  error={errors.loanId?.message}
                  required
                  placeholder="Enter Loan ID"
                />
             )}

            <Input
              label="Payment Date"
              type="date"
              {...register('paymentDate')}
              error={errors.paymentDate?.message}
              required
            />

            <Input
              label="Amount Received"
              type="number"
              {...register('amount')}
              error={errors.amount?.message}
              required
              leftIcon={<span className="text-gray-500">₹</span>}
            />

            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700">Payment Method</label>
              <select
                {...register('paymentMethod')}
                className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
              >
                <option value="CASH">Cash</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="MOBILE_MONEY">Mobile Money</option>
                <option value="CHEQUE">Cheque</option>
                <option value="CARD">Card</option>
                <option value="WALLET">Wallet</option>
                <option value="OTHER">Other</option>
              </select>
              {errors.paymentMethod?.message && (
                <p className="text-sm text-red-600">{errors.paymentMethod.message}</p>
              )}
            </div>

             <Input
              label="Reference / Transaction ID"
              {...register('referenceNumber')}
              error={errors.referenceNumber?.message}
              placeholder="e.g. UTR12345678"
            />

            <div className="col-span-full">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Notes
                </label>
                <textarea
                  {...register('notes')}
                  rows={3}
                  className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                  placeholder="Additional notes about payment..."
                />
            </div>
          </div>

          <div className="mt-6 flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate(-1)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Record Payment
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
}
