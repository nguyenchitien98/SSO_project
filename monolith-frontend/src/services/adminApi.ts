import { apiClient } from './apiClient';
import { PageResponse } from '@/types/api';

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  enabled: boolean;
  locked: boolean;
  lockedReason: string | null;
  lastLoginAt: string | null;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AuditLogResponse {
  id: number;
  actorId: string | null;
  actorName: string;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  ipAddress: string | null;
  createdAt: string;
}

const SSO_ADMIN_URL = 'http://localhost:9000/admin/users';

export const adminApi = {
  // SSO User Management
  getUsers: (page = 0, size = 10) =>
    apiClient.get<PageResponse<UserResponse>>(`${SSO_ADMIN_URL}?page=${page}&size=${size}`),

  updateUserStatus: (id: string, enabled: boolean, reason = '') =>
    apiClient.put<UserResponse>(`${SSO_ADMIN_URL}/${id}/status`, { enabled, reason }),

  assignRoles: (id: string, roles: string[]) =>
    apiClient.post<UserResponse>(`${SSO_ADMIN_URL}/${id}/roles`, roles),

  // Monolith Audit Logs
  getAuditLogs: (page = 0, size = 10) =>
    apiClient.get<PageResponse<AuditLogResponse>>(`/api/admin/audit-logs?page=${page}&size=${size}`),
};
