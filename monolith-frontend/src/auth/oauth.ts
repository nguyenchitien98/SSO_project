/**
 * OAuth2 & OIDC Client Helper for monolith-frontend (React SPA).
 * Performs the token exchange, token refresh, and token revocation calls.
 */

const SSO_BASE_URL = 'http://localhost:9000';
const CLIENT_ID = 'monolith-web';
const REDIRECT_URI = 'http://localhost:3000/callback';

export interface TokenResponse {
  access_token: string;
  refresh_token?: string;
  id_token?: string;
  expires_in: number;
  scope: string;
  token_type: string;
}

/**
 * Builds the Authorization URL to redirect the user to the SSO Server login page.
 */
export function buildAuthorizeUrl(state: string, codeChallenge: string): string {
  const url = new URL(`${SSO_BASE_URL}/oauth2/authorize`);
  url.searchParams.append('response_type', 'code');
  url.searchParams.append('client_id', CLIENT_ID);
  url.searchParams.append('redirect_uri', REDIRECT_URI);
  url.searchParams.append('scope', 'openid profile email');
  url.searchParams.append('state', state);
  url.searchParams.append('code_challenge', codeChallenge);
  url.searchParams.append('code_challenge_method', 'S256');
  return url.toString();
}

/**
 * Exchanges the Authorization Code for Access Token and Refresh Token.
 */
export async function exchangeCode(code: string, codeVerifier: string): Promise<TokenResponse> {
  const params = new URLSearchParams();
  params.append('grant_type', 'authorization_code');
  params.append('client_id', CLIENT_ID);
  params.append('redirect_uri', REDIRECT_URI);
  params.append('code', code);
  params.append('code_verifier', codeVerifier);

  const response = await fetch(`${SSO_BASE_URL}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to exchange authorization code: ${response.status} - ${errorText}`);
  }

  return response.json();
}

/**
 * Refreshes the Access Token using the Refresh Token.
 */
export async function refreshAccessToken(refreshToken: string): Promise<TokenResponse> {
  const params = new URLSearchParams();
  params.append('grant_type', 'refresh_token');
  params.append('client_id', CLIENT_ID);
  params.append('refresh_token', refreshToken);

  const response = await fetch(`${SSO_BASE_URL}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to refresh token: ${response.status} - ${errorText}`);
  }

  return response.json();
}

/**
 * Revokes the Access Token or Refresh Token on logout.
 */
export async function revokeToken(token: string, tokenTypeHint: 'access_token' | 'refresh_token'): Promise<void> {
  const params = new URLSearchParams();
  params.append('token', token);
  params.append('token_type_hint', tokenTypeHint);
  params.append('client_id', CLIENT_ID);

  const response = await fetch(`${SSO_BASE_URL}/oauth2/revoke`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  });

  if (!response.ok) {
    console.error('Failed to revoke token', response.statusText);
  }
}
