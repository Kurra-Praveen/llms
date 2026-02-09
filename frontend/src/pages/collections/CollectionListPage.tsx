import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  FunnelIcon,
  ExclamationTriangleIcon,
} from '@heroicons/react/24/outline';
import { Button, Table, Pagination, FilterDialog, countActiveFilters } from '@/components/ui';
import type { FilterField } from '@/components/ui';
import { collectionService } from '@/services/collectionService';
import type { CollectionCase } from '@/types';
import { logger } from '@/utils/logger';

const pageLogger = logger.scope('CollectionListPage');

const filterFields: FilterField[] = [
  {
    key: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { value: 'OPEN', label: 'Open' },
      { value: 'IN_PROGRESS', label: 'In Progress' },
      { value: 'ESCALATED', label: 'Escalated' },
      { value: 'RESOLVED', label: 'Resolved' },
      { value: 'ON_HOLD', label: 'On Hold' },
    ],
  },
  {
    key: 'dpdBucket',
    label: 'DPD Bucket',
    type: 'select',
    options: [
      { value: '1-30', label: '1-30 Days' },
      { value: '31-60', label: '31-60 Days' },
      { value: '61-90', label: '61-90 Days' },
      { value: '90+', label: '90+ Days (NPA)' },
    ],
  },
];

const statusColors: Record<string, string> = {
  OPEN: 'bg-blue-100 text-blue-800',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
  ESCALATED: 'bg-red-100 text-red-800',
  RESOLVED: 'bg-green-100 text-green-800',
  ON_HOLD: 'bg-gray-100 text-gray-800',
};

const priorityColors: Record<string, string> = {
  LOW: 'bg-green-100 text-green-800',
  MEDIUM: 'bg-yellow-100 text-yellow-800',
  HIGH: 'bg-orange-100 text-orange-800',
  CRITICAL: 'bg-red-100 text-red-800',
};

const dpdColors: Record<string, string> = {
  'CURRENT': 'bg-green-100 text-green-800',
  '1-30': 'bg-yellow-100 text-yellow-800',
  '31-60': 'bg-orange-100 text-orange-800',
  '61-90': 'bg-red-100 text-red-800',
  '90+': 'bg-red-200 text-red-900',
};

export function CollectionListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<Record<string, any>>({});

  const { data, isLoading } = useQuery({
    queryKey: ['collections', page, filters],
    queryFn: () =>
      collectionService.getAll({
        page,
        status: filters.status,
        dpdBucket: filters.dpdBucket,
      }),
  });

  const handleRowClick = (item: CollectionCase) => {
    pageLogger.debug('Row clicked', { id: item.id });
    navigate(`/collections/${item.id}`);
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
          <h1 className="text-2xl font-bold text-gray-900">Collections</h1>
          <p className="text-gray-500 mt-1">
            Manage overdue loan collection cases
          </p>
        </div>
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
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="ESCALATED">Escalated</option>
                <option value="RESOLVED">Resolved</option>
              </select>
            </div>
          </div>
          <Button
            variant="outline"
            leftIcon={<FunnelIcon className="h-5 w-5" />}
            onClick={() => setShowFilters(true)}
          >
            Filters
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
              header: 'Case No.',
              accessorKey: 'caseNumber',
              className: 'font-medium',
            },
            {
              header: 'Borrower',
              cell: (item) => (
                <div>
                  <div className="font-medium text-gray-900">{item.borrowerName}</div>
                  <div className="text-gray-500 text-xs">{item.loanNumber}</div>
                </div>
              ),
            },
            {
              header: 'DPD',
              cell: (item) => (
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                      dpdColors[item.dpdBucket] || 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {item.dpdDays} days
                  </span>
                </div>
              ),
            },
            {
              header: 'Overdue Amount',
              cell: (item) => (
                <div>
                  <div className="font-medium text-red-600">
                    ₹{item.overdueAmount.toLocaleString()}
                  </div>
                  <div className="text-xs text-gray-500">
                    P: ₹{item.overduePrincipal.toLocaleString()}
                  </div>
                </div>
              ),
            },
            {
              header: 'Status',
              cell: (item) => (
                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    statusColors[item.status]
                  }`}
                >
                  {item.status.replace('_', ' ')}
                </span>
              ),
            },
            {
              header: 'Priority',
              cell: (item) => (
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                    priorityColors[item.priority]
                  }`}
                >
                  {item.priority === 'CRITICAL' && (
                    <ExclamationTriangleIcon className="h-3 w-3 mr-1" />
                  )}
                  {item.priority}
                </span>
              ),
            },
            {
              header: 'Assigned To',
              cell: (item) => (
                <span className="text-gray-900">
                  {item.assignedToName || '-'}
                </span>
              ),
            },
            {
              header: 'Next Action',
              cell: (item) => (
                <div className="text-sm">
                  {item.nextActionDate ? (
                    <div>
                      <div className="text-gray-900">
                        {new Date(item.nextActionDate).toLocaleDateString()}
                      </div>
                      <div className="text-xs text-gray-500 truncate max-w-[150px]">
                        {item.nextAction}
                      </div>
                    </div>
                  ) : (
                    '-'
                  )}
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
          title="Filter Collections"
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

export default CollectionListPage;
