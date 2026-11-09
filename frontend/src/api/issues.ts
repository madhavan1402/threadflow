export type IssueCategory =
  | 'MACHINE_BREAKDOWN'
  | 'MATERIAL_SHORTAGE'
  | 'LABOUR_SHORTAGE'
  | 'QUALITY_ISSUE'
  | 'PRINTING_DELAY'
  | 'WASHING_DELAY'
  | 'POWER_FAILURE'
  | 'OTHER';

export type IssueClassificationStatus =
  | 'PENDING'
  | 'PENDING_REVIEW'
  | 'AI_UNAVAILABLE'
  | 'CLASSIFICATION_FAILED'
  | 'REVIEWED';

export type IssueReviewDecision = 'ACCEPTED_AI' | 'CORRECTED';

export type ReviewHistoryResponse = {
  id: number;
  decision: IssueReviewDecision;
  category: IssueCategory;
  englishTranslation?: string | null;
  reviewerId: number;
  reviewerEmail: string;
  reviewedAt: string;
};

export type ClassificationResponse = {
  issueId: number;
  productionUpdateId: number;
  orderId: number;
  poNumber: string;
  styleName: string;
  supervisorId: number;
  originalNote: string;
  detectedLanguage?: string | null;
  englishTranslation?: string | null;
  predictedCategory?: IssueCategory | null;
  confidence?: number | null;
  status: IssueClassificationStatus;
  providerId?: string | null;
  modelId?: string | null;
  reviewedCategory?: IssueCategory | null;
  reviewedTranslation?: string | null;
  reviewerId?: number | null;
  reviewerEmail?: string | null;
  reviewedAt?: string | null;
  reviewHistory: ReviewHistoryResponse[];
};

export type ReviewRequest = {
  acceptAiClassification: boolean;
  category?: IssueCategory | null;
  englishTranslation?: string | null;
};

export const ISSUE_CATEGORY_LABELS: Record<IssueCategory, string> = {
  MACHINE_BREAKDOWN: 'Machine Breakdown',
  MATERIAL_SHORTAGE: 'Material Shortage',
  LABOUR_SHORTAGE: 'Labour Shortage',
  QUALITY_ISSUE: 'Quality Issue',
  PRINTING_DELAY: 'Printing Delay',
  WASHING_DELAY: 'Washing Delay',
  POWER_FAILURE: 'Power Failure',
  OTHER: 'Other',
};

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
  if (!response.ok) throw new Error(body?.message || 'The issue request could not be completed.');
  return body as T;
}

export const issuesApi = {
  pendingReview: () => request<ClassificationResponse[]>('/api/v1/issues/pending-review'),
  classification: (issueId: number) => request<ClassificationResponse>(`/api/v1/issues/${issueId}/classification`),
  review: (issueId: number, data: ReviewRequest) =>
    request<ClassificationResponse>(`/api/v1/issues/${issueId}/review`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  reclassify: (issueId: number) =>
    request<ClassificationResponse>(`/api/v1/issues/${issueId}/reclassify`, {
      method: 'POST',
    }),
};
