/**
 * Reusable loading / empty / error state — mirrors the web app's pattern of
 * explicit states instead of silently rendering stale or blank content.
 */

import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

type Variant = 'loading' | 'empty' | 'error';

interface Props {
  variant: Variant;
  message?: string;
  onRetry?: () => void;
  /** Override the leading emoji. */
  icon?: string;
}

const DEFAULTS: Record<Variant, { icon: string; text: string }> = {
  loading: { icon: '⏳', text: 'Loading...' },
  empty: { icon: '📭', text: 'Nothing here yet.' },
  error: { icon: '⚠️', text: 'Something went wrong.' },
};

export function ErrorState({ variant, message, onRetry, icon }: Props) {
  const defaults = DEFAULTS[variant];

  return (
    <View style={styles.container}>
      <Text style={styles.icon}>{icon ?? defaults.icon}</Text>
      <Text style={styles.message}>{message ?? defaults.text}</Text>
      {variant === 'error' && onRetry && (
        <TouchableOpacity style={styles.retryButton} onPress={onRetry}>
          <Text style={styles.retryText}>Retry</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 80,
    paddingHorizontal: 32,
  },
  icon: { fontSize: 44, marginBottom: 12 },
  message: { fontSize: 15, color: '#6b7280', textAlign: 'center' },
  retryButton: {
    marginTop: 16,
    backgroundColor: '#2563eb',
    borderRadius: 10,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  retryText: { color: '#fff', fontSize: 14, fontWeight: '600' },
});