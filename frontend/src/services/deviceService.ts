/**
 * Device API Service (/v1/devices)
 * Registers/unregisters push-notification device tokens.
 * On web this is a no-op placeholder; the mobile app uses it with FCM tokens.
 */

import api from './api';
import { logger } from '@/utils/logger';
import type { ApiResponse, DeviceTokenRequest } from '@/types';

const deviceLogger = logger.scope('DeviceService');

export const deviceService = {
  /** Register a device token for push notifications */
  async register(request: DeviceTokenRequest): Promise<void> {
    deviceLogger.info('Registering device', { platform: request.platform });
    await api.post<ApiResponse<null>>('/v1/devices', request);
  },

  /** Unregister a device token (e.g., on logout) */
  async unregister(token: string): Promise<void> {
    deviceLogger.info('Unregistering device');
    await api.delete<ApiResponse<null>>(`/v1/devices/${encodeURIComponent(token)}`);
  },
};

export default deviceService;
