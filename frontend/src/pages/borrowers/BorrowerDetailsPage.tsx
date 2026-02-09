import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowLeftIcon,
  PencilIcon,
  NoSymbolIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline';
import { Button, Card, CardHeader, StatusBadge, Spinner, Badge } from '@/components/ui';
import { borrowerService } from '@/services/borrowerService';

export function BorrowerDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: borrower, isLoading } = useQuery({
    queryKey: ['borrower', id],
    queryFn: () => borrowerService.getById(id!),
    enabled: !!id,
  });

  if (isLoading) {
    return <Spinner fullScreen text="Loading borrower details..." />;
  }

  if (!borrower) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-bold text-gray-900">Error</h2>
        <p className="text-gray-500 mt-2">Failed to load borrower details</p>
        <Button
          variant="secondary"
          className="mt-4"
          onClick={() => navigate('/borrowers')}
        >
          Back to List
        </Button>
      </div>
    );
  }

  const handleEdit = () => {
    navigate(`/borrowers/${id}/edit`);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/borrowers')}
            className="p-2"
          >
            <ArrowLeftIcon className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{borrower.fullName}</h1>
            <div className="flex items-center gap-2 mt-1">
              <span className="text-gray-500 text-sm">{borrower.borrowerCode}</span>
              <StatusBadge status={borrower.status} type="borrower" />
            </div>
          </div>
        </div>
        <div className="flex gap-3">
          <Button
            variant="outline"
            leftIcon={<PencilIcon className="h-4 w-4" />}
            onClick={handleEdit}
          >
            Edit
          </Button>
          {borrower.status === 'ACTIVE' ? (
            <Button
              variant="danger"
              leftIcon={<NoSymbolIcon className="h-4 w-4" />}
              onClick={() => {
                if (confirm('Are you sure you want to block this borrower?')) {
                  borrowerService.block(borrower.id).then(() => {
                    // Refresh query
                    window.location.reload();
                  });
                }
              }}
            >
              Block
            </Button>
          ) : (
             <Button
              variant="secondary"
              leftIcon={<CheckCircleIcon className="h-4 w-4 text-green-600" />}
              onClick={() => {
                if (confirm('Are you sure you want to activate this borrower?')) {
                  borrowerService.activate(borrower.id).then(() => {
                    window.location.reload();
                  });
                }
              }}
            >
              Activate
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader title="Personal Information" />
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-6">
              <div>
                <dt className="text-sm font-medium text-gray-500">Full Name</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.fullName}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Gender</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.gender || '-'}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Date of Birth</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.dateOfBirth || '-'}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">ID Type / Number</dt>
                <dd className="mt-1 text-sm text-gray-900">
                  {borrower.idType ? `${borrower.idType} - ${borrower.idNumber}` : '-'}
                </dd>
              </div>
            </dl>
          </Card>

          <Card>
            <CardHeader title="Contact & Address" />
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-6">
              <div>
                <dt className="text-sm font-medium text-gray-500">Phone</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.phone}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Email</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.email || '-'}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-sm font-medium text-gray-500">Address</dt>
                <dd className="mt-1 text-sm text-gray-900">
                  {[
                    borrower.addressLine1,
                    borrower.addressLine2,
                    borrower.city,
                    borrower.state,
                    borrower.postalCode,
                    borrower.country,
                  ]
                    .filter(Boolean)
                    .join(', ') || '-'}
                </dd>
              </div>
            </dl>
          </Card>

          <Card>
            <CardHeader title="Employment" />
             <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-6">
              <div>
                <dt className="text-sm font-medium text-gray-500">Occupation</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.occupation || '-'}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Employer</dt>
                <dd className="mt-1 text-sm text-gray-900">{borrower.employerName || '-'}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">Monthly Income</dt>
                <dd className="mt-1 text-sm text-gray-900">
                  {borrower.monthlyIncome ? `₹${borrower.monthlyIncome.toLocaleString()}` : '-'}
                </dd>
              </div>
             </dl>
          </Card>
        </div>

        {/* Sidebar Info */}
        <div className="space-y-6">
          <Card>
            <CardHeader title="Risk Profile" />
            <div className="space-y-4">
              <div>
                <div className="text-sm font-medium text-gray-500 mb-1">Risk Band</div>
                <Badge
                  variant={
                    borrower.riskBand === 'LOW' ? 'success' :
                    borrower.riskBand === 'MEDIUM' ? 'warning' :
                    borrower.riskBand === 'HIGH' ? 'danger' : 'gray'
                  }
                >
                  {borrower.riskBand || 'UNRATED'}
                </Badge>
              </div>
              <div>
                <div className="text-sm font-medium text-gray-500 mb-1">Risk Score</div>
                <div className="text-2xl font-bold text-gray-900">{borrower.riskScore || '-'}</div>
              </div>
              <div>
                <div className="text-sm font-medium text-gray-500 mb-1">Credit Rating</div>
                <div className="text-lg font-medium text-gray-900">{borrower.creditRating || '-'}</div>
              </div>
            </div>
          </Card>

          <Card>
             <CardHeader title="Loan Summary" />
             <div className="space-y-4">
               <div className="flex justify-between items-center">
                 <span className="text-sm text-gray-500">Active Loans</span>
                 <span className="text-sm font-medium text-gray-900">{borrower.activeLoans || 0}</span>
               </div>
               <div className="flex justify-between items-center">
                 <span className="text-sm text-gray-500">Total Loans</span>
                 <span className="text-sm font-medium text-gray-900">{borrower.totalLoans || 0}</span>
               </div>
               <div className="border-t border-gray-100 my-2"></div>
               <div>
                 <div className="text-sm font-medium text-gray-500 mb-1">Total Outstanding</div>
                 <div className="text-xl font-bold text-primary-600">
                   ₹{(borrower.totalOutstanding || 0).toLocaleString()}
                 </div>
               </div>
             </div>
          </Card>

           {borrower.notes && (
            <Card>
              <CardHeader title="Notes" />
              <p className="text-sm text-gray-600 whitespace-pre-wrap">{borrower.notes}</p>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

export default BorrowerDetailsPage;
