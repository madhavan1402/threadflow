export type ProductionMilestone = { id: number; milestoneName: string; sequenceNumber: number; status: string };
export type AvailableOrder = {
  id: number; poNumber: string; styleName: string; buyerName: string;
  totalQuantity: number; exFactoryDate: string;
  productionUnit: { id: number; name: string; code: string; description?: string | null; active: boolean } | null;
  assignmentSource: 'UNIT' | 'DIRECT' | 'BOTH'; milestones: ProductionMilestone[];
};
export type ProductionPhoto = { id: number; originalFilename: string; contentType: string; fileSize: number };
export type ProductionUpdate = {
  id: number; orderId: number; poNumber: string; milestoneId: number; milestoneName: string;
  supervisorId: number; supervisorEmail: string; productionDate: string; completedQuantity: number;
  issueNote?: string | null; createdAt: string; updatedAt: string; photos: ProductionPhoto[];
};

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '';

function csrf() {
  const match = document.cookie.split('; ').find((item) => item.startsWith('XSRF-TOKEN='));
  return match ? decodeURIComponent(match.split('=').slice(1).join('=')) : '';
}

async function jsonRequest<T>(path: string, init: RequestInit = {}) {
  await fetch(`${API_BASE}/api/v1/auth/csrf`, { credentials: 'include' });
  const token = csrf();
  const headers = new Headers(init.headers);
  if (token && ['POST','PUT','DELETE'].includes(init.method || 'GET')) headers.set('X-XSRF-TOKEN', token);
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || 'The production request could not be completed.');
  return body as T;
}

export function validatePhotos(files: File[]) {
  const allowed = new Set(['image/jpeg', 'image/png', 'image/webp']);
  for (const file of files) {
    if (!allowed.has(file.type)) return 'Photos must be JPEG, PNG or WebP.';
    if (file.size > 10 * 1024 * 1024) return 'Photos must be 10 MB or smaller.';
  }
  return '';
}

export const productionApi = {
  availableOrders: () => jsonRequest<AvailableOrder[]>('/api/v1/production/available-orders'),
  milestones: (orderId: number) => jsonRequest<ProductionMilestone[]>(`/api/v1/production/orders/${orderId}/milestones`),
  myUpdates: () => jsonRequest<ProductionUpdate[]>('/api/v1/production/my-updates'),
  updates: () => jsonRequest<ProductionUpdate[]>('/api/v1/production/updates'),
  orderUpdates: (orderId: number) => jsonRequest<ProductionUpdate[]>(`/api/v1/production/orders/${orderId}/updates`),
  async submit(fields: {
    orderId: number; milestoneId: number; productionDate: string; completedQuantity: number;
    issueNote?: string; idempotencyKey: string; photos: File[];
  }) {
    await fetch(`${API_BASE}/api/v1/auth/csrf`, { credentials: 'include' });
    const token = csrf();
    const form = new FormData();
    form.set('orderId', String(fields.orderId));
    form.set('milestoneId', String(fields.milestoneId));
    form.set('productionDate', fields.productionDate);
    form.set('completedQuantity', String(fields.completedQuantity));
    form.set('issueNote', fields.issueNote || '');
    form.set('idempotencyKey', fields.idempotencyKey);
    fields.photos.forEach(file => form.append('photos', file));
    const headers = new Headers();
    if (token) headers.set('X-XSRF-TOKEN', token);
    const response = await fetch(`${API_BASE}/api/v1/production/updates`, {
      method: 'POST', headers, body: form, credentials: 'include',
    });
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new Error(body?.message || 'The production update could not be saved.');
    return body as ProductionUpdate;
  },
  photoUrl(photoId: number) { return `${API_BASE}/api/v1/production/photos/${photoId}`; },
};
