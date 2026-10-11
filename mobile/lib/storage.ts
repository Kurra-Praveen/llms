/**
 * Secure token storage — uses the device Keystore/Keychain via expo-secure-store.
 * (This is the mobile equivalent of the web app's localStorage, but encrypted.)
 */

import * as SecureStore from 'expo-secure-store';
import type { User } from './types';

const ACCESS_TOKEN = 'accessToken';
const REFRESH_TOKEN = 'refreshToken';
const USER = 'user';

export const storage = {
  async saveAuth(accessToken: string, refreshToken: string, user: User): Promise<void> {
    await Promise.all([
      SecureStore.setItemAsync(ACCESS_TOKEN, accessToken),
      SecureStore.setItemAsync(REFRESH_TOKEN, refreshToken),
      SecureStore.setItemAsync(USER, JSON.stringify(user)),
    ]);
  },

  async getAccessToken(): Promise<string | null> {
    return SecureStore.getItemAsync(ACCESS_TOKEN);
  },

  async getRefreshToken(): Promise<string | null> {
    return SecureStore.getItemAsync(REFRESH_TOKEN);
  },

  async getUser(): Promise<User | null> {
    const json = await SecureStore.getItemAsync(USER);
    if (!json) return null;
    try {
      return JSON.parse(json) as User;
    } catch {
      return null;
    }
  },

  async clear(): Promise<void> {
    await Promise.all([
      SecureStore.deleteItemAsync(ACCESS_TOKEN),
      SecureStore.deleteItemAsync(REFRESH_TOKEN),
      SecureStore.deleteItemAsync(USER),
    ]);
  },
};
