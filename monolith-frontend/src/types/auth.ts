/**
 * Authentication type definitions.
 */

export interface UserInfo {
  sub: string; // UUID of the user
  username: string;
  email: string;
  name: string;
  roles: string[];
  permissions: string[];
}

export interface AuthState {
  isAuthenticated: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  user: UserInfo | null;
  isLoading: boolean;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

