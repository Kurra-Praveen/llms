/**
 * Sidebar Navigation Component
 */

import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  HomeIcon,
  UsersIcon,
  BanknotesIcon,
  CreditCardIcon,
  Cog6ToothIcon,
  BuildingOfficeIcon,
  ChartBarIcon,
  ClipboardDocumentListIcon,
} from '@heroicons/react/24/outline';
import { useAuth } from '@/contexts/AuthContext';
import { logger } from '@/utils/logger';

const sidebarLogger = logger.scope('Sidebar');

interface NavItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  roles?: string[];
}

const navigation: NavItem[] = [
  { name: 'Dashboard', href: '/dashboard', icon: HomeIcon },
  { name: 'Borrowers', href: '/borrowers', icon: UsersIcon },
  { name: 'Loans', href: '/loans', icon: BanknotesIcon },
  { name: 'Payments', href: '/payments', icon: CreditCardIcon },
  { name: 'Collections', href: '/collections', icon: ClipboardDocumentListIcon },
  { name: 'Reports', href: '/reports', icon: ChartBarIcon },
  {
    name: 'Tenants',
    href: '/tenants',
    icon: BuildingOfficeIcon,
    roles: ['SUPER_ADMIN'],
  },
  { name: 'Settings', href: '/settings', icon: Cog6ToothIcon },
];

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user, hasRole } = useAuth();
  const location = useLocation();

  sidebarLogger.debug('Sidebar render', {
    isOpen,
    currentPath: location.pathname,
    userRole: user?.role,
  });

  // Filter navigation items based on user role
  const filteredNavigation = navigation.filter((item) => {
    if (!item.roles) return true;
    return item.roles.some((role) => hasRole(role as any));
  });

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-gray-600 bg-opacity-75 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          fixed inset-y-0 left-0 z-50
          w-64 bg-white border-r border-gray-200
          transform transition-transform duration-300 ease-in-out
          lg:translate-x-0 lg:static lg:inset-0
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        {/* Logo */}
        <div className="h-16 flex items-center justify-center border-b border-gray-200">
          <h1 className="text-xl font-bold text-primary-600">LoanPlatform</h1>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {filteredNavigation.map((item) => {
            const isActive = location.pathname.startsWith(item.href);

            return (
              <NavLink
                key={item.name}
                to={item.href}
                onClick={() => {
                  sidebarLogger.debug('Navigation clicked', { to: item.href });
                  onClose();
                }}
                className={`
                  group flex items-center gap-3 px-3 py-2.5 rounded-lg
                  text-sm font-medium transition-colors duration-200
                  ${
                    isActive
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                  }
                `}
              >
                <item.icon
                  className={`
                    h-5 w-5 flex-shrink-0
                    ${isActive ? 'text-primary-600' : 'text-gray-400 group-hover:text-gray-500'}
                  `}
                />
                {item.name}
              </NavLink>
            );
          })}
        </nav>

        {/* User info */}
        {user && (
          <div className="p-4 border-t border-gray-200">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-primary-100 flex items-center justify-center">
                <span className="text-primary-700 font-medium">
                  {user.firstName?.charAt(0)}
                  {user.lastName?.charAt(0)}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-xs text-gray-500 truncate">{user.role}</p>
              </div>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}

export default Sidebar;
