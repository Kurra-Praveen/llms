import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  PlusIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
} from '@heroicons/react/24/outline';
import { Button, Input, Table, Pagination, StatusBadge, FilterDialog, countActiveFilters } from '@/components/ui';
import type { FilterField } from '@/components/ui';
import { paymentService } from '@/services/paymentService';

const filterFields: FilterField[] = [
  {
    key: 'paymentMethod',
    label: 'Payment Method',
    type: 'select',
    options: [
      { value: 'CASH', label: 'Cash' },
      { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
      { value: 'MOBILE_MONEY', label: 'Mobile Money' },
      { value: 'CHEQUE', label: 'Cheque' },
      { value: 'CARD', label: 'Card' },
      { value: 'WALLET', label: 'Wallet' },
      { value: 'OTHER', label: 'Other' },
    ],
  },
  {
    key: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'PENDING', label: 'Pending' },
      { value: 'COMPLETED', label: 'Completed' },
      { value: 'FAILED', label: 'Failed' },
      { value: 'REVERSED', label: 'Reversed' },
    ],
  },
  {
    key: 'paymentDate',
    label: 'Payment Date',
    type: 'dateRange',
  },
  {
    key: 'amount',
    label: 'Amount',
    type: 'numberRange',
  },
];

export function PaymentListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [loanId, setLoanId] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<Record<string, any>>({});

  const { data, isLoading } = useQuery({
    queryKey: ['payments', page, loanId, filters],
    queryFn: () =>
      loanId
        ? paymentService.getByLoanId(loanId, page)
        : paymentService.getAll({ page }),
  });

  const handleApplyFilters = (newFilters: Record<string, any>) => {
    setFilters(newFilters);
    setPage(0);
  };

  const handleClearFilters = () => {
    setFilters({});
    setPage(0);
  };

  const activeFilterCount = countActiveFilters(filters);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
          <p className="text-gray-500 mt-1">
            View and manage loan payments
          </p>
        </div>
        <Button
          onClick={() => navigate('/payments/new')}
          leftIcon={<PlusIcon className="h-5 w-5" />}
        >
          Record Payment
        </Button>
      </div>

      <div className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
             <Input
              placeholder="Search by Loan ID (Optional)..."
              value={loanId}
              onChange={(e) => {
                  setLoanId(e.target.value);
                  setPage(0);
              }}
              leftIcon={<MagnifyingGlassIcon className="h-5 w-5" />}
            />
          </div>
          <Button
            variant="outline"
            leftIcon={<FunnelIcon className="h-5 w-5" />}
            onClick={() => setShowFilters(true)}
          >
            Filters
            {activeFilterCount > 0 && (
              <span className="ml-2 bg-primary-100 text-primary-700 px-2 py-0.5 rounded-full text-xs font-medium">
                {activeFilterCount}
              </span>
            )}
          </Button>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
        <Table
          data={data?.content || []}
          isLoading={isLoading}
          keyExtractor={(item) => item.id}
          onRowClick={(item) => navigate(`/payments/${item.id}`)}
          columns={[
            {
              header: 'Payment No.',
              accessorKey: 'paymentNumber',
              className: 'font-medium',
            },
            {
              header: 'Date',
              cell: (item) => new Date(item.paymentDate).toLocaleDateString(),
            },
            {
              header: 'Loan',
              cell: (item) => (
                <div>
                    <div className="font-medium text-gray-900">{item.loanNumber || item.loanId}</div>
                    <div className="text-xs text-gray-500">{item.borrowerName}</div>
                </div>
              ),
            },
            {
              header: 'Amount',
              cell: (item) => (
                <span className="font-medium text-gray-900">
                  ₹{item.amountPaid.toLocaleString()}
                </span>
              ),
            },
            {
              header: 'Method',
              cell: (item) => item.paymentMethod.replace('_', ' '),
            },
            {
              header: 'Status',
              cell: (item) => <StatusBadge status={item.status} type="payment" />,
            },
             {
              header: 'Breakdown',
              cell: (item) => (
                <div className="text-xs text-gray-500">
                   <div>P: {item.principalPaid}</div>
                   <div>I: {item.interestPaid}</div>
                   {item.penaltyPaid > 0 && <div>Pen: {item.penaltyPaid}</div>}
                </div>
              ),
            },
          ]}
        />
        {data && (
          <Pagination
            currentPage={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={data.size}
            onPageChange={setPage}
          />
        )}
      </div>

      {showFilters && (
        <FilterDialog
          title="Filter Payments"
          fields={filterFields}
          values={filters}
          onApply={handleApplyFilters}
          onClose={() => setShowFilters(false)}
          onClear={handleClearFilters}
        />
      )}
    </div>
  );
}

export default PaymentListPage;
