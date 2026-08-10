/**
 * User Profile related types.
 */

export interface UserProfile {
  id: string; // UUID
  displayName: string;
  phone: string | null;
  avatarUrl: string | null;
  address: string | null;
  preferences: string | null; // JSON String
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  displayName: string;
  phone?: string | null;
  avatarUrl?: string | null;
  address?: string | null;
  preferences?: string | null;
}
