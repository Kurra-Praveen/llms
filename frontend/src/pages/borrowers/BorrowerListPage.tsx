import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  PlusIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
} from '@heroicons/react/24/outline';
import { Button, Input, Table, Pagination, StatusBadge } from '@/components/ui';
import { borrowerService } from '@/services/borrowerService';
import type { Borrower } from '@/types';
import { logger } from '@/utils/logger';

const pageLogger = logger.scope('BorrowerListPage');

export function BorrowerListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Debounce search
  React.useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0); // Reset to first page on search
    }, 500);
    return () => clearTimeout(timer);
  }, [search]);

  const { data, isLoading, error } = useQuery({
    queryKey: ['borrowers', page, debouncedSearch],
    queryFn: () =>
      debouncedSearch
        ? borrowerService.search(debouncedSearch, page)
        : borrowerService.getAll({ page }),
  });

  const handleRowClick = (borrower: Borrower) => {
    pageLogger.debug('Row clicked', { id: borrower.id });
    navigate(`/borrowers/${borrower.id}`);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Borrowers</h1>
          <p className="text-gray-500 mt-1">
            Manage your borrowers and their loan history
          </p>
        </div>
        <Button
          onClick={() => navigate('/borrowers/new')}
          leftIcon={<PlusIcon className="h-5 w-5" />}
        >
          Add Borrower
        </Button>
      </div>

      <div className="bg-white p-4 rounded-lg border border-gray-200 shadow-sm">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <Input
              placeholder="Search by name, email, or phone..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              leftIcon={<MagnifyingGlassIcon className="h-5 w-5" />}
            />
          </div>
          <Button variant="outline" leftIcon={<FunnelIcon className="h-5 w-5" />}>
            Filters
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
              header: 'Name',
              cell: (item) => (
                <div>
                  <div className="font-medium text-gray-900">{item.fullName}</div>
                  <div className="text-gray-500 text-xs">{item.borrowerCode}</div>
                </div>
              ),
            },
            {
              header: 'Contact',
              cell: (item) => (
                <div>
                  <div className="text-gray-900">{item.phone}</div>
                  <div className="text-gray-500 text-xs">{item.email || '-'}</div>
                </div>
              ),
            },
            {
              header: 'Status',
              cell: (item) => <StatusBadge status={item.status} type="borrower" />,
            },
            {
              header: 'Risk Band',
              cell: (item) => (
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                    item.riskBand === 'LOW'
                      ? 'bg-green-100 text-green-800'
                      : item.riskBand === 'MEDIUM'
                      ? 'bg-yellow-100 text-yellow-800'
                      : item.riskBand === 'HIGH'
                      ? 'bg-red-100 text-red-800'
                      : 'bg-gray-100 text-gray-800'
                  }`}
                >
                  {item.riskBand || 'UNRATED'}
                </span>
              ),
            },
            {
              header: 'Active Loans',
              accessorKey: 'activeLoans',
              cell: (item) => (
                 <span className="font-medium">{item.activeLoans || 0}</span>
              ),
            },
            {
              header: 'Created',
              cell: (item) => new Date(item.createdAt).toLocaleDateString(),
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

export default BorrowerListPage;
