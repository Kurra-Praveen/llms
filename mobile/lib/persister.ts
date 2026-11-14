/**
 * Query cache persister — backed by AsyncStorage so cached data survives
 * app restarts and is available on flaky networks.
 *
 * Implements the PersistQueryClientProvider's expected persister interface:
 *   persistClient(data)   — save dehydrated cache
 *   restoreClient()       — restore hydrated cache
 *   removeClient()        — clear persisted cache
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import type { PersistedClient, Persister } from '@tanstack/react-query-persist-client';

const CACHE_KEY = 'loan-platform-query-cache-v1';

export const persister: Persister = {
  async persistClient(persistedClient: PersistedClient) {
    try {
      await AsyncStorage.setItem(CACHE_KEY, JSON.stringify(persistedClient));
    } catch (err) {
      if (__DEV__) console.warn('[persister] persistClient failed:', err);
    }
  },

  async restoreClient(): Promise<PersistedClient | undefined> {
    try {
      const data = await AsyncStorage.getItem(CACHE_KEY);
      if (data) {
        return JSON.parse(data) as PersistedClient;
      }
    } catch (err) {
      if (__DEV__) console.warn('[persister] restoreClient failed:', err);
    }
    return undefined;
  },

  async removeClient() {
    try {
      await AsyncStorage.removeItem(CACHE_KEY);
    } catch (err) {
      if (__DEV__) console.warn('[persister] removeClient failed:', err);
    }
  },
};