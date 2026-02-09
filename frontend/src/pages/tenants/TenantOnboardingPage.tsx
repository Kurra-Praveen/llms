import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Input, Card } from '@/components/ui';
import { tenantService } from '@/services/tenantService';
import type { CreateTenantRequest } from '@/types';

const tenantSchema = z.object({
  businessName: z.string().min(2, 'Business name required'),
  businessCode: z.string().min(3, 'Code must be at least 3 chars'),
  contactEmail: z.string().email(),
  contactPhone: z.string().min(10, 'Phone required'),
  address: z.string().min(5, 'Address required'),
  subscriptionPlan: z.enum(['STANDARD', 'PREMIUM', 'ENTERPRISE']),
  adminFirstName: z.string().min(2, 'Admin name required'),
  adminLastName: z.string().min(2, 'Admin last name required'),
  adminEmail: z.string().email(),
  adminPassword: z.string().min(8, 'Password too short'),
});

type TenantFormData = z.infer<typeof tenantSchema>;

export function TenantOnboardingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<TenantFormData>({
    resolver: zodResolver(tenantSchema),
    defaultValues: {
        subscriptionPlan: 'STANDARD'
    }
  });

  const mutation = useMutation({
    mutationFn: (data: TenantFormData) => tenantService.create(data as CreateTenantRequest),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
      navigate('/tenants');
    },
    onError: () => {
      alert('Failed to create tenant. check logs.');
    }
  });

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Onboard New Tenant</h1>
        <p className="text-gray-500 mt-1">Create a new lender workspace.</p>
      </div>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))}>
          <Card>
              <div className="space-y-6">
                  <h3 className="text-lg font-medium text-gray-900">Business Details</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <Input label="Business Name" {...register('businessName')} error={errors.businessName?.message} required />
                      <Input label="Business Code" {...register('businessCode')} error={errors.businessCode?.message} required placeholder="e.g. ABC-LEND" />
                      <Input label="Contact Email" {...register('contactEmail')} error={errors.contactEmail?.message} required />
                      <Input label="Contact Phone" {...register('contactPhone')} error={errors.contactPhone?.message} required />
                      <Input label="Address" {...register('address')} error={errors.address?.message} required className="md:col-span-2" />
                      <div className="md:col-span-2">
                         <label className="block text-sm font-medium text-gray-700 mb-1">Subscription Plan</label>
                         <select {...register('subscriptionPlan')} className="block w-full rounded-lg border-gray-300 py-2.5 shadow-sm focus:border-primary-500 focus:ring-primary-500">
                             <option value="STANDARD">Standard</option>
                             <option value="PREMIUM">Premium</option>
                             <option value="ENTERPRISE">Enterprise</option>
                         </select>
                      </div>
                  </div>

                  <div className="border-t border-gray-200 my-4"></div>

                  <h3 className="text-lg font-medium text-gray-900">Admin Account</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <Input label="First Name" {...register('adminFirstName')} error={errors.adminFirstName?.message} required />
                      <Input label="Last Name" {...register('adminLastName')} error={errors.adminLastName?.message} required />
                      <Input label="Email" {...register('adminEmail')} error={errors.adminEmail?.message} required />
                      <Input label="Password" type="password" {...register('adminPassword')} error={errors.adminPassword?.message} required />
                  </div>
              </div>

              <div className="mt-6 flex justify-end gap-3">
                  <Button variant="secondary" onClick={() => navigate('/tenants')} type="button">Cancel</Button>
                  <Button type="submit" isLoading={isSubmitting}>Create Tenant</Button>
              </div>
          </Card>
      </form>
    </div>
  );
}
