import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  PlusIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
} from '@heroicons/react/24/outline';
import { Button, Input, Table, Pagination, StatusBadge, FilterDialog, countActiveFilters } from '@/components/ui';
import type { FilterField } from '@/components/ui';
import { borrowerService } from '@/services/borrowerService';
import type { Borrower } from '@/types';
import { logger } from '@/utils/logger';

const pageLogger = logger.scope('BorrowerListPage');

const filterFields: FilterField[] = [
  {
    key: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'ACTIVE', label: 'Active' },
      { value: 'INACTIVE', label: 'Inactive' },
      { value: 'BLOCKED', label: 'Blocked' },
      { value: 'BLACKLISTED', label: 'Blacklisted' },
    ],
  },
  {
    key: 'riskBand',
    label: 'Risk Band',
    type: 'select',
    options: [
      { value: 'LOW', label: 'Low Risk' },
      { value: 'MEDIUM', label: 'Medium Risk' },
      { value: 'HIGH', label: 'High Risk' },
      { value: 'VERY_HIGH', label: 'Very High Risk' },
      { value: 'UNRATED', label: 'Unrated' },
    ],
  },
  {
    key: 'createdDate',
    label: 'Created Date',
    type: 'dateRange',
  },
];

export function BorrowerListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<Record<string, any>>({});

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0); // Reset to first page on search
    }, 500);
    return () => clearTimeout(timer);
  }, [search]);

  const { data, isLoading } = useQuery({
    queryKey: ['borrowers', page, debouncedSearch, filters],
    queryFn: () => {
      if (debouncedSearch) {
        return borrowerService.search(debouncedSearch, page);
      }
      // Apply filters if set
      if (filters.status) {
        return borrowerService.getByStatus(filters.status, page);
      }
      return borrowerService.getAll({ page });
    },
  });

  const handleRowClick = (borrower: Borrower) => {
    pageLogger.debug('Row clicked', { id: borrower.id });
    navigate(`/borrowers/${borrower.id}`);
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

      {showFilters && (
        <FilterDialog
          title="Filter Borrowers"
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

export default BorrowerListPage;
