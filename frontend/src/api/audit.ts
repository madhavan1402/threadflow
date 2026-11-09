export type DateAuditRecord = {
  id: number;
  orderId: number;
  previousExFactoryDate: string;
  newExFactoryDate: string;
  reason: string;
  changedById?: number | null;
  changedByEmail?: string | null;
  changedAt: string;
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
  if (!response.ok) throw new Error(body?.message || 'Date audit request could not be completed.');
  return body as T;
}

export const auditApi = {
  getAuditHistory(orderId: number): Promise<DateAuditRecord[]> {
    return request<DateAuditRecord[]>(`/api/v1/orders/${orderId}/audit-history`);
  },
  changeExFactoryDate(orderId: number, newExFactoryDate: string, reason: string): Promise<any> {
    return request(`/api/v1/orders/${orderId}/ex-factory-date`, {
      method: 'PUT',
      body: JSON.stringify({ newExFactoryDate, reason }),
    });
  },
};
