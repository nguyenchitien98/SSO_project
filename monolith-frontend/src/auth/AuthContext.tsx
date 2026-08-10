import React, { createContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { AuthState } from '@/types/auth';
import {
  generateCodeVerifier,
  generateCodeChallenge,
  generateState,
} from './pkce';
import {
  buildAuthorizeUrl,
  exchangeCode,
  refreshAccessToken,
  revokeToken,
} from './oauth';
import { registerTokenProvider } from '@/services/apiClient';

interface AuthContextType extends AuthState {
  login: () => Promise<void>;
  logout: () => Promise<void>;
  handleCallback: (code: string, state: string) => Promise<void>;
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
  getAccessToken: () => Promise<string | null>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Helper function to decode JWT payload without external library
function parseJwt(token: string): any {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    console.error('Failed to parse JWT token', e);
    return null;
  }
}

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    accessToken: null,
    refreshToken: null,
    user: null,
    isLoading: true,
  });

  // Setup local states from storage if exists
  useEffect(() => {
    const initAuth = async () => {
      const accessToken = sessionStorage.getItem('access_token');
      const refreshToken = sessionStorage.getItem('refresh_token');

      if (accessToken && refreshToken) {
        const payload = parseJwt(accessToken);
        if (payload && payload.exp * 1000 > Date.now()) {
          // Token is still valid
          setState({
            isAuthenticated: true,
            accessToken,
            refreshToken,
            user: {
              sub: payload.sub,
              username: payload.sub, // Fallback if no username
              email: payload.email || '',
              name: payload.name || '',
              roles: payload.roles || [],
              permissions: payload.permissions || [],
            },
            isLoading: false,
          });
          return;
        } else {
          // Access token expired, attempt refresh
          try {
            const tokens = await refreshAccessToken(refreshToken);
            const newPayload = parseJwt(tokens.access_token);
            if (newPayload) {
              sessionStorage.setItem('access_token', tokens.access_token);
              if (tokens.refresh_token) {
                sessionStorage.setItem('refresh_token', tokens.refresh_token);
              }
              setState({
                isAuthenticated: true,
                accessToken: tokens.access_token,
                refreshToken: tokens.refresh_token || refreshToken,
                user: {
                  sub: newPayload.sub,
                  username: newPayload.sub,
                  email: newPayload.email || '',
                  name: newPayload.name || '',
                  roles: newPayload.roles || [],
                  permissions: newPayload.permissions || [],
                },
                isLoading: false,
              });
              return;
            }
          } catch (e) {
            console.error('Auto-refresh token failed on initialization', e);
            // Session is invalid or refresh token expired
            sessionStorage.removeItem('access_token');
            sessionStorage.removeItem('refresh_token');
          }
        }
      }

      setState(prev => ({ ...prev, isLoading: false }));
    };

    initAuth();
  }, []);

  // Periodic silent renew before access token expires
  useEffect(() => {
    if (!state.isAuthenticated || !state.refreshToken || !state.accessToken) return;

    const payload = parseJwt(state.accessToken);
    if (!payload) return;

    const expTime = payload.exp * 1000;
    const timeUntilExpiry = expTime - Date.now();
    // Refresh 60 seconds before it expires
    const refreshDelay = Math.max(0, timeUntilExpiry - 60000);

    const timer = setTimeout(async () => {
      try {
        console.log('Silent renewing access token...');
        const tokens = await refreshAccessToken(state.refreshToken!);
        const newPayload = parseJwt(tokens.access_token);
        if (newPayload) {
          sessionStorage.setItem('access_token', tokens.access_token);
          if (tokens.refresh_token) {
            sessionStorage.setItem('refresh_token', tokens.refresh_token);
          }
          setState(prev => ({
            ...prev,
            accessToken: tokens.access_token,
            refreshToken: tokens.refresh_token || prev.refreshToken,
            user: {
              sub: newPayload.sub,
              username: newPayload.sub,
              email: newPayload.email || '',
              name: newPayload.name || '',
              roles: newPayload.roles || [],
              permissions: newPayload.permissions || [],
            },
          }));
        }
      } catch (e) {
        console.error('Silent renew failed', e);
        logout();
      }
    }, refreshDelay);

    return () => clearTimeout(timer);
  }, [state.isAuthenticated, state.accessToken, state.refreshToken]);

  // Trigger authorize redirect
  const login = useCallback(async () => {
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    const oauthState = generateState();

    sessionStorage.setItem('code_verifier', codeVerifier);
    sessionStorage.setItem('oauth_state', oauthState);

    window.location.href = buildAuthorizeUrl(oauthState, codeChallenge);
  }, []);

  // Logout clear storage & state
  const logout = useCallback(async () => {
    const accessToken = sessionStorage.getItem('access_token');
    const refreshToken = sessionStorage.getItem('refresh_token');

    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('code_verifier');
    sessionStorage.removeItem('oauth_state');

    setState({
      isAuthenticated: false,
      accessToken: null,
      refreshToken: null,
      user: null,
      isLoading: false,
    });

    if (accessToken) {
      await revokeToken(accessToken, 'access_token');
    }
    if (refreshToken) {
      await revokeToken(refreshToken, 'refresh_token');
    }

    // Redirect to login page
    window.location.href = '/login';
  }, []);

  // Handle OAuth callback
  const handleCallback = useCallback(async (code: string, callbackState: string) => {
    setState(prev => ({ ...prev, isLoading: true }));
    const savedState = sessionStorage.getItem('oauth_state');
    const codeVerifier = sessionStorage.getItem('code_verifier');

    if (!savedState || !codeVerifier) {
      setState(prev => ({ ...prev, isLoading: false }));
      throw new Error('OAuth flow was not initiated locally (missing state/verifier)');
    }

    if (savedState !== callbackState) {
      setState(prev => ({ ...prev, isLoading: false }));
      throw new Error('CSRF State mismatch error');
    }

    try {
      const tokens = await exchangeCode(code, codeVerifier);
      const payload = parseJwt(tokens.access_token);
      if (!payload) {
        throw new Error('Invalid JWT access token received');
      }

      sessionStorage.setItem('access_token', tokens.access_token);
      if (tokens.refresh_token) {
        sessionStorage.setItem('refresh_token', tokens.refresh_token);
      }

      sessionStorage.removeItem('code_verifier');
      sessionStorage.removeItem('oauth_state');

      setState({
        isAuthenticated: true,
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token || null,
        user: {
          sub: payload.sub,
          username: payload.sub,
          email: payload.email || '',
          name: payload.name || '',
          roles: payload.roles || [],
          permissions: payload.permissions || [],
        },
        isLoading: false,
      });
    } catch (e) {
      setState(prev => ({ ...prev, isLoading: false }));
      throw e;
    }
  }, []);

  // Permission checkers
  const hasPermission = useCallback((permission: string) => {
    return state.user?.permissions?.includes(permission) || false;
  }, [state.user]);

  const hasRole = useCallback((role: string) => {
    return state.user?.roles?.some(r => r.toUpperCase() === role.toUpperCase()) || false;
  }, [state.user]);

  // Method to get a valid access token (resolving expiry dynamically)
  const getAccessToken = useCallback(async (): Promise<string | null> => {
    const accessToken = sessionStorage.getItem('access_token');
    const refreshToken = sessionStorage.getItem('refresh_token');

    if (!accessToken || !refreshToken) return null;

    const payload = parseJwt(accessToken);
    if (payload && payload.exp * 1000 > Date.now() + 10000) {
      // Valid for at least another 10 seconds
      return accessToken;
    }

    // Expired or close to expiry, trigger sync refresh
    try {
      const tokens = await refreshAccessToken(refreshToken);
      sessionStorage.setItem('access_token', tokens.access_token);
      if (tokens.refresh_token) {
        sessionStorage.setItem('refresh_token', tokens.refresh_token);
      }
      return tokens.access_token;
    } catch (e) {
      console.error('Failed to refresh token during getAccessToken', e);
      logout();
      return null;
    }
  }, [logout]);

  useEffect(() => {
    registerTokenProvider(getAccessToken);
  }, [getAccessToken]);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        handleCallback,
        hasPermission,
        hasRole,
        getAccessToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
