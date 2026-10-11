/**
 * App configuration.
 *
 * API_URL points to the backend. On a physical device, `localhost` is the
 * phone itself — so we use the laptop's LAN IP. Override via app.json
 * `expo.extra.apiUrl` or the EXPO_PUBLIC_API_URL env var.
 */

import Constants from 'expo-constants';

const extra = Constants.expoConfig?.extra ?? {};

// Laptop LAN IP — phone and laptop must be on the same Wi-Fi.
const LAN_API_URL = 'http://192.168.31.210:4044/api';

export const config = {
  apiUrl: process.env.EXPO_PUBLIC_API_URL ?? extra.apiUrl ?? LAN_API_URL,
} as const;
