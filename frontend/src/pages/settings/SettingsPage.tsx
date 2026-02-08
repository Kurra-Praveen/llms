import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/contexts/AuthContext';
import { Button, Input, Card, CardHeader } from '@/components/ui';
import { logger } from '@/utils/logger';

const settingsLogger = logger.scope('SettingsPage');

const profileSchema = z.object({
  firstName: z.string().min(2, 'First name is required'),
  lastName: z.string().min(2, 'Last name is required'),
  email: z.string().email().readonly(),
  phone: z.string().optional(),
});

type ProfileFormData = z.infer<typeof profileSchema>;

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

type PasswordFormData = z.infer<typeof passwordSchema>;

export function SettingsPage() {
  const { user } = useAuth();

  const {
    register: registerProfile,
    handleSubmit: handleSubmitProfile,
    formState: { errors: profileErrors, isSubmitting: isProfileSubmitting },
  } = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
      phone: user?.phone || '',
    },
  });

  const {
    register: registerPassword,
    handleSubmit: handleSubmitPassword,
    formState: { errors: passwordErrors, isSubmitting: isPasswordSubmitting },
    reset: resetPassword,
  } = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
  });

  const onProfileSubmit = async (data: ProfileFormData) => {
    settingsLogger.info('Updating profile', data);
    // TODO: Implement API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    alert('Profile updated successfully (Mock)');
  };

  const onPasswordSubmit = async (data: PasswordFormData) => {
    settingsLogger.info('Updating password');
    // TODO: Implement API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    alert('Password updated successfully (Mock)');
    resetPassword();
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-500 mt-1">Manage your account and preferences</p>
      </div>

      {/* Profile Section */}
      <Card>
        <CardHeader title="Profile Information" />
        <form onSubmit={handleSubmitProfile(onProfileSubmit)} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="First Name"
              {...registerProfile('firstName')}
              error={profileErrors.firstName?.message}
            />
            <Input
              label="Last Name"
              {...registerProfile('lastName')}
              error={profileErrors.lastName?.message}
            />
            <Input
              label="Email Address"
              {...registerProfile('email')}
              error={profileErrors.email?.message}
              disabled
              className="bg-gray-50"
            />
            <Input
              label="Phone Number"
              {...registerProfile('phone')}
              error={profileErrors.phone?.message}
            />
          </div>
          <div className="flex justify-end">
            <Button type="submit" isLoading={isProfileSubmitting}>
              Save Profile
            </Button>
          </div>
        </form>
      </Card>

      {/* Security Section */}
      <Card>
        <CardHeader title="Security" subtitle="Update your password" />
        <form onSubmit={handleSubmitPassword(onPasswordSubmit)} className="space-y-4 max-w-md">
          <Input
            label="Current Password"
            type="password"
            {...registerPassword('currentPassword')}
            error={passwordErrors.currentPassword?.message}
          />
          <Input
            label="New Password"
            type="password"
            {...registerPassword('newPassword')}
            error={passwordErrors.newPassword?.message}
          />
          <Input
            label="Confirm New Password"
            type="password"
            {...registerPassword('confirmPassword')}
            error={passwordErrors.confirmPassword?.message}
          />
          <div className="flex justify-end">
            <Button variant="secondary" type="submit" isLoading={isPasswordSubmitting}>
              Change Password
            </Button>
          </div>
        </form>
      </Card>

      <Card>
        <CardHeader title="Application Info" />
        <div className="text-sm text-gray-500">
          <p>Version: 1.0.0</p>
          <p>Environment: {import.meta.env.MODE}</p>
          {user?.tenantId && <p>Tenant ID: {user.tenantId}</p>}
        </div>
      </Card>
    </div>
  );
}

export default SettingsPage;
