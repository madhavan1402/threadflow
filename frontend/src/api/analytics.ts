export type RiskStatus = 'ON_TRACK' | 'AT_RISK' | 'DELAYED' | 'COMPLETED';

export type OrderDelayRisk = {
  orderId: number;
  poNumber: string;
  buyerName: string;
  styleName: string;
  exFactoryDate: string;
  calculatedForDate: string;
  finalMilestoneName: string | null;
  totalQuantity: number;
  reportedCompletedQuantity: number;
  completedQuantity: number;
  remainingQuantity: number;
  elapsedProductionDays: number;
  remainingProductionDays: number;
  requiredDailyRate: number | null;
  actualDailyRate: number;
  estimatedCompletionDays: number | null;
  estimatedCompletionDate: string | null;
  productionShortfall: number;
  completionPercent: number;
  riskStatus: RiskStatus;
  reasons: string[];
};

export type ProductionSummary = {
  calculatedForDate: string;
  totalOrders: number;
  onTrack: number;
  atRisk: number;
  delayed: number;
  completed: number;
};

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '';

async function get<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, { credentials: 'include' });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || 'Production analytics could not be loaded.');
  return body as T;
}

export const analyticsApi = {
  orderRisk: (orderId: number) => get<OrderDelayRisk>(`/api/v1/analytics/orders/${orderId}/delay-risk`),
  allOrderRisks: () => get<OrderDelayRisk[]>('/api/v1/analytics/delay-risk'),
  summary: () => get<ProductionSummary>('/api/v1/analytics/production-summary'),
};