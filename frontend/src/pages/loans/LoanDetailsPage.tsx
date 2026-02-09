import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeftIcon,
  CheckCircleIcon,
  XCircleIcon,
  BanknotesIcon,
  CurrencyRupeeIcon,
} from '@heroicons/react/24/outline';
import { Button, Card, CardHeader, StatusBadge, Spinner } from '@/components/ui';
import { loanService } from '@/services/loanService';
import type { RepaymentSchedule } from '@/types';

export function LoanDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [actionNote, setActionNote] = useState('');
  const [showActionModal, setShowActionModal] = useState<'approve' | 'reject' | 'disburse' | null>(null);
  const [disbursementDate, setDisbursementDate] = useState(new Date().toISOString().split('T')[0]);
  const [firstPaymentDate, setFirstPaymentDate] = useState('');

  const { data: loan, isLoading: isLoadingLoan } = useQuery({
    queryKey: ['loan', id],
    queryFn: () => loanService.getById(id!),
    enabled: !!id,
  });

  const { data: schedule, isLoading: isLoadingSchedule } = useQuery({
    queryKey: ['loan-schedule', id],
    queryFn: () => loanService.getSchedule(id!),
    enabled: !!id,
  });

  const approveMutation = useMutation({
    mutationFn: () => loanService.approve(id!, actionNote),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loan', id] });
      setShowActionModal(null);
      setActionNote('');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => loanService.reject(id!, actionNote),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loan', id] });
      setShowActionModal(null);
      setActionNote('');
    },
  });

  const disburseMutation = useMutation({
    mutationFn: () => loanService.disburse(id!, {
        disbursementDate: disbursementDate,
        firstPaymentDate: firstPaymentDate || calculateFirstPaymentDate(disbursementDate, loan?.repaymentFrequency || 'MONTHLY'),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loan', id] });
      queryClient.invalidateQueries({ queryKey: ['loan-schedule', id] });
      setShowActionModal(null);
      setDisbursementDate(new Date().toISOString().split('T')[0]);
      setFirstPaymentDate('');
    },
  });

  // Calculate first payment date based on disbursement date and frequency
  function calculateFirstPaymentDate(disbDate: string, frequency: string): string {
    const date = new Date(disbDate);
    switch (frequency) {
      case 'WEEKLY':
        date.setDate(date.getDate() + 7);
        break;
      case 'BI_WEEKLY':
        date.setDate(date.getDate() + 14);
        break;
      case 'DAILY':
        date.setDate(date.getDate() + 1);
        break;
      case 'QUARTERLY':
        date.setMonth(date.getMonth() + 3);
        break;
      default: // MONTHLY
        date.setMonth(date.getMonth() + 1);
    }
    return date.toISOString().split('T')[0];
  }

  // Get frequency label for display
  function getFrequencyLabel(frequency: string): string {
    switch (frequency) {
      case 'WEEKLY': return 'Weekly';
      case 'BI_WEEKLY': return 'Bi-Weekly';
      case 'DAILY': return 'Daily';
      case 'QUARTERLY': return 'Quarterly';
      default: return 'Monthly';
    }
  }

  if (isLoadingLoan || isLoadingSchedule) {
    return <Spinner fullScreen text="Loading loan details..." />;
  }

  if (!loan) {
    return <div>Loan not found</div>;
  }

  const handleAction = () => {
    if (showActionModal === 'approve') approveMutation.mutate();
    if (showActionModal === 'reject') rejectMutation.mutate();
    if (showActionModal === 'disburse') disburseMutation.mutate();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/loans')}
            className="p-2"
          >
            <ArrowLeftIcon className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Loan #{loan.loanNumber}</h1>
            <div className="flex items-center gap-2 mt-1">
              <span className="text-gray-500 text-sm">
                Borrower: <a href={`/borrowers/${loan.borrowerId}`} className="text-primary-600 hover:underline">{loan.borrowerName}</a>
              </span>
              <span className="text-gray-300">|</span>
              <StatusBadge status={loan.status} type="loan" />
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          {/* DRAFT status - can approve directly */}
          {loan.status === 'DRAFT' && (
            <Button
              variant="primary"
              leftIcon={<CheckCircleIcon className="h-4 w-4" />}
              onClick={() => setShowActionModal('approve')}
            >
              Approve Loan
            </Button>
          )}

          {loan.status === 'PENDING_APPROVAL' && (
            <>
              <Button
                variant="danger"
                leftIcon={<XCircleIcon className="h-4 w-4" />}
                onClick={() => setShowActionModal('reject')}
              >
                Reject
              </Button>
              <Button
                variant="success"
                leftIcon={<CheckCircleIcon className="h-4 w-4" />}
                onClick={() => setShowActionModal('approve')}
              >
                Approve
              </Button>
            </>
          )}

          {loan.status === 'APPROVED' && (
             <Button
                variant="primary"
                leftIcon={<BanknotesIcon className="h-4 w-4" />}
                onClick={() => setShowActionModal('disburse')}
              >
                Disburse Loan
              </Button>
          )}

          {(loan.status === 'DISBURSED' || loan.status === 'ACTIVE') && (
             <Button
                variant="primary"
                leftIcon={<CurrencyRupeeIcon className="h-4 w-4" />}
                onClick={() => navigate(`/payments/new?loanId=${loan.id}`)}
              >
                Record Payment
              </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader title="Loan Terms" />
            <dl className="grid grid-cols-1 sm:grid-cols-3 gap-x-4 gap-y-6">
              <div>
                <dt className="text-sm font-medium text-gray-500">Principal Amount</dt>
                <dd className="mt-1 text-lg font-bold text-gray-900">₹{loan.principalAmount.toLocaleString()}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">
                  {loan.interestType === 'DAILY_FIXED' ? 'Daily Rate (per ₹100)' : 'Interest Rate'}
                </dt>
                <dd className="mt-1 text-lg font-bold text-gray-900">
                  {loan.interestType === 'DAILY_FIXED' ? (
                    <>₹{(loan.dailyFixedAmount ?? 0).toLocaleString()} <span className="text-xs font-normal text-gray-500">per ₹100/day</span></>
                  ) : (
                    <>{loan.interestRate}% <span className="text-xs font-normal text-gray-500">p.a.</span></>
                  )}
                </dd>
                {loan.interestType === 'DAILY_FIXED' && (
                  <dd className="mt-1 text-sm text-green-600">
                    Daily interest: ₹{((loan.principalAmount / 100) * (loan.dailyFixedAmount ?? 0)).toLocaleString()}/day
                  </dd>
                )}
              </div>
               <div>
                <dt className="text-sm font-medium text-gray-500">Interest Type</dt>
                <dd className="mt-1 text-sm text-gray-900">
                  {loan.interestType === 'DAILY_FIXED' ? 'Daily Fixed (₹/day)' : loan.interestType.replace('_', ' ')}
                </dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Tenure</dt>
                <dd className="mt-1 text-sm text-gray-900">{loan.tenureMonths} Months</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Repayment Freq.</dt>
                <dd className="mt-1 text-sm text-gray-900">{loan.repaymentFrequency}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Application Date</dt>
                <dd className="mt-1 text-sm text-gray-900">{new Date(loan.applicationDate).toLocaleDateString()}</dd>
              </div>
            </dl>
          </Card>

          {/* Disbursement & Charges Details - Show after loan is disbursed */}
          {(loan.status === 'ACTIVE' || loan.status === 'DISBURSED' || loan.status === 'CLOSED') && loan.disbursementDate && (
            <Card>
              <CardHeader title="Disbursement Details" />
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-4">
                <div>
                  <dt className="text-sm font-medium text-gray-500">Disbursement Date</dt>
                  <dd className="mt-1 text-sm text-gray-900">{new Date(loan.disbursementDate).toLocaleDateString()}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">First Payment Date</dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {loan.firstPaymentDate ? new Date(loan.firstPaymentDate).toLocaleDateString() : '-'}
                  </dd>
                </div>

                {/* Charges Breakdown */}
                <div className="col-span-full border-t border-gray-200 pt-4 mt-2">
                  <h4 className="text-sm font-semibold text-gray-700 mb-3">Charges Breakdown</h4>
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="space-y-2">
                      <div className="flex justify-between text-sm">
                        <span className="text-gray-600">Loan Principal</span>
                        <span className="font-medium text-gray-900">₹{loan.principalAmount.toLocaleString()}</span>
                      </div>
                      {(loan.processingFee ?? 0) > 0 && (
                        <div className="flex justify-between text-sm">
                          <span className="text-gray-600">Processing Fee</span>
                          <span className="font-medium text-red-600">- ₹{(loan.processingFee ?? 0).toLocaleString()}</span>
                        </div>
                      )}
                      {(loan.otherCharges ?? 0) > 0 && (
                        <div className="flex justify-between text-sm">
                          <span className="text-gray-600">Other Charges</span>
                          <span className="font-medium text-red-600">- ₹{(loan.otherCharges ?? 0).toLocaleString()}</span>
                        </div>
                      )}
                      {(loan.totalChargesDeducted ?? 0) > 0 && (
                        <>
                          <div className="border-t border-gray-300 my-2" />
                          <div className="flex justify-between text-sm">
                            <span className="text-gray-600 font-medium">Total Charges Deducted</span>
                            <span className="font-bold text-red-600">- ₹{(loan.totalChargesDeducted ?? 0).toLocaleString()}</span>
                          </div>
                        </>
                      )}
                      <div className="border-t border-gray-300 my-2" />
                      <div className="flex justify-between text-sm">
                        <span className="text-gray-700 font-semibold">Net Amount Disbursed</span>
                        <span className="font-bold text-green-600 text-lg">
                          ₹{(loan.netDisbursementAmount ?? loan.principalAmount).toLocaleString()}
                        </span>
                      </div>
                    </div>

                    {loan.deductChargesUpfront && (loan.totalChargesDeducted ?? 0) > 0 && (
                      <div className="mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded text-xs text-yellow-700">
                        Note: Charges were deducted upfront from the disbursement amount. Interest is calculated on the full principal amount of ₹{loan.principalAmount.toLocaleString()}.
                      </div>
                    )}
                  </div>
                </div>
              </dl>
            </Card>
          )}

          {/* Repayment Schedule Table */}
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
             <div className="px-6 py-4 border-b border-gray-200">
               <h3 className="text-lg font-semibold text-gray-900">Repayment Schedule</h3>
             </div>

             {schedule && schedule.length > 0 ? (
               <div className="overflow-x-auto">
                 <table className="min-w-full divide-y divide-gray-200">
                   <thead className="bg-gray-50">
                     <tr>
                       <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">#</th>
                       <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Due Date</th>
                       <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                       <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Principal</th>
                       <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Interest</th>
                       <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Paid</th>
                       <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Status</th>
                     </tr>
                   </thead>
                   <tbody className="bg-white divide-y divide-gray-200">
                     {schedule.map((item: RepaymentSchedule) => (
                       <tr key={item.id} className="hover:bg-gray-50">
                         <td className="px-4 py-3 text-sm text-gray-900">{item.installmentNumber}</td>
                         <td className="px-4 py-3 text-sm text-gray-900">{new Date(item.dueDate).toLocaleDateString()}</td>
                         <td className="px-4 py-3 text-sm font-medium text-gray-900 text-right">₹{item.installmentAmount.toLocaleString()}</td>
                         <td className="px-4 py-3 text-sm text-gray-500 text-right">₹{item.principalComponent.toLocaleString()}</td>
                         <td className="px-4 py-3 text-sm text-gray-500 text-right">₹{item.interestComponent.toLocaleString()}</td>
                         <td className="px-4 py-3 text-sm text-gray-900 text-right">₹{item.totalPaid.toLocaleString()}</td>
                         <td className="px-4 py-3 text-center">
                           <StatusBadge status={item.status} size="sm" />
                         </td>
                       </tr>
                     ))}
                   </tbody>
                 </table>
               </div>
             ) : (
                <div className="p-8 text-center text-gray-500">
                   Schedule will be generated upon approval/disbursement.
                </div>
             )}
          </div>
        </div>

        {/* Sidebar Info */}
        <div className="space-y-6">
          <Card>
            <CardHeader title="Summary" />
            <div className="space-y-4">
               <div>
                 <div className="text-sm font-medium text-gray-500 mb-1">Outstanding Principal</div>
                 <div className="text-xl font-bold text-gray-900">₹{loan.outstandingPrincipal.toLocaleString()}</div>
               </div>
               <div>
                 <div className="text-sm font-medium text-gray-500 mb-1">Total Outstanding</div>
                 <div className="text-2xl font-bold text-primary-600">₹{(loan.totalOutstanding ?? 0).toLocaleString()}</div>
               </div>

               <div className="border-t border-gray-100 my-2"></div>

               <div className="flex justify-between">
                 <span className="text-sm text-gray-500">Interest Accrued</span>
                 <span className="text-sm font-medium">₹{loan.outstandingInterest.toLocaleString()}</span>
               </div>
               <div className="flex justify-between">
                 <span className="text-sm text-gray-500">Penalties</span>
                 <span className="text-sm font-medium text-red-600">₹{loan.outstandingPenalty.toLocaleString()}</span>
               </div>
            </div>
          </Card>

           <Card>
            <CardHeader title="Dates" />
            <div className="space-y-3">
               <div className="flex justify-between">
                 <span className="text-sm text-gray-500">Created</span>
                 <span className="text-sm text-gray-900">{new Date(loan.createdAt).toLocaleDateString()}</span>
               </div>
               {loan.approvalDate && (
                 <div className="flex justify-between">
                   <span className="text-sm text-gray-500">Approved</span>
                   <span className="text-sm text-gray-900">{new Date(loan.approvalDate).toLocaleDateString()}</span>
                 </div>
               )}
               {loan.disbursementDate && (
                 <div className="flex justify-between">
                   <span className="text-sm text-gray-500">Disbursed</span>
                   <span className="text-sm text-gray-900">{new Date(loan.disbursementDate).toLocaleDateString()}</span>
                 </div>
               )}
               {loan.maturityDate && (
                 <div className="flex justify-between">
                   <span className="text-sm text-gray-500">Maturity</span>
                   <span className="text-sm text-gray-900">{new Date(loan.maturityDate).toLocaleDateString()}</span>
                 </div>
               )}
            </div>
          </Card>

          {loan.hasCollateral && (
            <Card>
              <CardHeader title="Collateral" />
              <div className="space-y-3">
                <div>
                   <span className="text-xs text-gray-500 uppercase tracking-wide">Type</span>
                   <p className="font-medium text-gray-900">{loan.collateralType}</p>
                </div>
                <div>
                   <span className="text-xs text-gray-500 uppercase tracking-wide">Value</span>
                   <p className="font-medium text-gray-900">₹{loan.collateralValue?.toLocaleString()}</p>
                </div>
                <div>
                   <span className="text-xs text-gray-500 uppercase tracking-wide">Description</span>
                   <p className="text-sm text-gray-700">{loan.collateralDescription}</p>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Action Modal */}
      {showActionModal && (
         <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
               <h3 className="text-lg font-bold text-gray-900 mb-4">
                 {showActionModal === 'approve' && 'Approve Loan'}
                 {showActionModal === 'reject' && 'Reject Loan'}
                 {showActionModal === 'disburse' && 'Disburse Loan'}
               </h3>

               {showActionModal === 'disburse' ? (
                 <div className="space-y-4">
                   {/* Loan Summary */}
                   <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                     <div className="grid grid-cols-2 gap-4">
                       <div>
                         <span className="text-xs text-gray-500 uppercase">Principal</span>
                         <p className="font-bold text-lg text-gray-900">₹{loan.principalAmount.toLocaleString()}</p>
                       </div>
                       <div>
                         <span className="text-xs text-gray-500 uppercase">Borrower</span>
                         <p className="font-medium text-gray-900">{loan.borrowerName}</p>
                       </div>
                       <div>
                         <span className="text-xs text-gray-500 uppercase">Interest Rate</span>
                         <p className="font-medium text-gray-900">{loan.interestRate}% p.a.</p>
                       </div>
                       <div>
                         <span className="text-xs text-gray-500 uppercase">Repayment</span>
                         <p className="font-medium text-primary-600">{getFrequencyLabel(loan.repaymentFrequency)}</p>
                       </div>
                     </div>

                     {/* Charges breakdown */}
                     {((loan.processingFee ?? 0) > 0 || (loan.otherCharges ?? 0) > 0) && (
                       <div className="mt-4 pt-4 border-t border-gray-300">
                         <div className="space-y-2">
                           {(loan.processingFee ?? 0) > 0 && (
                             <div className="flex justify-between text-sm">
                               <span className="text-gray-600">Processing Fee</span>
                               <span className="font-medium text-red-600">- ₹{(loan.processingFee ?? 0).toLocaleString()}</span>
                             </div>
                           )}
                           {(loan.otherCharges ?? 0) > 0 && (
                             <div className="flex justify-between text-sm">
                               <span className="text-gray-600">Other Charges</span>
                               <span className="font-medium text-red-600">- ₹{(loan.otherCharges ?? 0).toLocaleString()}</span>
                             </div>
                           )}
                           <div className="border-t border-gray-200 pt-2 mt-2">
                             <div className="flex justify-between text-sm">
                               <span className="font-semibold text-gray-700">Net Disbursement</span>
                               <span className="font-bold text-green-600">
                                 ₹{(loan.principalAmount - ((loan.processingFee ?? 0) + (loan.otherCharges ?? 0))).toLocaleString()}
                               </span>
                             </div>
                           </div>
                           {loan.deductChargesUpfront && (
                             <p className="text-xs text-amber-600 mt-1">
                               Charges will be deducted upfront from disbursement
                             </p>
                           )}
                         </div>
                       </div>
                     )}
                   </div>

                   {/* Disbursement Date */}
                   <div>
                     <label className="block text-sm font-medium text-gray-700 mb-1">
                       Disbursement Date <span className="text-red-500">*</span>
                     </label>
                     <input
                       type="date"
                       className="w-full border border-gray-300 rounded-lg p-2 focus:ring-primary-500 focus:border-primary-500"
                       value={disbursementDate}
                       onChange={(e) => {
                         setDisbursementDate(e.target.value);
                         // Auto-calculate first payment date if not manually set
                         if (!firstPaymentDate) {
                           setFirstPaymentDate(calculateFirstPaymentDate(e.target.value, loan.repaymentFrequency));
                         }
                       }}
                       min={new Date().toISOString().split('T')[0]}
                     />
                   </div>

                   {/* First Payment Date */}
                   <div>
                     <label className="block text-sm font-medium text-gray-700 mb-1">
                       First Payment Date <span className="text-red-500">*</span>
                     </label>
                     <input
                       type="date"
                       className="w-full border border-gray-300 rounded-lg p-2 focus:ring-primary-500 focus:border-primary-500"
                       value={firstPaymentDate || calculateFirstPaymentDate(disbursementDate, loan.repaymentFrequency)}
                       onChange={(e) => setFirstPaymentDate(e.target.value)}
                       min={disbursementDate}
                     />
                     <p className="text-xs text-gray-500 mt-1">
                       Suggested: {calculateFirstPaymentDate(disbursementDate, loan.repaymentFrequency)} (based on {getFrequencyLabel(loan.repaymentFrequency).toLowerCase()} payments)
                     </p>
                   </div>

                   <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                     <p className="text-sm text-yellow-800">
                       <strong>Note:</strong> This will generate the repayment schedule and activate the loan. This action cannot be undone.
                     </p>
                   </div>
                 </div>
               ) : (
                 <div className="mb-4">
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                     Notes (Optional)
                   </label>
                   <textarea
                     className="w-full border border-gray-300 rounded-lg p-2"
                     rows={3}
                     value={actionNote}
                     onChange={(e) => setActionNote(e.target.value)}
                     placeholder="Add a reason or comment..."
                   />
                 </div>
               )}

               <div className="flex justify-end gap-3">
                 <Button
                    variant="secondary"
                    onClick={() => setShowActionModal(null)}
                    disabled={approveMutation.isPending || rejectMutation.isPending || disburseMutation.isPending}
                 >
                    Cancel
                 </Button>
                 <Button
                    variant={showActionModal === 'reject' ? 'danger' : 'primary'}
                    onClick={handleAction}
                    isLoading={approveMutation.isPending || rejectMutation.isPending || disburseMutation.isPending}
                 >
                    Confirm
                 </Button>
               </div>
            </div>
         </div>
      )}
    </div>
  );
}

export default LoanDetailsPage;
