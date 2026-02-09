import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeftIcon,
  ArrowPathIcon,
  CheckCircleIcon,
  XCircleIcon,
} from '@heroicons/react/24/outline';
import { Button, Card, CardHeader, StatusBadge, Spinner } from '@/components/ui';
import { paymentService } from '@/services/paymentService';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/contexts/ToastContext';
import { useState } from 'react';

export function PaymentDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [reversalReason, setReversalReason] = useState('');
  const [showReversalDialog, setShowReversalDialog] = useState(false);

  const isAdmin = user?.role === 'SUPER_ADMIN' || user?.role === 'LENDER_ADMIN';

  const { data: payment, isLoading, error } = useQuery({
    queryKey: ['payment', id],
    queryFn: () => paymentService.getById(id!),
    enabled: !!id,
  });

  const reversalMutation = useMutation({
    mutationFn: (reason: string) => paymentService.reverse(id!, reason),
    onSuccess: () => {
      showToast('Payment reversed successfully', 'success');
      queryClient.invalidateQueries({ queryKey: ['payment', id] });
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setShowReversalDialog(false);
      setReversalReason('');
    },
    onError: (error: Error) => {
      showToast(error.message || 'Failed to reverse payment', 'error');
    },
  });

  const handleReversal = () => {
    if (!reversalReason.trim()) {
      showToast('Please provide a reason for reversal', 'warning');
      return;
    }
    reversalMutation.mutate(reversalReason);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner />
      </div>
    );
  }

  if (error || !payment) {
    return (
      <div className="text-center py-12">
        <XCircleIcon className="h-12 w-12 mx-auto text-red-400" />
        <h2 className="text-xl font-semibold text-gray-900 mt-4">Payment Not Found</h2>
        <p className="text-gray-500 mt-2">The payment you're looking for doesn't exist.</p>
        <Button onClick={() => navigate('/payments')} className="mt-4">
          Back to Payments
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/payments')}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <ArrowLeftIcon className="h-5 w-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Payment {payment.paymentNumber}
            </h1>
            <p className="text-gray-500 mt-1">
              {new Date(payment.paymentDate).toLocaleDateString('en-IN', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={payment.status} type="payment" />
          {isAdmin && payment.status === 'COMPLETED' && !payment.isReversed && (
            <Button
              variant="outline"
              onClick={() => setShowReversalDialog(true)}
              leftIcon={<ArrowPathIcon className="h-4 w-4" />}
            >
              Reverse Payment
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Payment Details */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader title="Payment Details" />
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm text-gray-500">Payment Number</label>
                <p className="font-medium text-gray-900">{payment.paymentNumber}</p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Receipt Number</label>
                <p className="font-medium text-gray-900">{payment.receiptNumber || '-'}</p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Payment Date</label>
                <p className="font-medium text-gray-900">
                  {new Date(payment.paymentDate).toLocaleDateString()}
                </p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Payment Method</label>
                <p className="font-medium text-gray-900">
                  {payment.paymentMethod.replace('_', ' ')}
                </p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Reference Number</label>
                <p className="font-medium text-gray-900">{payment.referenceNumber || '-'}</p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Transaction ID</label>
                <p className="font-medium text-gray-900">{payment.transactionId || '-'}</p>
              </div>
            </div>
            {payment.notes && (
              <div className="mt-4 pt-4 border-t">
                <label className="text-sm text-gray-500">Notes</label>
                <p className="text-gray-900 mt-1">{payment.notes}</p>
              </div>
            )}
          </Card>

          {/* Allocation Breakdown */}
          <Card>
            <CardHeader title="Payment Allocation" subtitle="How this payment was applied" />
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div className="text-center p-4 bg-blue-50 rounded-lg">
                  <p className="text-2xl font-bold text-blue-600">
                    ₹{payment.principalPaid.toLocaleString()}
                  </p>
                  <p className="text-sm text-blue-700 mt-1">Principal</p>
                </div>
                <div className="text-center p-4 bg-green-50 rounded-lg">
                  <p className="text-2xl font-bold text-green-600">
                    ₹{payment.interestPaid.toLocaleString()}
                  </p>
                  <p className="text-sm text-green-700 mt-1">Interest</p>
                </div>
                <div className="text-center p-4 bg-orange-50 rounded-lg">
                  <p className="text-2xl font-bold text-orange-600">
                    ₹{payment.penaltyPaid.toLocaleString()}
                  </p>
                  <p className="text-sm text-orange-700 mt-1">Penalty</p>
                </div>
              </div>

              {payment.excessAmount > 0 && (
                <div className="p-4 bg-purple-50 rounded-lg">
                  <div className="flex items-center justify-between">
                    <span className="text-purple-700 font-medium">Excess Amount</span>
                    <span className="text-xl font-bold text-purple-600">
                      ₹{payment.excessAmount.toLocaleString()}
                    </span>
                  </div>
                  <p className="text-sm text-purple-600 mt-1">
                    This amount will be applied to future installments
                  </p>
                </div>
              )}

              {payment.allocations && payment.allocations.length > 0 && (
                <div className="mt-4">
                  <h4 className="font-medium text-gray-900 mb-3">Installment Allocation</h4>
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                            Installment
                          </th>
                          <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                            Due Date
                          </th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                            Principal
                          </th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                            Interest
                          </th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                            Penalty
                          </th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                            Total
                          </th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {payment.allocations.map((alloc, idx) => (
                          <tr key={idx}>
                            <td className="px-4 py-3 text-sm text-gray-900">
                              #{alloc.installmentNumber}
                            </td>
                            <td className="px-4 py-3 text-sm text-gray-500">
                              {alloc.dueDate ? new Date(alloc.dueDate).toLocaleDateString() : '-'}
                            </td>
                            <td className="px-4 py-3 text-sm text-right text-gray-900">
                              ₹{alloc.principalAllocated.toLocaleString()}
                            </td>
                            <td className="px-4 py-3 text-sm text-right text-gray-900">
                              ₹{alloc.interestAllocated.toLocaleString()}
                            </td>
                            <td className="px-4 py-3 text-sm text-right text-gray-900">
                              ₹{(alloc.penaltyAllocated || 0).toLocaleString()}
                            </td>
                            <td className="px-4 py-3 text-sm text-right font-medium text-gray-900">
                              ₹{alloc.totalAllocated.toLocaleString()}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Amount Summary */}
          <Card className="bg-gradient-to-br from-primary-50 to-primary-100 border-primary-200">
            <div className="text-center">
              <p className="text-sm text-primary-700">Total Amount Paid</p>
              <p className="text-3xl font-bold text-primary-900 mt-1">
                ₹{payment.amountPaid.toLocaleString()}
              </p>
            </div>
          </Card>

          {/* Loan Info */}
          <Card>
            <CardHeader title="Loan Information" />
            <div className="space-y-3">
              <div>
                <label className="text-sm text-gray-500">Loan Number</label>
                <p className="font-medium text-gray-900">
                  <button
                    onClick={() => navigate(`/loans/${payment.loanId}`)}
                    className="text-primary-600 hover:text-primary-700 hover:underline"
                  >
                    {payment.loanNumber || payment.loanId}
                  </button>
                </p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Borrower</label>
                <p className="font-medium text-gray-900">{payment.borrowerName || '-'}</p>
              </div>
            </div>
          </Card>

          {/* Status Info */}
          {payment.isReversed && (
            <Card className="bg-red-50 border-red-200">
              <div className="flex items-start gap-3">
                <XCircleIcon className="h-6 w-6 text-red-600 flex-shrink-0" />
                <div>
                  <p className="font-medium text-red-900">Payment Reversed</p>
                  {payment.reversalReason && (
                    <p className="text-sm text-red-700 mt-1">
                      Reason: {payment.reversalReason}
                    </p>
                  )}
                </div>
              </div>
            </Card>
          )}

          {payment.status === 'COMPLETED' && !payment.isReversed && (
            <Card className="bg-green-50 border-green-200">
              <div className="flex items-start gap-3">
                <CheckCircleIcon className="h-6 w-6 text-green-600 flex-shrink-0" />
                <div>
                  <p className="font-medium text-green-900">Payment Successful</p>
                  <p className="text-sm text-green-700 mt-1">
                    Recorded on {new Date(payment.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Reversal Dialog */}
      {showReversalDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Reverse Payment</h3>
            <p className="text-gray-600 mb-4">
              This will reverse the payment of <strong>₹{payment.amountPaid.toLocaleString()}</strong>
              and restore the loan balance. This action cannot be undone.
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Reason for Reversal
              </label>
              <textarea
                value={reversalReason}
                onChange={(e) => setReversalReason(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                rows={3}
                placeholder="Enter the reason for reversing this payment..."
              />
            </div>
            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => {
                  setShowReversalDialog(false);
                  setReversalReason('');
                }}
              >
                Cancel
              </Button>
              <Button
                onClick={handleReversal}
                isLoading={reversalMutation.isPending}
                className="bg-red-600 hover:bg-red-700"
              >
                Reverse Payment
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default PaymentDetailsPage;
