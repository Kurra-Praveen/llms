import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  PlusIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
} from '@heroicons/react/24/outline';
import { Button, Input, Table, Pagination, StatusBadge } from '@/components/ui';
import { loanService } from '@/services/loanService';
import type { Loan } from '@/types';
import { logger } from '@/utils/logger';

const pageLogger = logger.scope('LoanListPage');

export function LoanListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);

  // Initialize filter from URL param
  const [statusFilter, setStatusFilter] = useState<string>(searchParams.get('status') || '');

  // Update URL when filter changes
  useEffect(() => {
    if (statusFilter) {
      setSearchParams({ status: statusFilter });
    } else {
      setSearchParams({});
    }
  }, [statusFilter, setSearchParams]);

  // Sync state if URL changes externally (e.g. back button)
  useEffect(() => {
    const status = searchParams.get('status');
    if (status !== null && status !== statusFilter) {
      setStatusFilter(status);
    }
  }, [searchParams]);

  const { data, isLoading } = useQuery({
    queryKey: ['loans', page, statusFilter],
    queryFn: () =>
      statusFilter
        ? loanService.getByStatus(statusFilter, page)
        : loanService.getAll({ page }),
  });

  const handleRowClick = (loan: Loan) => {
    pageLogger.debug('Row clicked', { id: loan.id });
    navigate(`/loans/${loan.id}`);
  };

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
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
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
          {/* Placeholder for search if needed later */}
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
                 <span className={`${item.totalOutstanding > 0 ? 'text-gray-900' : 'text-green-600'}`}>
                   {item.status === 'ACTIVE' || item.status === 'DISBURSED' || item.status === 'CLOSED'
                     ? `₹${item.totalOutstanding?.toLocaleString() ?? '0'}`
                     : '-'}
                 </span>
              ),
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
    </div>
  );
}

export default LoanListPage;
