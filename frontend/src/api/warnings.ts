export type BuyerDelayWarning = {
  id: number;
  orderId: number;
  poNumber: string;
  status: 'PENDING_CONFIRMATION' | 'CONFIRMED' | 'DISMISSED';
  riskStatus: string;
  internalReasons: string;
  suggestedMessage: string;
  confirmedMessage?: string | null;
  confirmedById?: number | null;
  confirmedByEmail?: string | null;
  confirmedAt?: string | null;
  createdAt: string;
};

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '';

function csrf() {
  const match = document.cookie.split('; ').find((item) => item.startsWith('XSRF-TOKEN='));
  return match ? decodeURIComponent(match.split('=').slice(1).join('=')) : '';
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  await fetch(`${API_BASE}/api/v1/auth/csrf`, { credentials: 'include' });
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  const token = csrf();
  if (token && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(init.method || 'GET')) {
    headers.set('X-XSRF-TOKEN', token);
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || 'Delay warning request could not be completed.');
  return body as T;
}

export const warningsApi = {
  getWarnings(orderId: number): Promise<BuyerDelayWarning[]> {
    return request<BuyerDelayWarning[]>(`/api/v1/orders/${orderId}/buyer-warnings`);
  },
  createDraft(orderId: number, customMessage?: string): Promise<BuyerDelayWarning> {
    return request<BuyerDelayWarning>(`/api/v1/orders/${orderId}/buyer-warnings`, {
      method: 'POST',
      body: JSON.stringify({ customMessage }),
    });
  },
  confirmWarning(orderId: number, warningId: number, approvedMessage: string): Promise<BuyerDelayWarning> {
    return request<BuyerDelayWarning>(`/api/v1/orders/${orderId}/buyer-warnings/${warningId}/confirm`, {
      method: 'POST',
      body: JSON.stringify({ approvedMessage }),
    });
  },
  dismissWarning(orderId: number, warningId: number): Promise<BuyerDelayWarning> {
    return request<BuyerDelayWarning>(`/api/v1/orders/${orderId}/buyer-warnings/${warningId}/dismiss`, {
      method: 'POST',
    });
  },
};
