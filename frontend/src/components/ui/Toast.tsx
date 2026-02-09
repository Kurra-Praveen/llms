import { useEffect, useState } from 'react';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  XMarkIcon,
} from '@heroicons/react/24/outline';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastProps {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
  onClose: (id: string) => void;
}

const typeStyles: Record<ToastType, { bg: string; icon: string; iconColor: string }> = {
  success: {
    bg: 'bg-green-50 border-green-200',
    icon: 'bg-green-100',
    iconColor: 'text-green-600',
  },
  error: {
    bg: 'bg-red-50 border-red-200',
    icon: 'bg-red-100',
    iconColor: 'text-red-600',
  },
  warning: {
    bg: 'bg-yellow-50 border-yellow-200',
    icon: 'bg-yellow-100',
    iconColor: 'text-yellow-600',
  },
  info: {
    bg: 'bg-blue-50 border-blue-200',
    icon: 'bg-blue-100',
    iconColor: 'text-blue-600',
  },
};

const typeIcons: Record<ToastType, React.ElementType> = {
  success: CheckCircleIcon,
  error: ExclamationCircleIcon,
  warning: ExclamationTriangleIcon,
  info: InformationCircleIcon,
};

export function Toast({ id, message, type, duration = 5000, onClose }: ToastProps) {
  const [isExiting, setIsExiting] = useState(false);
  const styles = typeStyles[type];
  const Icon = typeIcons[type];

  useEffect(() => {
    if (duration > 0) {
      const timer = setTimeout(() => {
        setIsExiting(true);
        setTimeout(() => onClose(id), 300);
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [id, duration, onClose]);

  const handleClose = () => {
    setIsExiting(true);
    setTimeout(() => onClose(id), 300);
  };

  return (
    <div
      className={`
        flex items-center gap-3 p-4 rounded-lg border shadow-lg max-w-md
        transition-all duration-300 transform
        ${styles.bg}
        ${isExiting ? 'opacity-0 translate-x-full' : 'opacity-100 translate-x-0'}
      `}
    >
      <div className={`flex-shrink-0 p-1 rounded-full ${styles.icon}`}>
        <Icon className={`h-5 w-5 ${styles.iconColor}`} />
      </div>
      <p className="flex-1 text-sm font-medium text-gray-900">{message}</p>
      <button
        onClick={handleClose}
        className="flex-shrink-0 p-1 rounded hover:bg-gray-200 transition-colors"
      >
        <XMarkIcon className="h-4 w-4 text-gray-500" />
      </button>
    </div>
  );
}

export interface ToastContainerProps {
  toasts: Array<{ id: string; message: string; type: ToastType; duration?: number }>;
  onClose: (id: string) => void;
}

export function ToastContainer({ toasts, onClose }: ToastContainerProps) {
  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          id={toast.id}
          message={toast.message}
          type={toast.type}
          duration={toast.duration}
          onClose={onClose}
        />
      ))}
    </div>
  );
}
