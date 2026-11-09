export type BuyerOverview = {
  buyerId: number;
  buyerName: string;
  country: string;
  totalOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  confirmedWarningsCount: number;
};

export type BuyerMilestoneProgress = {
  id: number;
  milestoneName: string;
  sequenceNumber: number;
  plannedStartDate: string;
  plannedEndDate: string;
  actualCompletionDate?: string | null;
  status: string;
};

export type BuyerConfirmedWarning = {
  id: number;
  riskStatus: string;
  message: string;
  confirmedAt: string;
};

export type BuyerApprovedPhoto = {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  approvedAt?: string | null;
};

export type BuyerOrderSummary = {
  id: number;
  poNumber: string;
  styleName: string;
  productDescription?: string | null;
  totalQuantity: number;
  exFactoryDate: string;
  status: string;
  progressPercent: number;
  confirmedWarningsCount: number;
  approvedPhotosCount: number;
};

export type BuyerOrderDetail = {
  id: number;
  poNumber: string;
  styleName: string;
  productDescription?: string | null;
  totalQuantity: number;
  exFactoryDate: string;
  status: string;
  progressPercent: number;
  milestones: BuyerMilestoneProgress[];
  confirmedWarnings: BuyerConfirmedWarning[];
  approvedPhotos: BuyerApprovedPhoto[];
};

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '';
const BUYER_TOKEN_KEY = 'threadflow_buyer_token';

export function getStoredBuyerToken(): string {
  try {
    return localStorage.getItem(BUYER_TOKEN_KEY) || sessionStorage.getItem(BUYER_TOKEN_KEY) || '';
  } catch {
    return '';
  }
}

export function setStoredBuyerToken(token: string): void {
  try {
    if (token) {
      localStorage.setItem(BUYER_TOKEN_KEY, token);
      sessionStorage.setItem(BUYER_TOKEN_KEY, token);
    } else {
      localStorage.removeItem(BUYER_TOKEN_KEY);
      sessionStorage.removeItem(BUYER_TOKEN_KEY);
    }
  } catch {
    // storage unavailable
  }
}

export function clearStoredBuyerToken(): void {
  setStoredBuyerToken('');
}

async function buyerFetch<T>(path: string, tokenOverride?: string): Promise<T> {
  const token = tokenOverride ?? getStoredBuyerToken();
  const headers = new Headers();
  if (token) {
    headers.set('X-Buyer-Token', token);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'GET',
    headers,
    credentials: 'include',
  });

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || (response.status === 401 ? 'Valid buyer token required' : response.status === 403 ? 'Access denied for this buyer' : 'Request failed'));
  }
  return body as T;
}

export const buyerApi = {
  getOverview(token?: string): Promise<BuyerOverview> {
    return buyerFetch<BuyerOverview>('/api/v1/buyer/portal', token);
  },
  getOrders(token?: string): Promise<BuyerOrderSummary[]> {
    return buyerFetch<BuyerOrderSummary[]>('/api/v1/buyer/orders', token);
  },
  getOrderDetail(orderId: number, token?: string): Promise<BuyerOrderDetail> {
    return buyerFetch<BuyerOrderDetail>(`/api/v1/buyer/orders/${orderId}`, token);
  },
  getMilestones(orderId: number, token?: string): Promise<BuyerMilestoneProgress[]> {
    return buyerFetch<BuyerMilestoneProgress[]>(`/api/v1/buyer/orders/${orderId}/milestones`, token);
  },
  getPhotos(orderId: number, token?: string): Promise<BuyerApprovedPhoto[]> {
    return buyerFetch<BuyerApprovedPhoto[]>(`/api/v1/buyer/orders/${orderId}/photos`, token);
  },
  photoContentUrl(orderId: number, photoId: number, token?: string): string {
    const activeToken = token ?? getStoredBuyerToken();
    const query = activeToken ? `?buyerToken=${encodeURIComponent(activeToken)}` : '';
    return `${API_BASE}/api/v1/buyer/orders/${orderId}/photos/${photoId}/content${query}`;
  },
};
