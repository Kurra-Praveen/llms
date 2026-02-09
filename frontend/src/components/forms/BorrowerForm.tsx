import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Input, Card } from '@/components/ui';
import { borrowerSchema, type BorrowerFormData } from '@/lib/validations/borrower';
import { borrowerService } from '@/services/borrowerService';
import { logger } from '@/utils/logger';
import type { Borrower } from '@/types';

const formLogger = logger.scope('BorrowerForm');

interface BorrowerFormProps {
  initialData?: Borrower;
  isEdit?: boolean;
}

export function BorrowerForm({ initialData, isEdit = false }: BorrowerFormProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<BorrowerFormData>({
    resolver: zodResolver(borrowerSchema) as any,
    defaultValues: {
      fullName: initialData?.fullName || '',
      email: initialData?.email || '',
      phone: initialData?.phone || '',
      alternatePhone: initialData?.alternatePhone || '',
      dateOfBirth: initialData?.dateOfBirth || '',
      gender: initialData?.gender,
      idType: initialData?.idType,
      idNumber: initialData?.idNumber || '',
      addressLine1: initialData?.addressLine1 || '',
      addressLine2: initialData?.addressLine2 || '',
      city: initialData?.city || '',
      state: initialData?.state || '',
      postalCode: initialData?.postalCode || '',
      country: initialData?.country || '',
      monthlyIncome: initialData?.monthlyIncome,
      occupation: initialData?.occupation || '',
      employerName: initialData?.employerName || '',
      notes: initialData?.notes || '',
    },
  });

  const mutation = useMutation({
    mutationFn: (data: BorrowerFormData) => {
      if (isEdit && initialData) {
        return borrowerService.update(initialData.id, data);
      }
      return borrowerService.create(data);
    },
    onSuccess: (data) => {
      formLogger.info(isEdit ? 'Borrower updated' : 'Borrower created', { id: data.id });
      queryClient.invalidateQueries({ queryKey: ['borrowers'] });
      if (isEdit) {
        queryClient.invalidateQueries({ queryKey: ['borrower', data.id] });
      }
      navigate('/borrowers');
    },
    onError: (error) => {
      formLogger.error('Failed to save borrower', { error });
      // TODO: Show toast notification
    },
  });

  const onSubmit = (data: BorrowerFormData) => {
    formLogger.debug('Form submitted', { data });
    mutation.mutate(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <Card>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="col-span-full">
            <h3 className="text-lg font-medium text-gray-900">Personal Information</h3>
            <p className="mt-1 text-sm text-gray-500">Basic details about the borrower.</p>
          </div>

          <Input
            label="Full Name"
            {...register('fullName')}
            error={errors.fullName?.message}
            placeholder="John Doe"
            required
          />

          <Input
            label="Date of Birth"
            type="date"
            {...register('dateOfBirth')}
            error={errors.dateOfBirth?.message}
          />

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">Gender</label>
            <select
              {...register('gender')}
              className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            >
              <option value="">Select Gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
            {errors.gender?.message && (
              <p className="text-sm text-red-600">{errors.gender.message}</p>
            )}
          </div>

          <div className="col-span-full border-t border-gray-200 my-4" />

          <div className="col-span-full">
            <h3 className="text-lg font-medium text-gray-900">Contact Details</h3>
          </div>

          <Input
            label="Email"
            type="email"
            {...register('email')}
            error={errors.email?.message}
            placeholder="john@example.com"
          />

          <Input
            label="Phone Number"
            type="tel"
            {...register('phone')}
            error={errors.phone?.message}
            placeholder="+1234567890"
            required
          />

          <Input
            label="Alternate Phone"
            type="tel"
            {...register('alternatePhone')}
            error={errors.alternatePhone?.message}
          />

          <div className="col-span-full border-t border-gray-200 my-4" />

          <div className="col-span-full">
            <h3 className="text-lg font-medium text-gray-900">Identity & Address</h3>
          </div>

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">ID Type</label>
            <select
              {...register('idType')}
              className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
            >
              <option value="">Select ID Type</option>
              <option value="NATIONAL_ID">National ID</option>
              <option value="PASSPORT">Passport</option>
              <option value="DRIVING_LICENSE">Driving License</option>
              <option value="VOTER_ID">Voter ID</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <Input
            label="ID Number"
            {...register('idNumber')}
            error={errors.idNumber?.message}
          />

          <Input
            label="Address Line 1"
            {...register('addressLine1')}
            error={errors.addressLine1?.message}
            className="col-span-full"
          />

          <Input
            label="Address Line 2"
            {...register('addressLine2')}
            error={errors.addressLine2?.message}
            className="col-span-full"
          />

          <Input
            label="City"
            {...register('city')}
            error={errors.city?.message}
          />

          <Input
            label="State"
            {...register('state')}
            error={errors.state?.message}
          />

          <Input
            label="Postal Code"
            {...register('postalCode')}
            error={errors.postalCode?.message}
          />

          <Input
            label="Country"
            {...register('country')}
            error={errors.country?.message}
          />

          <div className="col-span-full border-t border-gray-200 my-4" />

          <div className="col-span-full">
            <h3 className="text-lg font-medium text-gray-900">Employment</h3>
          </div>

          <Input
            label="Occupation"
            {...register('occupation')}
            error={errors.occupation?.message}
          />

          <Input
            label="Employer Name"
            {...register('employerName')}
            error={errors.employerName?.message}
          />

          <Input
            label="Monthly Income"
            type="number"
            {...register('monthlyIncome')}
            error={errors.monthlyIncome?.message}
          />

          <div className="col-span-full">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Notes
            </label>
            <textarea
              {...register('notes')}
              rows={4}
              className="block w-full rounded-lg border border-gray-300 py-2.5 px-4 shadow-sm focus:border-primary-500 focus:ring-primary-500"
              placeholder="Additional notes about the borrower..."
            />
          </div>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/borrowers')}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button type="submit" isLoading={isSubmitting}>
            {isEdit ? 'Update Borrower' : 'Create Borrower'}
          </Button>
        </div>
      </Card>
    </form>
  );
}
