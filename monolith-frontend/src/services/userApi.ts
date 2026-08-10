import { apiClient } from './apiClient';
import { UserProfile, UpdateProfileRequest } from '@/types/user';
import { ChangePasswordRequest } from '@/types/auth';

export const userApi = {
  getMyProfile: () =>
    apiClient.get<UserProfile>('/api/users/me'),

  updateMyProfile: (request: UpdateProfileRequest) =>
    apiClient.put<UserProfile>('/api/users/me', request),

  changePassword: (request: ChangePasswordRequest) =>
    apiClient.post<void>('http://localhost:9000/auth/change-password', request),
};
