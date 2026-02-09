import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  PlusIcon,
  FunnelIcon,
} from '@heroicons/react/24/outline';
import { Button, Table, Pagination, StatusBadge, FilterDialog, countActiveFilters } from '@/components/ui';
import type { FilterField } from '@/components/ui';
import { loanService } from '@/services/loanService';
import type { Loan } from '@/types';
import { logger } from '@/utils/logger';

const pageLogger = logger.scope('LoanListPage');

const filterFields: FilterField[] = [
  {
    key: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'DRAFT', label: 'Draft' },
      { value: 'PENDING_APPROVAL', label: 'Pending Approval' },
      { value: 'APPROVED', label: 'Approved' },
      { value: 'ACTIVE', label: 'Active (Disbursed)' },
      { value: 'CLOSED', label: 'Closed' },
      { value: 'WRITTEN_OFF', label: 'Written Off' },
      { value: 'REJECTED', label: 'Rejected' },
      { value: 'CANCELLED', label: 'Cancelled' },
    ],
  },
  {
    key: 'interestType',
    label: 'Interest Type',
    type: 'select',
    options: [
      { value: 'FLAT', label: 'Flat Rate' },
      { value: 'REDUCING_BALANCE', label: 'Reducing Balance' },
      { value: 'SIMPLE', label: 'Simple Interest' },
      { value: 'INTEREST_ONLY', label: 'Interest Only' },
      { value: 'BULLET', label: 'Bullet' },
      { value: 'DAILY_FIXED', label: 'Daily Fixed' },
    ],
  },
  {
    key: 'dpdBucket',
    label: 'DPD Bucket',
    type: 'select',
    options: [
      { value: 'CURRENT', label: 'Current (0 DPD)' },
      { value: '1-30', label: '1-30 Days' },
      { value: '31-60', label: '31-60 Days' },
      { value: '61-90', label: '61-90 Days' },
      { value: '90+', label: '90+ Days (NPA)' },
    ],
  },
  {
    key: 'disbursementDate',
    label: 'Disbursement Date',
    type: 'dateRange',
  },
  {
    key: 'amount',
    label: 'Loan Amount',
    type: 'numberRange',
  },
];

export function LoanListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);

  // Initialize filter from URL param
  const [filters, setFilters] = useState<Record<string, any>>(() => {
    const status = searchParams.get('status');
    return status ? { status } : {};
  });

  // Update URL when filter changes
  useEffect(() => {
    if (filters.status) {
      setSearchParams({ status: filters.status });
    } else {
      setSearchParams({});
    }
  }, [filters, setSearchParams]);

  // Sync state if URL changes externally (e.g. back button)
  useEffect(() => {
    const status = searchParams.get('status');
    if (status !== null && status !== filters.status) {
      setFilters({ ...filters, status });
    }
  }, [searchParams]);

  const { data, isLoading } = useQuery({
    queryKey: ['loans', page, filters],
    queryFn: () =>
      filters.status
        ? loanService.getByStatus(filters.status, page)
        : loanService.getAll({ page }),
  });

  const handleRowClick = (loan: Loan) => {
    pageLogger.debug('Row clicked', { id: loan.id });
    navigate(`/loans/${loan.id}`);
  };

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
          <h1 className="text-2xl font-bold text-gray-900">Loans</h1>
          <p className="text-gray-500 mt-1">
            Manage loan applications and active loans
          </p>
        </div>
        <Button
          onClick={() => navigate('/loans/new')}
          leftIcon={<PlusIcon className="h-5 w-5" />}
        >
          New Loan Application
        </Button>
      </div>

      <div className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
        <div className="flex flex-col sm:flex-row gap-4 items-center">
          <div className="flex-1 w-full sm:w-auto">
             <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <FunnelIcon className="h-5 w-5 text-gray-400" />
                </div>
                <select
                  value={filters.status || ''}
                  onChange={(e) => {
                    setFilters({ ...filters, status: e.target.value });
                    setPage(0);
                  }}
                  className="block w-full pl-10 pr-10 py-2.5 text-base border-gray-300 focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm rounded-lg"
                >
                  <option value="">All Statuses</option>
                  <option value="DRAFT">Draft</option>
                  <option value="PENDING_APPROVAL">Pending Approval</option>
                  <option value="APPROVED">Approved</option>
                  <option value="ACTIVE">Active (Disbursed)</option>
                  <option value="CLOSED">Closed</option>
                  <option value="WRITTEN_OFF">Written Off</option>
                </select>
             </div>
          </div>
          <Button
            variant="outline"
            leftIcon={<FunnelIcon className="h-5 w-5" />}
            onClick={() => setShowFilters(true)}
          >
            Advanced Filters
            {activeFilterCount > 1 && (
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
          onRowClick={handleRowClick}
          columns={[
            {
              header: 'Loan No.',
              accessorKey: 'loanNumber',
              className: 'font-medium',
            },
            {
              header: 'Borrower',
              cell: (item) => (
                <div>
                  <div className="font-medium text-gray-900">{item.borrowerName}</div>
                  <div className="text-gray-500 text-xs">{item.borrowerCode}</div>
                </div>
              ),
            },
            {
              header: 'Amount',
              cell: (item) => (
                <span className="font-medium">
                  ₹{item.principalAmount.toLocaleString()}
                </span>
              ),
            },
            {
              header: 'Status',
              cell: (item) => <StatusBadge status={item.status} type="loan" />,
            },
            {
              header: 'Outstanding',
              cell: (item) => (
                 <span className={`${(item.totalOutstanding ?? 0) > 0 ? 'text-gray-900' : 'text-green-600'}`}>
                   {item.status === 'ACTIVE' || item.status === 'DISBURSED' || item.status === 'CLOSED'
                     ? `₹${(item.totalOutstanding ?? 0).toLocaleString()}`
                     : '-'}
                 </span>
              ),
            },
            {
              header: 'DPD',
              cell: (item) => {
                if (!['ACTIVE', 'DISBURSED'].includes(item.status)) return '-';
                const dpd = item.dpd || 0;
                return (
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                      dpd === 0
                        ? 'bg-green-100 text-green-800'
                        : dpd <= 30
                        ? 'bg-yellow-100 text-yellow-800'
                        : dpd <= 60
                        ? 'bg-orange-100 text-orange-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {dpd} days
                  </span>
                );
              },
            },
            {
              header: 'Created',
              cell: (item) => new Date(item.applicationDate).toLocaleDateString(),
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
          title="Filter Loans"
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

export default LoanListPage;
