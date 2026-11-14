/**
 * Root layout — sets up React Query with AsyncStorage persistence,
 * restores the auth session, and gates routes based on auth state.
 */

import { useEffect } from 'react';
import { Stack, useRouter, useSegments } from 'expo-router';
import { QueryClient } from '@tanstack/react-query';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { StatusBar } from 'expo-status-bar';
import { authStore, useAuth } from '@/lib/auth';
import { persister } from '@/lib/persister';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Data is fresh for 1 minute; cache lives on disk for 24 hours
      staleTime: 1000 * 60,
      gcTime: 1000 * 60 * 60 * 24,
      retry: 1,
    },
  },
});

function AuthGate() {
  const { isAuthenticated, isLoading } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    authStore.initialize();
  }, []);

  useEffect(() => {
    if (isLoading) return;

    const inLoginScreen = segments[0] === 'login';

    if (!isAuthenticated && !inLoginScreen) {
      router.replace('/login');
    } else if (isAuthenticated && inLoginScreen) {
      router.replace('/(tabs)');
    }
  }, [isAuthenticated, isLoading, segments, router]);

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="login" />
      <Stack.Screen name="(tabs)" />
      <Stack.Screen
        name="notifications"
        options={{ presentation: 'card', headerShown: true, title: 'Notifications' }}
      />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister, maxAge: 1000 * 60 * 60 * 24 }}
    >
      <StatusBar style="dark" />
      <AuthGate />
    </PersistQueryClientProvider>
  );
}