/**
 * Helper utilities for OAuth2 PKCE (Proof Key for Code Exchange).
 * Uses the native Web Crypto API.
 */

/**
 * Generates a random high-entropy verifier string.
 * @returns string
 */
export function generateCodeVerifier(): string {
  const array = new Uint8Array(64);
  window.crypto.getRandomValues(array);
  return base64UrlEncode(array);
}

/**
 * Generates the code challenge corresponding to the verifier using SHA-256.
 * @param verifier
 * @returns Promise<string>
 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await window.crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(hash));
}

/**
 * Generates a random state string for CSRF protection.
 */
export function generateState(): string {
  const array = new Uint8Array(16);
  window.crypto.getRandomValues(array);
  return base64UrlEncode(array);
}

/**
 * Encodes a Uint8Array buffer into Base64URL format without padding.
 */
function base64UrlEncode(array: Uint8Array): string {
  let binary = '';
  const len = array.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(array[i]);
  }
  const base64 = window.btoa(binary);
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}
