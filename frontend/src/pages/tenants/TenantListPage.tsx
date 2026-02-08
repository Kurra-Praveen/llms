import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  PlusIcon,
  BuildingOfficeIcon,
} from '@heroicons/react/24/outline';
import { Button, Table, Pagination, StatusBadge, Spinner } from '@/components/ui';
import { tenantService } from '@/services/tenantService';
import { logger } from '@/utils/logger';

const pageLogger = logger.scope('TenantListPage');

export function TenantListPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['tenants', page],
    queryFn: () => tenantService.getAll(page),
  });

  const suspendMutation = useMutation({
    mutationFn: tenantService.suspend,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tenants'] }),
  });

  const activateMutation = useMutation({
    mutationFn: tenantService.activate,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tenants'] }),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Tenants</h1>
          <p className="text-gray-500 mt-1">
            Manage platform tenants (Lenders)
          </p>
        </div>
        <Button
          onClick={() => navigate('/tenants/new')}
          leftIcon={<PlusIcon className="h-5 w-5" />}
        >
          Onboard Tenant
        </Button>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
        <Table
          data={data?.content || []}
          isLoading={isLoading}
          keyExtractor={(item) => item.id}
          columns={[
            {
              header: 'Business Name',
              cell: (item) => (
                <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-primary-50 flex items-center justify-center text-primary-600">
                        <BuildingOfficeIcon className="h-5 w-5" />
                    </div>
                    <div>
                        <div className="font-medium text-gray-900">{item.businessName}</div>
                        <div className="text-xs text-gray-500">{item.businessCode}</div>
                    </div>
                </div>
              ),
            },
            {
              header: 'Contact',
              cell: (item) => (
                <div>
                  <div className="text-gray-900">{item.contactEmail}</div>
                  <div className="text-gray-500 text-xs">{item.contactPhone || '-'}</div>
                </div>
              ),
            },
            {
              header: 'Plan',
              accessorKey: 'subscriptionPlan',
              cell: (item) => (
                 <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                    {item.subscriptionPlan}
                 </span>
              )
            },
            {
              header: 'Status',
              cell: (item) => <StatusBadge status={item.status} type="tenant" />,
            },
            {
              header: 'Limits',
              cell: (item) => (
                 <div className="text-xs text-gray-500">
                     <div>Users: {item.maxBorrowers}</div>
                     <div>Loans: {item.maxLoans}</div>
                 </div>
              )
            },
            {
              header: 'Joined',
              cell: (item) => new Date(item.createdAt).toLocaleDateString(),
            },
            {
               header: 'Actions',
               cell: (item) => (
                 <div className="flex gap-2">
                    {item.status === 'ACTIVE' ? (
                        <button
                          onClick={() => {
                              if(confirm('Suspend this tenant?')) suspendMutation.mutate(item.id);
                          }}
                          className="text-red-600 hover:text-red-800 text-sm font-medium"
                        >
                            Suspend
                        </button>
                    ) : (
                        <button
                          onClick={() => {
                              if(confirm('Activate this tenant?')) activateMutation.mutate(item.id);
                          }}
                          className="text-green-600 hover:text-green-800 text-sm font-medium"
                        >
                            Activate
                        </button>
                    )}
                 </div>
               )
            }
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

export default TenantListPage;
