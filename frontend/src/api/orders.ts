export type Buyer = { id: number; buyerName: string; country: string; contactEmail?: string };
export type Variant = { id?: number; color: string; size: string; quantity: number };
export type Milestone = {
  id: number; milestoneName: string; sequenceNumber: number; plannedStartDate: string;
  plannedEndDate: string; actualStartDate?: string | null; actualCompletionDate?: string | null;
  status: string; notes?: string | null;
};
export type ProductionOrder = {
  id: number; poNumber: string; buyer: { id: number; buyerName: string; country: string };
  styleName: string; productDescription?: string; totalQuantity: number; exFactoryDate: string;
  status: string; variants: Variant[]; milestones: Milestone[];
};
export type Template = {
  id: number; templateName: string; description?: string; active: boolean;
  milestones: Array<{ id: number; milestoneName: string; sequenceNumber: number; defaultDurationDays: number }>;
};
export type OrderPage = { content: ProductionOrder[]; totalElements: number; totalPages: number; number: number };

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
  if (!response.ok) throw new Error(body?.message || 'The request could not be completed.');
  return response.status === 204 ? undefined as T : body;
}

export const ordersApi = {
  list(params = ''): Promise<OrderPage> { return request(`/api/v1/orders${params}`); },
  get(id: number): Promise<ProductionOrder> { return request(`/api/v1/orders/${id}`); },
  create(body: object): Promise<ProductionOrder> { return request('/api/v1/orders', { method: 'POST', body: JSON.stringify(body) }); },
  updateMilestone(orderId: number, milestoneId: number, body: object): Promise<Milestone> {
    return request(`/api/v1/orders/${orderId}/milestones/${milestoneId}`, { method: 'PUT', body: JSON.stringify(body) });
  },
  buyers(): Promise<Buyer[]> { return request('/api/v1/buyers'); },
  createBuyer(body: object): Promise<Buyer> { return request('/api/v1/buyers', { method: 'POST', body: JSON.stringify(body) }); },
  templates(): Promise<Template[]> { return request('/api/v1/milestone-templates'); },
};
