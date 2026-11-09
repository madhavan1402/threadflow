export type ProductionUnit = {
  id: number; name: string; code: string; description?: string | null; active: boolean;
};
export type Supervisor = { id: number; email: string; enabled: boolean };
export type AssignmentHistory = {
  id: number; supervisorId: number; supervisorEmail: string; assignedById?: number | null;
  assignedByEmail?: string | null; assignedAt: string; revokedAt?: string | null; active: boolean;
};
export type OrderAssignments = {
  orderId: number; productionUnit: ProductionUnit | null; directAssignments: AssignmentHistory[];
};
export type UnitSupervisors = { productionUnitId: number; assignments: AssignmentHistory[] };

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '';

function csrf() {
  const tokenCookie = document.cookie.split('; ').find(item => item.startsWith('XSRF-TOKEN='));
  return tokenCookie ? decodeURIComponent(tokenCookie.split('=').slice(1).join('=')) : '';
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  await fetch(`${API_BASE}/api/v1/auth/csrf`, { credentials: 'include' });
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  const token = csrf();
  if (token && ['POST', 'PUT', 'DELETE'].includes(init.method || 'GET')) {
    headers.set('X-XSRF-TOKEN', token);
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || 'The assignment request could not be completed.');
  return body as T;
}

export const assignmentsApi = {
  units: () => request<ProductionUnit[]>('/api/v1/production-units'),
  supervisors: () => request<Supervisor[]>('/api/v1/supervisors'),
  unitSupervisors: (unitId: number) => request<UnitSupervisors>(`/api/v1/production-units/${unitId}/supervisors`),
  order: (orderId: number) => request<OrderAssignments>(`/api/v1/orders/${orderId}/assignments`),
  setOrderUnit: (orderId: number, productionUnitId: number | null) => request<OrderAssignments>(
    `/api/v1/orders/${orderId}/production-unit`,
    { method: 'PUT', body: JSON.stringify({ productionUnitId }) },
  ),
  setOrderSupervisors: (orderId: number, supervisorIds: number[]) => request<OrderAssignments>(
    `/api/v1/orders/${orderId}/supervisors`,
    { method: 'PUT', body: JSON.stringify({ supervisorIds }) },
  ),
};