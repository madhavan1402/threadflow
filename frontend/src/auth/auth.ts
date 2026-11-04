export type UserRole = 'MERCHANDISER' | 'SUPERVISOR';

export type AuthUser = {
  id: number;
  email: string;
  roles: UserRole[];
};

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '')
  || 'http://127.0.0.1:8080';

export function getCsrfToken() {
  const match = document.cookie.split('; ')
    .find((item) => item.startsWith('XSRF-TOKEN='));
  return match ? decodeURIComponent(match.split('=').slice(1).join('=')) : null;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new AuthApiError(
      response.status,
      body?.message || 'The server could not complete the request.',
      body?.fieldErrors || [],
    );
  }
  return response.status === 204 ? undefined as T : response.json();
}

export class AuthApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fieldErrors: Array<{ field: string; message: string }>,
  ) {
    super(message);
  }
}

export async function prepareCsrf() {
  return request<{ headerName: string; token: string }>('/api/v1/auth/csrf');
}

export async function login(email: string, password: string) {
  await prepareCsrf();
  const csrfToken = getCsrfToken();
  return request<AuthUser>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {},
  });
}

export async function currentUser() {
  return request<AuthUser>('/api/v1/auth/me');
}

export async function logout() {
  const csrfToken = getCsrfToken();
  return request<{ status: string }>('/api/v1/auth/logout', {
    method: 'POST',
    headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {},
  });
}

export function validateLogin(email: string, password: string) {
  const errors: { email?: string; password?: string } = {};
  if (!email.trim()) errors.email = 'Email is required';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) errors.email = 'Enter a valid email address';
  if (!password) errors.password = 'Password is required';
  return errors;
}
