import NextAuth from 'next-auth';
import { JWT } from 'next-auth/jwt';

// Helper to decode JWT Access Token payload
function decodeAccessToken(accessToken: string) {
  try {
    const payloadPart = accessToken.split('.')[1];
    if (!payloadPart) return {};
    const decoded = Buffer.from(payloadPart, 'base64').toString('utf-8');
    return JSON.parse(decoded);
  } catch (e) {
    console.error('Failed to decode access token:', e);
    return {};
  }
}

async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const url = `${process.env.AUTH_ISSUER}/oauth2/token`;
    
    // Build request body for refresh token grant
    const params = new URLSearchParams({
      client_id: process.env.AUTH_CLIENT_ID!,
      grant_type: 'refresh_token',
      refresh_token: token.refreshToken!,
    });

    if (process.env.AUTH_CLIENT_SECRET) {
      params.append('client_secret', process.env.AUTH_CLIENT_SECRET);
    }

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });

    const refreshedTokens = await response.json();

    if (!response.ok) {
      throw refreshedTokens;
    }

    const decoded = decodeAccessToken(refreshedTokens.access_token);

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      expiresAt: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken, // Fall back to old refresh token if not returned
      user: {
        id: decoded.sub || token.user.id,
        name: decoded.name || token.user.name,
        email: decoded.email || token.user.email,
        roles: decoded.roles || [],
        permissions: decoded.permissions || [],
      },
    };
  } catch (error) {
    console.error('Error refreshing access token:', error);
    return {
      ...token,
      error: 'RefreshAccessTokenError',
    };
  }
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    {
      id: 'sso-server',
      name: 'SSO Platform',
      type: 'oidc',
      issuer: process.env.AUTH_ISSUER, // http://localhost:9000
      clientId: process.env.AUTH_CLIENT_ID, // microservice-gateway
      clientSecret: process.env.AUTH_CLIENT_SECRET || '',
      authorization: {
        params: {
          scope: 'openid profile email',
        },
      },
      profile(profile) {
        return {
          id: profile.sub,
          name: profile.name || profile.preferred_username || profile.sub,
          email: profile.email,
          roles: [],
          permissions: [],
        };
      },
    },
  ],
  callbacks: {
    async jwt({ token, account, user }) {
      // First time sign in
      if (account && user) {
        const decoded = decodeAccessToken(account.access_token!);
        return {
          accessToken: account.access_token!,
          refreshToken: account.refresh_token,
          expiresAt: (account.expires_at ?? 0) * 1000,
          user: {
            id: user.id!,
            name: decoded.name || user.name || '',
            email: decoded.email || user.email || '',
            roles: decoded.roles || [],
            permissions: decoded.permissions || [],
          },
        };
      }

      // Subsequent access: return token if it has not expired yet
      if (Date.now() < token.expiresAt) {
        return token;
      }

      // Token has expired: rotate/refresh token
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.refreshToken = token.refreshToken;
      session.error = token.error;
      session.user = {
        ...session.user,
        id: token.user.id,
        name: token.user.name,
        email: token.user.email,
        roles: token.user.roles,
        permissions: token.user.permissions,
      };
      return session;
    },
  },
  pages: {
    signIn: '/login',
  },
  session: {
    strategy: 'jwt',
  },
});
