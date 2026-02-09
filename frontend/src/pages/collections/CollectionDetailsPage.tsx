import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeftIcon,
  PhoneIcon,
  ChatBubbleLeftIcon,
  ArrowUpIcon,
  CheckCircleIcon,
  ClockIcon,
} from '@heroicons/react/24/outline';
import { Button, Card, CardHeader, Spinner } from '@/components/ui';
import { collectionService } from '@/services/collectionService';
import { useToast } from '@/contexts/ToastContext';
import type { ActivityType, ContactMethod, CreateCollectionActivityRequest } from '@/types';

const statusColors: Record<string, string> = {
  OPEN: 'bg-blue-100 text-blue-800 border-blue-200',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  ESCALATED: 'bg-red-100 text-red-800 border-red-200',
  RESOLVED: 'bg-green-100 text-green-800 border-green-200',
  ON_HOLD: 'bg-gray-100 text-gray-800 border-gray-200',
};

const priorityColors: Record<string, string> = {
  LOW: 'bg-green-50 text-green-700 border-green-200',
  MEDIUM: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  HIGH: 'bg-orange-50 text-orange-700 border-orange-200',
  CRITICAL: 'bg-red-50 text-red-700 border-red-200',
};

const activityIcons: Record<string, React.ElementType> = {
  CALL_ATTEMPTED: PhoneIcon,
  CALL_CONNECTED: PhoneIcon,
  SMS_SENT: ChatBubbleLeftIcon,
  EMAIL_SENT: ChatBubbleLeftIcon,
  STATUS_CHANGE: ArrowUpIcon,
  NOTE_ADDED: ChatBubbleLeftIcon,
  PTP_RECORDED: ClockIcon,
  PAYMENT_RECEIVED: CheckCircleIcon,
};

export function CollectionDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [showActivityForm, setShowActivityForm] = useState(false);
  const [activityForm, setActivityForm] = useState<CreateCollectionActivityRequest>({
    activityType: 'CALL_ATTEMPTED' as ActivityType,
    notes: '',
  });

  const { data: collectionCase, isLoading } = useQuery({
    queryKey: ['collection', id],
    queryFn: () => collectionService.getById(id!),
    enabled: !!id,
  });

  const addActivityMutation = useMutation({
    mutationFn: (data: CreateCollectionActivityRequest) => collectionService.addActivity(id!, data),
    onSuccess: () => {
      showToast('Activity logged successfully', 'success');
      queryClient.invalidateQueries({ queryKey: ['collection', id] });
      setShowActivityForm(false);
      setActivityForm({ activityType: 'CALL_ATTEMPTED' as ActivityType, notes: '' });
    },
    onError: () => {
      showToast('Failed to log activity', 'error');
    },
  });

  const escalateMutation = useMutation({
    mutationFn: () => collectionService.escalate(id!, 'Escalated by agent'),
    onSuccess: () => {
      showToast('Case escalated successfully', 'success');
      queryClient.invalidateQueries({ queryKey: ['collection', id] });
    },
    onError: () => {
      showToast('Failed to escalate case', 'error');
    },
  });

  const resolveMutation = useMutation({
    mutationFn: () => collectionService.resolve(id!, 'PAYMENT_RECEIVED', 'Customer made payment'),
    onSuccess: () => {
      showToast('Case resolved successfully', 'success');
      queryClient.invalidateQueries({ queryKey: ['collection', id] });
    },
    onError: () => {
      showToast('Failed to resolve case', 'error');
    },
  });

  const handleSubmitActivity = (e: React.FormEvent) => {
    e.preventDefault();
    addActivityMutation.mutate(activityForm);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner />
      </div>
    );
  }

  if (!collectionCase) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-semibold text-gray-900">Case Not Found</h2>
        <Button onClick={() => navigate('/collections')} className="mt-4">
          Back to Collections
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
            onClick={() => navigate('/collections')}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <ArrowLeftIcon className="h-5 w-5 text-gray-600" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {collectionCase.caseNumber}
            </h1>
            <p className="text-gray-500 mt-1">
              {collectionCase.borrowerName} - {collectionCase.loanNumber}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span
            className={`px-3 py-1 rounded-full text-sm font-medium border ${
              statusColors[collectionCase.status]
            }`}
          >
            {collectionCase.status.replace('_', ' ')}
          </span>
          <span
            className={`px-3 py-1 rounded-full text-sm font-medium border ${
              priorityColors[collectionCase.priority]
            }`}
          >
            {collectionCase.priority}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Overdue Summary */}
          <Card>
            <CardHeader title="Overdue Summary" />
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center p-4 bg-red-50 rounded-lg">
                <p className="text-2xl font-bold text-red-600">
                  ₹{collectionCase.overdueAmount.toLocaleString()}
                </p>
                <p className="text-sm text-red-700">Total Overdue</p>
              </div>
              <div className="text-center p-4 bg-blue-50 rounded-lg">
                <p className="text-2xl font-bold text-blue-600">
                  ₹{collectionCase.overduePrincipal.toLocaleString()}
                </p>
                <p className="text-sm text-blue-700">Principal</p>
              </div>
              <div className="text-center p-4 bg-green-50 rounded-lg">
                <p className="text-2xl font-bold text-green-600">
                  ₹{collectionCase.overdueInterest.toLocaleString()}
                </p>
                <p className="text-sm text-green-700">Interest</p>
              </div>
              <div className="text-center p-4 bg-orange-50 rounded-lg">
                <p className="text-2xl font-bold text-orange-600">
                  ₹{collectionCase.overduePenalty.toLocaleString()}
                </p>
                <p className="text-sm text-orange-700">Penalty</p>
              </div>
            </div>
          </Card>

          {/* Activity Timeline */}
          <Card>
            <div className="flex items-center justify-between mb-4">
              <CardHeader title="Activity Timeline" />
              <Button
                size="sm"
                onClick={() => setShowActivityForm(!showActivityForm)}
              >
                Log Activity
              </Button>
            </div>

            {showActivityForm && (
              <form onSubmit={handleSubmitActivity} className="mb-6 p-4 bg-gray-50 rounded-lg">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Activity Type
                    </label>
                    <select
                      value={activityForm.activityType}
                      onChange={(e) =>
                        setActivityForm({ ...activityForm, activityType: e.target.value as ActivityType })
                      }
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    >
                      <option value="CALL_ATTEMPTED">Call Attempted</option>
                      <option value="CALL_CONNECTED">Call Connected</option>
                      <option value="SMS_SENT">SMS Sent</option>
                      <option value="EMAIL_SENT">Email Sent</option>
                      <option value="FIELD_VISIT">Field Visit</option>
                      <option value="PTP_RECORDED">Promise to Pay</option>
                      <option value="NOTE_ADDED">Note</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Contact Method
                    </label>
                    <select
                      value={activityForm.contactMethod || ''}
                      onChange={(e) =>
                        setActivityForm({ ...activityForm, contactMethod: e.target.value as ContactMethod })
                      }
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    >
                      <option value="">Select method</option>
                      <option value="PHONE">Phone</option>
                      <option value="SMS">SMS</option>
                      <option value="EMAIL">Email</option>
                      <option value="WHATSAPP">WhatsApp</option>
                      <option value="FIELD_VISIT">Field Visit</option>
                    </select>
                  </div>
                </div>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Notes
                  </label>
                  <textarea
                    value={activityForm.notes || ''}
                    onChange={(e) => setActivityForm({ ...activityForm, notes: e.target.value })}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    placeholder="Add notes about this activity..."
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <Button
                    variant="outline"
                    type="button"
                    onClick={() => setShowActivityForm(false)}
                  >
                    Cancel
                  </Button>
                  <Button type="submit" isLoading={addActivityMutation.isPending}>
                    Save Activity
                  </Button>
                </div>
              </form>
            )}

            <div className="space-y-4">
              {collectionCase.activities && collectionCase.activities.length > 0 ? (
                collectionCase.activities.map((activity) => {
                  const Icon = activityIcons[activity.activityType] || ChatBubbleLeftIcon;
                  return (
                    <div key={activity.id} className="flex gap-4">
                      <div className="flex-shrink-0">
                        <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                          <Icon className="h-5 w-5 text-primary-600" />
                        </div>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <p className="text-sm font-medium text-gray-900">
                            {activity.activityType.replace(/_/g, ' ')}
                          </p>
                          <p className="text-xs text-gray-500">
                            {new Date(activity.activityDate).toLocaleString()}
                          </p>
                        </div>
                        {activity.notes && (
                          <p className="text-sm text-gray-600 mt-1">{activity.notes}</p>
                        )}
                        {activity.contactMethod && (
                          <p className="text-xs text-gray-500 mt-1">
                            via {activity.contactMethod}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <p>No activities logged yet.</p>
                </div>
              )}
            </div>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Quick Actions */}
          <Card>
            <CardHeader title="Actions" />
            <div className="space-y-3">
              {collectionCase.status !== 'RESOLVED' && (
                <>
                  <Button
                    variant="outline"
                    className="w-full"
                    leftIcon={<PhoneIcon className="h-4 w-4" />}
                    onClick={() => {
                      setActivityForm({ activityType: 'CALL_ATTEMPTED' as ActivityType, notes: '' });
                      setShowActivityForm(true);
                    }}
                  >
                    Log Call
                  </Button>
                  {collectionCase.status !== 'ESCALATED' && (
                    <Button
                      variant="outline"
                      className="w-full text-orange-600 border-orange-200 hover:bg-orange-50"
                      leftIcon={<ArrowUpIcon className="h-4 w-4" />}
                      onClick={() => escalateMutation.mutate()}
                      isLoading={escalateMutation.isPending}
                    >
                      Escalate Case
                    </Button>
                  )}
                  <Button
                    className="w-full bg-green-600 hover:bg-green-700"
                    leftIcon={<CheckCircleIcon className="h-4 w-4" />}
                    onClick={() => resolveMutation.mutate()}
                    isLoading={resolveMutation.isPending}
                  >
                    Mark Resolved
                  </Button>
                </>
              )}
            </div>
          </Card>

          {/* Case Details */}
          <Card>
            <CardHeader title="Case Details" />
            <div className="space-y-3">
              <div>
                <label className="text-sm text-gray-500">DPD Days</label>
                <p className="font-medium text-gray-900">{collectionCase.dpdDays} days</p>
              </div>
              <div>
                <label className="text-sm text-gray-500">DPD Bucket</label>
                <p className="font-medium text-gray-900">{collectionCase.dpdBucket}</p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Assigned To</label>
                <p className="font-medium text-gray-900">
                  {collectionCase.assignedToName || 'Unassigned'}
                </p>
              </div>
              {collectionCase.lastContactDate && (
                <div>
                  <label className="text-sm text-gray-500">Last Contact</label>
                  <p className="font-medium text-gray-900">
                    {new Date(collectionCase.lastContactDate).toLocaleDateString()}
                  </p>
                </div>
              )}
              {collectionCase.nextActionDate && (
                <div>
                  <label className="text-sm text-gray-500">Next Action</label>
                  <p className="font-medium text-gray-900">
                    {new Date(collectionCase.nextActionDate).toLocaleDateString()}
                  </p>
                  <p className="text-sm text-gray-500">{collectionCase.nextAction}</p>
                </div>
              )}
            </div>
          </Card>

          {/* Borrower Info */}
          <Card>
            <CardHeader title="Borrower Info" />
            <div className="space-y-3">
              <div>
                <label className="text-sm text-gray-500">Name</label>
                <p className="font-medium text-gray-900">{collectionCase.borrowerName}</p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Phone</label>
                <p className="font-medium text-gray-900">
                  <a href={`tel:${collectionCase.borrowerPhone}`} className="text-primary-600 hover:underline">
                    {collectionCase.borrowerPhone}
                  </a>
                </p>
              </div>
              <div>
                <label className="text-sm text-gray-500">Loan</label>
                <p className="font-medium text-gray-900">
                  <button
                    onClick={() => navigate(`/loans/${collectionCase.loanId}`)}
                    className="text-primary-600 hover:underline"
                  >
                    {collectionCase.loanNumber}
                  </button>
                </p>
              </div>
            </div>
          </Card>

          {/* PTP Info */}
          {collectionCase.ptpDate && (
            <Card className="bg-yellow-50 border-yellow-200">
              <CardHeader title="Promise to Pay" />
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-yellow-700">Date</span>
                  <span className="font-medium text-yellow-900">
                    {new Date(collectionCase.ptpDate).toLocaleDateString()}
                  </span>
                </div>
                {collectionCase.ptpAmount && (
                  <div className="flex justify-between">
                    <span className="text-yellow-700">Amount</span>
                    <span className="font-medium text-yellow-900">
                      ₹{collectionCase.ptpAmount.toLocaleString()}
                    </span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-yellow-700">Status</span>
                  <span className="font-medium text-yellow-900">
                    {collectionCase.ptpStatus || 'PENDING'}
                  </span>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

export default CollectionDetailsPage;
