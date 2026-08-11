import { DefaultSession } from 'next-auth';

export interface UserInfo {
  id: string;
  name: string;
  email: string;
  roles: string[];
  permissions: string[];
}

export interface SessionData {
  user: UserInfo;
  accessToken: string;
  refreshToken?: string;
  error?: string;
}

declare module 'next-auth' {
  interface Session extends DefaultSession {
    accessToken: string;
    refreshToken?: string;
    error?: string;
    user: UserInfo & DefaultSession['user'];
  }

  interface User {
    id?: string;
    name?: string | null;
    email?: string | null;
    roles?: string[];
    permissions?: string[];
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    accessToken: string;
    refreshToken?: string;
    expiresAt: number;
    user: UserInfo;
    error?: string;
  }
}
