import { ApiResponse, ApiErrorResponse } from '@/types/api';

let getAccessTokenRef: (() => Promise<string | null>) | null = null;

/**
 * Registers the token provider from AuthContext.
 */
export function registerTokenProvider(provider: () => Promise<string | null>) {
  getAccessTokenRef = provider;
}

interface RequestOptions extends RequestInit {
  idempotencyKey?: string;
  skipAuth?: boolean;
}

/**
 * Custom fetch request wrapper.
 * Automatically attaches Authorization header and processes ApiResponse.
 */
async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { idempotencyKey, skipAuth, ...fetchOptions } = options;

  const headers = new Headers(fetchOptions.headers || {});
  headers.set('Content-Type', 'application/json');

  if (!skipAuth && getAccessTokenRef) {
    const token = await getAccessTokenRef();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  if (idempotencyKey) {
    headers.set('Idempotency-Key', idempotencyKey);
  }

  const response = await fetch(endpoint, {
    ...fetchOptions,
    headers,
  });

  // Handle No Content success response
  if (response.status === 204) {
    return null as unknown as T;
  }

  let json: any;
  try {
    json = await response.json();
  } catch (e) {
    throw new Error(`Failed to parse response JSON: ${response.status} ${response.statusText}`);
  }

  if (!response.ok) {
    // If it matches ApiErrorResponse
    if (json && json.success === false && json.errorCode) {
      const apiError: ApiErrorResponse = json;
      throw apiError;
    }
    throw new Error(json.message || `HTTP ${response.status}: ${response.statusText}`);
  }

  const apiResponse: ApiResponse<T> = json;
  return apiResponse.data;
}

export const apiClient = {
  get: <T>(url: string, options?: RequestOptions) =>
    request<T>(url, { ...options, method: 'GET' }),

  post: <T>(url: string, body?: any, options?: RequestOptions) =>
    request<T>(url, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),

  put: <T>(url: string, body?: any, options?: RequestOptions) =>
    request<T>(url, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),

  delete: <T>(url: string, options?: RequestOptions) =>
    request<T>(url, { ...options, method: 'DELETE' }),
};
