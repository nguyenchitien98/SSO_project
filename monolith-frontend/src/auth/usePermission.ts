import { useAuth } from './useAuth';

export function usePermission() {
  const { hasPermission, hasRole } = useAuth();
  return { hasPermission, hasRole };
}
