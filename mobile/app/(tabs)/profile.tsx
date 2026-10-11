/**
 * Profile tab — borrower info + logout.
 */

import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '@/lib/auth';

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const router = useRouter();

  const onLogout = () => {
    Alert.alert('Sign out', 'Are you sure you want to sign out?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Sign Out',
        style: 'destructive',
        onPress: async () => {
          await logout();
          router.replace('/login');
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>
          {user?.firstName?.charAt(0)}
          {user?.lastName?.charAt(0)}
        </Text>
      </View>
      <Text style={styles.name}>
        {user?.firstName} {user?.lastName}
      </Text>
      <Text style={styles.email}>{user?.email}</Text>
      <View style={styles.roleBadge}>
        <Text style={styles.roleText}>{user?.role}</Text>
      </View>

      <TouchableOpacity style={styles.logoutButton} onPress={onLogout}>
        <Text style={styles.logoutText}>Sign Out</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f3f4f6', alignItems: 'center', paddingTop: 48 },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#dbeafe',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: { fontSize: 28, fontWeight: '700', color: '#2563eb' },
  name: { fontSize: 22, fontWeight: '700', color: '#111827', marginTop: 16 },
  email: { fontSize: 14, color: '#6b7280', marginTop: 4 },
  roleBadge: {
    backgroundColor: '#eff6ff',
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 4,
    marginTop: 10,
  },
  roleText: { fontSize: 12, fontWeight: '600', color: '#2563eb' },
  logoutButton: {
    marginTop: 40,
    backgroundColor: '#fef2f2',
    borderColor: '#fecaca',
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 12,
    paddingHorizontal: 32,
  },
  logoutText: { color: '#b91c1c', fontSize: 16, fontWeight: '600' },
});
