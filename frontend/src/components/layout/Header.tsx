/**
 * Header Component
 */

import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react';
import {
  Bars3Icon,
  BellIcon,
  UserCircleIcon,
  ArrowRightOnRectangleIcon,
  Cog6ToothIcon,
} from '@heroicons/react/24/outline';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { notificationService } from '@/services';
import { logger } from '@/utils/logger';

const headerLogger = logger.scope('Header');

interface HeaderProps {
  onMenuClick: () => void;
}

export function Header({ onMenuClick }: HeaderProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Poll unread notification count every 30s for the badge
  const { data: unreadCount } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationService.getUnreadCount(),
    refetchInterval: 30_000,
    retry: false,
  });

  const handleLogout = async () => {
    headerLogger.info('Logout clicked');
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      headerLogger.error('Logout failed', { error });
    }
  };

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 lg:px-6">
      {/* Left side */}
      <div className="flex items-center gap-4">
        {/* Mobile menu button */}
        <button
          onClick={onMenuClick}
          className="lg:hidden p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg"
        >
          <Bars3Icon className="h-6 w-6" />
        </button>

        {/* Breadcrumb or page title could go here */}
      </div>

      {/* Right side */}
      <div className="flex items-center gap-2">
        {/* Notifications */}
        <button
          onClick={() => navigate('/notifications')}
          className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg relative"
          aria-label="Notifications"
        >
          <BellIcon className="h-6 w-6" />
          {(unreadCount ?? 0) > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-[1.25rem] h-5 px-1 bg-red-500 text-white text-xs font-medium rounded-full flex items-center justify-center">
              {unreadCount! > 99 ? '99+' : unreadCount}
            </span>
          )}
        </button>

        {/* User menu */}
        <Menu as="div" className="relative">
          <MenuButton className="flex items-center gap-2 p-2 text-gray-700 hover:bg-gray-100 rounded-lg">
            <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center">
              <span className="text-primary-700 font-medium text-sm">
                {user?.firstName?.charAt(0)}
                {user?.lastName?.charAt(0)}
              </span>
            </div>
            <span className="hidden md:block text-sm font-medium">
              {user?.firstName} {user?.lastName}
            </span>
          </MenuButton>

          <MenuItems
            className="
              absolute right-0 mt-2 w-56 origin-top-right
              bg-white rounded-lg shadow-lg ring-1 ring-black ring-opacity-5
              focus:outline-none z-50
            "
          >
            <div className="p-2">
              <div className="px-3 py-2 border-b border-gray-100 mb-1">
                <p className="text-sm font-medium text-gray-900">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-gray-500">{user?.email}</p>
                <p className="text-xs text-primary-600 mt-1">{user?.role}</p>
              </div>

              <MenuItem>
                {({ active }) => (
                  <button
                    onClick={() => navigate('/profile')}
                    className={`
                      w-full flex items-center gap-2 px-3 py-2 text-sm rounded-lg
                      ${active ? 'bg-gray-100' : ''}
                    `}
                  >
                    <UserCircleIcon className="h-5 w-5 text-gray-400" />
                    Profile
                  </button>
                )}
              </MenuItem>

              <MenuItem>
                {({ active }) => (
                  <button
                    onClick={() => navigate('/settings')}
                    className={`
                      w-full flex items-center gap-2 px-3 py-2 text-sm rounded-lg
                      ${active ? 'bg-gray-100' : ''}
                    `}
                  >
                    <Cog6ToothIcon className="h-5 w-5 text-gray-400" />
                    Settings
                  </button>
                )}
              </MenuItem>

              <div className="border-t border-gray-100 mt-1 pt-1">
                <MenuItem>
                  {({ active }) => (
                    <button
                      onClick={handleLogout}
                      className={`
                        w-full flex items-center gap-2 px-3 py-2 text-sm rounded-lg text-red-600
                        ${active ? 'bg-red-50' : ''}
                      `}
                    >
                      <ArrowRightOnRectangleIcon className="h-5 w-5" />
                      Sign out
                    </button>
                  )}
                </MenuItem>
              </div>
            </div>
          </MenuItems>
        </Menu>
      </div>
    </header>
  );
}

export default Header;
